package tv.roam.client.feature.server

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.enums.EConfigType

class ServerEditorMapperTest {

    @Test
    fun `new vless state has a usable default port and tcp transport`() {
        val state = ServerEditorMapper.initialState(EConfigType.VLESS)

        assertEquals(AppConfig.DEFAULT_PORT.toString(), state.value(ServerField.Port))
        assertEquals("tcp", state.value(ServerField.Network))
        assertEquals("none", state.value(ServerField.HeaderType))
        assertFalse(state.isOn(ServerField.AllowInsecure))
    }

    @Test
    fun `vless reality fields survive a round trip`() {
        val item = ProfileItem.create(EConfigType.VLESS).copy(
            remarks = "Reality node",
            server = "example.com",
            serverPort = "443",
            password = "uuid",
            method = "none",
            flow = "xtls-rprx-vision",
            network = "ws",
            host = "cdn.example.com",
            path = "/ws",
            security = AppConfig.REALITY,
            sni = "example.com",
            publicKey = "public-key",
            shortId = "abcd",
            spiderX = "/",
            fingerPrint = "chrome",
        )

        val state = ServerEditorMapper.fromProfile(item)
        val restored = ServerEditorMapper.toProfile(state, ProfileItem.create(EConfigType.VLESS))

        assertEquals("Reality node", restored.remarks)
        assertEquals("example.com", restored.server)
        assertEquals("uuid", restored.password)
        assertEquals("none", restored.method)
        assertEquals("xtls-rprx-vision", restored.flow)
        assertEquals("ws", restored.network)
        assertEquals("cdn.example.com", restored.host)
        assertEquals("/ws", restored.path)
        assertEquals(AppConfig.REALITY, restored.security)
        assertEquals("public-key", restored.publicKey)
        assertEquals("abcd", restored.shortId)
        assertEquals("/", restored.spiderX)
        assertEquals("chrome", restored.fingerPrint)
    }

    @Test
    fun `wireguard fields survive a round trip`() {
        val item = ProfileItem.create(EConfigType.WIREGUARD).copy(
            remarks = "WG",
            server = "wg.example.com",
            serverPort = "51820",
            secretKey = "secret",
            publicKey = "public",
            preSharedKey = "psk",
            reserved = "1,2,3",
            localAddress = "10.0.0.2/32",
            mtu = 1280,
            remoteDNS = "9.9.9.9",
        )

        val restored = ServerEditorMapper.toProfile(
            ServerEditorMapper.fromProfile(item),
            ProfileItem.create(EConfigType.WIREGUARD),
        )

        assertEquals("secret", restored.secretKey)
        assertEquals("public", restored.publicKey)
        assertEquals("psk", restored.preSharedKey)
        assertEquals("1,2,3", restored.reserved)
        assertEquals("10.0.0.2/32", restored.localAddress)
        assertEquals(1280, restored.mtu)
        assertEquals("9.9.9.9", restored.remoteDNS)
    }

    @Test
    fun `hysteria2 defaults to tls when security is blank`() {
        val state = ServerEditorMapper.initialState(EConfigType.HYSTERIA2).copy(
            values = mapOf(
                ServerField.Remarks to "H2",
                ServerField.Address to "h2.example.com",
                ServerField.Password to "pass",
            ),
        )

        val item = ServerEditorMapper.toProfile(state, ProfileItem.create(EConfigType.HYSTERIA2))

        assertEquals(AppConfig.TLS, item.security)
        assertTrue(ServerEditorMapper.validate(state).isEmpty())
    }

    @Test
    fun `required fields and port are validated`() {
        val state = ServerEditorMapper.initialState(EConfigType.VMESS)
        val errors = ServerEditorMapper.validate(state)

        assertEquals(R.string.tv_editor_error_required, errors[ServerField.Remarks])
        assertEquals(R.string.tv_editor_error_required, errors[ServerField.Address])
        assertEquals(R.string.tv_editor_error_required, errors[ServerField.Password])
        assertNull(errors[ServerField.Port])
    }

    @Test
    fun `socks password is optional`() {
        val state = ServerEditorMapper.initialState(EConfigType.SOCKS).copy(
            values = mapOf(
                ServerField.Remarks to "SOCKS",
                ServerField.Address to "127.0.0.1",
                ServerField.Port to "1080",
            ),
        )

        assertTrue(ServerEditorMapper.validate(state).isEmpty())
    }

    @Test
    fun `trojan requires stream security`() {
        val state = ServerEditorMapper.initialState(EConfigType.TROJAN).copy(
            values = mapOf(
                ServerField.Remarks to "Trojan",
                ServerField.Address to "example.com",
                ServerField.Port to "443",
                ServerField.Password to "pass",
                ServerField.StreamSecurity to "",
            ),
        )

        assertEquals(
            R.string.tv_editor_error_required,
            ServerEditorMapper.validate(state)[ServerField.StreamSecurity],
        )
    }

    @Test
    fun `invalid transport json is rejected`() {
        val state = ServerEditorMapper.initialState(EConfigType.VMESS).copy(
            values = mapOf(
                ServerField.Remarks to "VMess",
                ServerField.Address to "example.com",
                ServerField.Port to "443",
                ServerField.Password to "uuid",
                ServerField.XhttpExtra to "{not json",
            ),
        )

        assertEquals(
            R.string.tv_editor_error_json,
            ServerEditorMapper.validate(state)[ServerField.XhttpExtra],
        )
    }

    @Test
    fun `custom editor requires content`() {
        val state = ServerEditorMapper.initialState(EConfigType.CUSTOM)

        assertEquals(
            R.string.tv_editor_error_required,
            ServerEditorMapper.validate(state)[ServerField.Content],
        )
    }

    @Test
    fun `selected server cannot be deleted from the editor`() {
        assertFalse(ServerEditorMapper.canDelete("guid-1", "guid-1"))
        assertFalse(ServerEditorMapper.canDelete("", null))
        assertTrue(ServerEditorMapper.canDelete("guid-1", "guid-2"))
        assertTrue(ServerEditorMapper.canDelete("guid-1", null))
    }

    @Test
    fun `xhttp shows its extra and browser dialer fields`() {
        val state = ServerEditorMapper.initialState(EConfigType.VMESS).copy(
            values = mapOf(ServerField.Network to "xhttp"),
        )

        val fields = ServerEditorDefinition.visibleFields(state)

        assertTrue(fields.contains(ServerField.XhttpExtra))
        assertTrue(fields.contains(ServerField.BrowserDialerMode))
        assertTrue(fields.contains(ServerField.XhttpMode))
    }
}
