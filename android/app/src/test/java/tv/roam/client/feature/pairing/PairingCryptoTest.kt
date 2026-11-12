package tv.roam.client.feature.pairing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Base64

class PairingCryptoTest {

    private val key = ByteArray(32) { it.toByte() }

    @Test
    fun `round trip preserves utf8 text`() {
        val text = "vless://example\n第二行"
        val payload = PairingCrypto.encrypt(text, key)

        assertEquals(text, PairingCrypto.decrypt(payload, key))
    }

    @Test
    fun `sjcl gcm payload is readable by the android crypto`() {
        // Generated with sjcl 1.0.9 using the same key and a fixed 96-bit IV.
        val payload = "AAECAwQFBgcICQoL.L2e6d6rFsnrkM_7l1vN7EjJAAMsVPPXOyL3oR0M"

        assertEquals("hello pairing", PairingCrypto.decrypt(payload, key))
    }

    @Test
    fun `wrong key and tampered payload are rejected`() {
        val payload = PairingCrypto.encrypt("secret", key)
        val wrongKey = ByteArray(32) { (it + 1).toByte() }

        assertNull(PairingCrypto.decrypt(payload, wrongKey))
        val parts = payload.split('.')
        val tampered = parts[0] + "." +
            (if (parts[1].first() == 'A') 'B' else 'A') + parts[1].drop(1)
        assertNull(PairingCrypto.decrypt(tampered, key))
        assertNull(PairingCrypto.decrypt("not-a-payload", key))
    }

    @Test
    fun `base64url helper is stable`() {
        val encoded = PairingCrypto.base64Url(key)
        assertNotNull(PairingCrypto.decodeBase64Url(encoded))
        assertEquals(key.toList(), Base64.getUrlDecoder().decode(encoded).toList())
    }
}
