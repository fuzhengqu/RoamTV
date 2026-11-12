package tv.roam.client.feature.pairing

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end payload protection for the phone pairing page.
 *
 * The key travels only in the QR code fragment, which browsers do not send to
 * the TV's HTTP server. The server sees an IV and AES-GCM ciphertext, never the
 * configuration text. The same primitive is used by the JVM tests.
 */
object PairingCrypto {
    const val KEY_BYTES = 32
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val MAX_PAYLOAD_BYTES = 64 * 1024

    private val random = SecureRandom()

    fun newKey(): ByteArray = ByteArray(KEY_BYTES).also(random::nextBytes)

    fun encrypt(plainText: String, key: ByteArray): String {
        require(key.size == KEY_BYTES) { "Pairing key must be 256 bits" }
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_BITS, iv),
        )
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return base64Url(iv) + "." + base64Url(cipherText)
    }

    /**
     * Returns null for malformed, oversized, tampered or undecryptable input.
     * Callers must treat null as a rejected payload and never log the body.
     */
    fun decrypt(payload: String, key: ByteArray): String? {
        if (key.size != KEY_BYTES) return null
        if (payload.length > MAX_PAYLOAD_BYTES) return null
        val parts = payload.split('.', limit = 2)
        if (parts.size != 2) return null
        return try {
            val iv = decodeBase64Url(parts[0]) ?: return null
            val cipherText = decodeBase64Url(parts[1]) ?: return null
            if (iv.size != IV_BYTES || cipherText.isEmpty()) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(TAG_BITS, iv),
            )
            val plain = cipher.doFinal(cipherText)
            if (plain.size > MAX_PAYLOAD_BYTES) return null
            plain.toString(Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun base64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun decodeBase64Url(value: String): ByteArray? = try {
        Base64.getUrlDecoder().decode(value)
    } catch (_: IllegalArgumentException) {
        null
    }
}
