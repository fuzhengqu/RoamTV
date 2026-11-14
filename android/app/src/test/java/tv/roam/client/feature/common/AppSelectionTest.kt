package tv.roam.client.feature.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppSelectionTest {

    @Test
    fun `invert adds unselected and removes selected packages`() {
        val result = AppSelection.invert(
            currentSelection = setOf("a", "b"),
            packageNames = listOf("b", "c"),
        )

        assertEquals(setOf("a", "c"), result)
    }

    @Test
    fun `normal mode selects packages present in the list`() {
        val result = AppSelection.fromProxyList(
            packageNames = listOf("com.example.a", "com.example.b"),
            proxyAppList = "com.example.a\ncom.example.other",
            bypassApps = false,
            forceGoogleApps = false,
        )

        assertEquals(setOf("com.example.a"), result)
    }

    @Test
    fun `bypass mode selects packages not present in the list`() {
        val result = AppSelection.fromProxyList(
            packageNames = listOf("com.example.a", "com.example.b"),
            proxyAppList = "com.example.a\n",
            bypassApps = true,
            forceGoogleApps = false,
        )

        assertEquals(setOf("com.example.b"), result)
    }

    @Test
    fun `forced google selection excludes webview`() {
        val result = AppSelection.fromProxyList(
            packageNames = listOf(
                "com.google.android.youtube",
                "com.google.android.webview",
                "com.example.app",
            ),
            proxyAppList = "",
            bypassApps = false,
            forceGoogleApps = true,
        )

        assertEquals(setOf("com.google.android.youtube"), result)
    }
}
