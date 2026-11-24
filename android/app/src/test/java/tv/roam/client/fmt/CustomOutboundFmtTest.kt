package tv.roam.client.fmt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tv.roam.client.AppConfig
import tv.roam.client.dto.V2rayConfig
import tv.roam.client.enums.EConfigType
import tv.roam.client.util.JsonUtil

class CustomOutboundFmtTest {

    @Test
    fun `single outbound json becomes a custom outbound profile`() {
        val json = """
            {
              "tag": "node",
              "protocol": "vless",
              "settings": { "address": "example.com", "port": 443, "id": "uuid" },
              "streamSettings": { "network": "tcp", "security": "tls" }
            }
        """.trimIndent()

        val (profile, raw) = CustomOutboundFmt.parse(json)

        assertEquals(EConfigType.CUSTOM_OUTBOUND, profile.configType)
        assertEquals("node", profile.remarks)
        assertEquals("example.com", profile.server)
        assertEquals("443", profile.serverPort)
        val outbound = JsonUtil.fromJson(raw, V2rayConfig.OutboundBean::class.java)
        assertNotNull(outbound)
        assertEquals(AppConfig.TAG_PROXY, outbound?.tag)
    }
}
