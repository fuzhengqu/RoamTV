package tv.roam.client.feature.pairing

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Minimal one-shot HTTP server for the phone pairing page.
 *
 * It intentionally supports only the two routes the page needs, binds to the
 * selected LAN address, caps the request size and POST count, and closes after
 * one successful import. Configuration text is never logged.
 */
class PairingHttpServer(
    private val session: PairingSession,
    private val bindAddress: InetAddress,
    private val pageHtml: String,
    private val sjclJs: String,
    private val onText: (String) -> Unit,
) {
    private var serverSocket: ServerSocket? = null
    private var worker: Thread? = null
    private val closed = AtomicBoolean(false)
    private val consumed = AtomicBoolean(false)
    private val postAttempts = AtomicInteger(0)

    /** Binds an ephemeral port and starts the accept loop. Returns the port. */
    fun start(): Int {
        val socket = ServerSocket(0, 8, bindAddress)
        socket.soTimeout = 1_000
        serverSocket = socket
        worker = Thread({ acceptLoop(socket) }, "RoamTV-pairing").apply {
            isDaemon = true
            start()
        }
        return socket.localPort
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { serverSocket?.close() }
        worker?.interrupt()
        serverSocket = null
        worker = null
    }

    private fun acceptLoop(socket: ServerSocket) {
        try {
            while (!closed.get() && !session.isExpired()) {
                try {
                    val client = socket.accept()
                    handle(client)
                } catch (_: SocketTimeoutException) {
                    // Check expiry/closed and continue.
                } catch (_: IOException) {
                    if (closed.get()) break
                }
            }
        } finally {
            close()
        }
    }

    private fun handle(client: Socket) {
        try {
            client.soTimeout = 5_000
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            val request = readRequest(input) ?: run {
                respond(output, 400, "Bad Request", "text/plain", "bad request")
                return
            }
            if (request.tooLarge) {
                respond(output, 413, "Payload Too Large", "text/plain", "too large")
                return
            }

            val path = request.path.substringBefore('?')
            when {
                request.method == "GET" && path == "/sjcl.js" -> {
                    respond(
                        output,
                        200,
                        "OK",
                        "application/javascript; charset=utf-8",
                        sjclJs,
                    )
                }

                request.method == "GET" && path.startsWith("/s/") -> {
                    val id = path.removePrefix("/s/").trim('/')
                    if (id != session.id || session.isExpired()) {
                        respond(output, 404, "Not Found", "text/plain", "not found")
                    } else {
                        respond(output, 200, "OK", "text/html; charset=utf-8", pageHtml)
                    }
                }

                request.method == "POST" && path == "/api/upload" -> {
                    handleUpload(request, output)
                }

                else -> respond(output, 404, "Not Found", "text/plain", "not found")
            }
        } catch (_: Exception) {
            // Never log the body or key. The UI reports a generic failure.
        } finally {
            runCatching { client.close() }
        }
    }

    private fun handleUpload(request: Request, output: BufferedOutputStream) {
        if (consumed.get()) {
            respond(output, 410, "Gone", "text/plain", "gone")
            return
        }
        if (session.isExpired()) {
            respond(output, 410, "Gone", "text/plain", "expired")
            return
        }
        if (postAttempts.incrementAndGet() > MAX_POST_ATTEMPTS) {
            respond(output, 429, "Too Many Requests", "text/plain", "too many")
            return
        }
        val suppliedSession = request.headers["x-pairing-session"].orEmpty()
        if (!constantTimeEquals(suppliedSession, session.id)) {
            respond(output, 403, "Forbidden", "text/plain", "forbidden")
            return
        }
        val payload = request.body.toString(StandardCharsets.UTF_8)
        val text = PairingCrypto.decrypt(payload, session.key)
        if (text == null) {
            respond(output, 400, "Bad Request", "text/plain", "invalid payload")
            return
        }
        if (!consumed.compareAndSet(false, true)) {
            respond(output, 410, "Gone", "text/plain", "gone")
            return
        }
        respond(output, 200, "OK", "text/plain; charset=utf-8", "ok")
        runCatching { onText(text) }
        close()
    }

    private fun readRequest(input: BufferedInputStream): Request? {
        val requestLine = readLine(input) ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 3) return null
        val headers = mutableMapOf<String, String>()
        var headerBytes = 0
        while (true) {
            val line = readLine(input) ?: return null
            if (line.isEmpty()) break
            headerBytes += line.length + 2
            if (headerBytes > MAX_HEADER_BYTES) return null
            val colon = line.indexOf(':')
            if (colon <= 0) return null
            headers[line.substring(0, colon).trim().lowercase()] =
                line.substring(colon + 1).trim()
        }
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        if (contentLength < 0) return null
        if (contentLength > MAX_BODY_BYTES) {
            return Request(parts[0], parts[1], headers, ByteArray(0), tooLarge = true)
        }
        val body = ByteArray(contentLength)
        var offset = 0
        while (offset < contentLength) {
            val read = input.read(body, offset, contentLength - offset)
            if (read < 0) throw EOFException("truncated body")
            offset += read
        }
        return Request(parts[0], parts[1], headers, body, tooLarge = false)
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = java.io.ByteArrayOutputStream(128)
        while (true) {
            val value = input.read()
            if (value < 0) {
                if (bytes.size() == 0) return null
                break
            }
            if (value == '\n'.code) break
            if (value != '\r'.code) {
                if (bytes.size() >= MAX_LINE_BYTES) return null
                bytes.write(value)
            }
        }
        return bytes.toString(StandardCharsets.UTF_8.name())
    }

    private fun respond(
        output: BufferedOutputStream,
        status: Int,
        reason: String,
        contentType: String,
        body: String,
    ) {
        val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("Referrer-Policy: no-referrer\r\n")
            append("Content-Security-Policy: default-src 'none'; ")
            append("style-src 'unsafe-inline'; script-src 'self' 'unsafe-inline'; ")
            append("connect-src 'self'; img-src data:\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(header.toByteArray(StandardCharsets.US_ASCII))
        output.write(bodyBytes)
        output.flush()
    }

    private fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(
            a.toByteArray(StandardCharsets.UTF_8),
            b.toByteArray(StandardCharsets.UTF_8),
        )

    private data class Request(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: ByteArray,
        val tooLarge: Boolean,
    )

    private companion object {
        const val MAX_BODY_BYTES = 64 * 1024
        const val MAX_HEADER_BYTES = 8 * 1024
        const val MAX_LINE_BYTES = 4 * 1024
        const val MAX_POST_ATTEMPTS = 5
    }
}
