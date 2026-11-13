package tv.roam.client.feature.pairing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class PairingHttpServerTest {

    private val key = ByteArray(32) { (it * 3).toByte() }

    @Test
    fun `serves the page and imports one encrypted payload`() {
        val session = PairingSession(
            id = "test-session",
            code = "123456",
            key = key,
            expiresAtMillis = System.currentTimeMillis() + 60_000,
        )
        val received = AtomicReference<String>()
        val latch = CountDownLatch(1)
        val server = PairingHttpServer(
            session = session,
            bindAddress = InetAddress.getLoopbackAddress(),
            pageHtml = "<html><title>RoamTV pairing</title></html>",
            sjclJs = "// sjcl",
            onText = {
                received.set(it)
                latch.countDown()
            },
        )
        val port = server.start()
        try {
            val page = request("GET", "http://127.0.0.1:$port/s/test-session?code=123456")
            assertEquals(200, page.code)
            assertTrue(page.body.contains("RoamTV pairing"))

            val js = request("GET", "http://127.0.0.1:$port/sjcl.js")
            assertEquals(200, js.code)
            assertEquals("// sjcl", js.body)

            val forbidden = request(
                "POST",
                "http://127.0.0.1:$port/api/upload",
                body = PairingCrypto.encrypt("hello", key),
                sessionId = "wrong",
            )
            assertEquals(403, forbidden.code)

            val tampered = request(
                "POST",
                "http://127.0.0.1:$port/api/upload",
                body = PairingCrypto.encrypt("hello", key).dropLast(1) + "A",
                sessionId = session.id,
            )
            assertEquals(400, tampered.code)

            val accepted = request(
                "POST",
                "http://127.0.0.1:$port/api/upload",
                body = PairingCrypto.encrypt("hello from phone", key),
                sessionId = session.id,
            )
            assertEquals(200, accepted.code)
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertEquals("hello from phone", received.get())
        } finally {
            server.close()
        }
    }

    private data class Response(val code: Int, val body: String)

    private fun request(
        method: String,
        url: String,
        body: String? = null,
        sessionId: String? = null,
    ): Response {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 3_000
        connection.readTimeout = 3_000
        if (sessionId != null) {
            connection.setRequestProperty("X-Pairing-Session", sessionId)
        }
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "text/plain")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        return Response(code, text)
    }
}
