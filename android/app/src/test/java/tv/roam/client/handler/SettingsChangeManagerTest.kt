package tv.roam.client.handler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tv.roam.client.AppConfig

class SettingsChangeManagerTest {

    @BeforeEach
    fun clearPendingFlags() {
        SettingsChangeManager.consumeRestartService()
        SettingsChangeManager.consumeSetupGroupTab()
    }

    @Test
    fun `ui only setting refreshes groups without restarting the core`() {
        SettingsChangeManager.notifySettingChanged(AppConfig.PREF_DOUBLE_COLUMN_DISPLAY)

        assertFalse(SettingsChangeManager.consumeRestartService())
        assertTrue(SettingsChangeManager.consumeSetupGroupTab())
    }

    @Test
    fun `core setting requests a restart and refreshes groups`() {
        SettingsChangeManager.notifySettingChanged(AppConfig.PREF_LOGLEVEL)

        assertTrue(SettingsChangeManager.consumeRestartService())
        assertTrue(SettingsChangeManager.consumeSetupGroupTab())
    }

    @Test
    fun `consume clears the pending restart`() {
        SettingsChangeManager.notifySettingChanged(AppConfig.PREF_LOGLEVEL)

        assertTrue(SettingsChangeManager.consumeRestartService())
        assertFalse(SettingsChangeManager.consumeRestartService())
    }

    @Test
    fun `consume clears the pending group refresh`() {
        SettingsChangeManager.notifySettingChanged(AppConfig.PREF_LOGLEVEL)

        assertTrue(SettingsChangeManager.consumeSetupGroupTab())
        assertFalse(SettingsChangeManager.consumeSetupGroupTab())
    }
}
