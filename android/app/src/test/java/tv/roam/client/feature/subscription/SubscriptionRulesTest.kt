package tv.roam.client.feature.subscription

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tv.roam.client.AppConfig
import tv.roam.client.dto.entities.SubscriptionItem

class SubscriptionRulesTest {

    @Test
    fun `remarks are required`() {
        assertFalse(SubscriptionRules.validateRemarks(""))
        assertFalse(SubscriptionRules.validateRemarks("   "))
        assertTrue(SubscriptionRules.validateRemarks("Main"))
    }

    @Test
    fun `empty url is allowed`() {
        assertTrue(SubscriptionRules.validateUrl("", allowInsecure = false))
    }

    @Test
    fun `https url is allowed`() {
        assertTrue(SubscriptionRules.validateUrl("https://example.com/sub", false))
    }

    @Test
    fun `public http url needs explicit permission`() {
        assertFalse(SubscriptionRules.validateUrl("http://example.com/sub", false))
        assertTrue(SubscriptionRules.validateUrl("http://example.com/sub", true))
    }

    @Test
    fun `private and loopback http urls are allowed`() {
        assertTrue(SubscriptionRules.validateUrl("http://127.0.0.1:8080/sub", false))
        assertTrue(SubscriptionRules.validateUrl("http://192.168.1.2/sub", false))
        assertTrue(SubscriptionRules.validateUrl("http://10.0.0.2/sub", false))
        assertTrue(SubscriptionRules.validateUrl("http://172.16.0.2/sub", false))
        assertFalse(SubscriptionRules.validateUrl("http://172.32.0.2/sub", false))
    }

    @Test
    fun `other schemes and malformed urls are rejected`() {
        assertFalse(SubscriptionRules.validateUrl("ftp://example.com/sub", true))
        assertFalse(SubscriptionRules.validateUrl("not a url", true))
    }

    @Test
    fun `auto update interval must meet the minimum`() {
        assertTrue(SubscriptionRules.validateInterval(false, 0))
        assertFalse(SubscriptionRules.validateInterval(true, 1))
        assertTrue(
            SubscriptionRules.validateInterval(
                true,
                AppConfig.SUBSCRIPTION_MIN_INTERVAL_MINUTES,
            )
        )
    }

    @Test
    fun `editor values map every persisted field`() {
        val item = SubscriptionItem(
            remarks = "Main",
            url = "https://example.com/sub",
            userAgent = "UA",
            requestHeaders = "{}",
            filter = "hk",
            updateInterval = 60,
            prevProfile = "entry",
            nextProfile = "exit",
        )

        val values = item.toEditorValues()

        assertEquals("Main", values[SubscriptionField.Remarks])
        assertEquals("https://example.com/sub", values[SubscriptionField.Url])
        assertEquals("UA", values[SubscriptionField.UserAgent])
        assertEquals("{}", values[SubscriptionField.Headers])
        assertEquals("hk", values[SubscriptionField.Filter])
        assertEquals("60", values[SubscriptionField.Interval])
        assertEquals("entry", values[SubscriptionField.PrevProfile])
        assertEquals("exit", values[SubscriptionField.NextProfile])
    }
}
