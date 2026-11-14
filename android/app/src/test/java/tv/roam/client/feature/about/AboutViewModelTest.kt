package tv.roam.client.feature.about

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tv.roam.client.dto.Contributor
import tv.roam.client.dto.TranslatorsCredit

class AboutViewModelTest {

    @Test
    fun `html license text is stripped to readable lines`() {
        val html = """
            <html><head><style>body { color: red; }</style></head>
            <body><h3>Notice</h3><p>A &amp; B</p><p>&copy; 2020 &#169;</p>
            <ul><li>One</li><li>Two</li></ul>
            <pre>line1
            line2</pre></body></html>
        """.trimIndent()

        val lines = htmlToPlainLines(html)

        assertTrue(lines.contains("Notice"))
        assertTrue(lines.contains("A & B"))
        assertTrue(lines.contains("© 2020 ©"))
        assertTrue(lines.contains("• One"))
        assertTrue(lines.contains("line1"))
        assertTrue(lines.contains("line2"))
        assertFalse(lines.any { it.contains("color: red") })
        assertFalse(lines.any { it.contains("<") })
    }

    @Test
    fun `translator credits keep language headings and contributors`() {
        val credits = listOf(
            TranslatorsCredit(
                language = "English",
                contributors = listOf(
                    Contributor(name = "alice", displayName = "Alice"),
                    Contributor(name = "bob"),
                ),
            ),
            TranslatorsCredit(
                language = "فارسی",
                contributors = listOf(Contributor(name = "carol", displayName = "Carol")),
            ),
        )

        val lines = translatorLines(credits)

        assertEquals(
            listOf("English", "  Alice", "  bob", "فارسی", "  Carol"),
            lines,
        )
    }
}
