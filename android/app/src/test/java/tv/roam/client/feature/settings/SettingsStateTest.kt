package tv.roam.client.feature.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tv.roam.client.AppConfig

class SettingsStateTest {

    @Test
    fun `default state is vpn with hev tunnel and local proxy`() {
        val state = SettingsUiState()

        assertTrue(state.isVpn)
        assertTrue(state.hevTunnelEnabled)
        assertTrue(state.effectiveLocalProxy)
        assertEquals("8", state.fieldValue(SettingField.MuxXudpConcurrency))
        assertEquals(AppConfig.DNS_PROXY, state.fieldValue(SettingField.RemoteDns))
    }

    @Test
    fun `proxy only mode disables the hev tunnel requirement`() {
        val state = SettingsUiState(
            dropdowns = mapOf(SettingDropdown.Mode to "Proxy only"),
            switches = mapOf(SettingSwitch.EnableLocalProxy to false),
        )

        assertFalse(state.isVpn)
        assertFalse(state.hevTunnelEnabled)
        assertFalse(state.effectiveLocalProxy)
    }

    @Test
    fun `hev tunnel forces the local proxy on`() {
        val state = SettingsUiState(
            switches = mapOf(
                SettingSwitch.UseHevTunnel to true,
                SettingSwitch.EnableLocalProxy to false,
            ),
        )

        assertTrue(state.hevTunnelEnabled)
        assertTrue(state.effectiveLocalProxy)
    }

    @Test
    fun `mux xudp concurrency parses numbers and falls back on invalid text`() {
        assertEquals(
            4,
            SettingsUiState(
                fields = mapOf(SettingField.MuxXudpConcurrency to "4"),
            ).muxXudpConcurrency,
        )
        assertEquals(
            AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY.toInt(),
            SettingsUiState(
                fields = mapOf(SettingField.MuxXudpConcurrency to "invalid"),
            ).muxXudpConcurrency,
        )
    }

    @Test
    fun `stored values override defaults`() {
        val state = SettingsUiState(
            switches = mapOf(SettingSwitch.Sniffing to false),
            fields = mapOf(SettingField.VpnMtu to "1400"),
            dropdowns = mapOf(SettingDropdown.CoreLogLevel to "debug"),
        )

        assertFalse(state.isOn(SettingSwitch.Sniffing))
        assertEquals("1400", state.fieldValue(SettingField.VpnMtu))
        assertEquals("debug", state.dropdownValue(SettingDropdown.CoreLogLevel))
    }
}
