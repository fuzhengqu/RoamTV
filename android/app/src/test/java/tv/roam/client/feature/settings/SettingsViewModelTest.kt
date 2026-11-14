package tv.roam.client.feature.settings

import android.content.Intent
import android.provider.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockConstruction

class SettingsViewModelTest {

    @Test
    fun `system vpn settings availability follows the resolver`() {
        mockConstruction(Intent::class.java) { _, context ->
            assertEquals(listOf(Settings.ACTION_VPN_SETTINGS), context.arguments())
        }.use {
            assertFalse(isSystemVpnSettingsAvailable { false })
            assertTrue(isSystemVpnSettingsAvailable { true })
        }
    }
}
