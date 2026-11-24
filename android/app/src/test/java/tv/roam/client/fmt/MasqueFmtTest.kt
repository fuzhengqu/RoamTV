package tv.roam.client.fmt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tv.roam.client.AppConfig
import tv.roam.client.enums.EConfigType
import tv.roam.client.enums.NetworkType

class MasqueFmtTest {

    @Test
    fun `masque uri maps warp and transport fields`() {
        val profile = MasqueFmt.parse(
            "masque://private-key@example.com:443" +
                "?publickey=public-key&address=172.16.0.2%2F32&dns=1.1.1.1" +
                "&alpn=h3&host=cdn.example.com&path=%2Fmasque" +
                "&headers=%7B%22X-Test%22%3A%22yes%22%7D#WARP"
        )

        requireNotNull(profile)
        assertEquals(EConfigType.MASQUE, profile.configType)
        assertEquals("example.com", profile.server)
        assertEquals("443", profile.serverPort)
        assertEquals("private-key", profile.secretKey)
        assertEquals("public-key", profile.publicKey)
        assertEquals("172.16.0.2/32", profile.localAddress)
        assertEquals("1.1.1.1", profile.remoteDNS)
        assertEquals("h3", profile.alpn)
        assertEquals("cdn.example.com", profile.host)
        assertEquals("/masque", profile.path)
        assertEquals("""{"X-Test":"yes"}""", profile.headers)
        assertEquals(NetworkType.MASQUE.type, profile.network)
        assertEquals(AppConfig.TLS, profile.security)
        assertFalse(profile.insecure!!)
        assertEquals("WARP", profile.remarks)
    }

    @Test
    fun `masque without query is rejected`() {
        assertNull(MasqueFmt.parse("masque://example.com:443"))
    }

    @Test
    fun `masque rejects unsupported alpn`() {
        assertNull(
            MasqueFmt.parse(
                "masque://example.com:443?publickey=key&alpn=http%2F1.1#bad"
            )
        )
    }

    @Test
    fun `masque uri survives round trip`() {
        val profile = MasqueFmt.parse(
            "masque://private@example.com:443?publickey=public&address=172.16.0.2%2F32" +
                "&dns=1.1.1.1&headers=%7B%22X-Test%22%3A%22yes%22%7D#node"
        )
        requireNotNull(profile)

        val exported = "masque://${MasqueFmt.toUri(profile)}"
        val reparsed = MasqueFmt.parse(exported)

        requireNotNull(reparsed)
        assertEquals("example.com", reparsed.server)
        assertEquals("443", reparsed.serverPort)
        assertEquals("private", reparsed.secretKey)
        assertEquals("public", reparsed.publicKey)
        assertEquals("172.16.0.2/32", reparsed.localAddress)
        assertEquals("1.1.1.1", reparsed.remoteDNS)
        assertEquals("""{"X-Test":"yes"}""", reparsed.headers)
        assertEquals("h3", reparsed.alpn)
    }
}
