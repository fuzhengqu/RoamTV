package tv.roam.client.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tv.roam.client.dto.GitHubRelease
import tv.roam.client.util.JsonUtil

class UpdateCheckerManagerTest {

    @Test
    fun `stable-only mode ignores pre-releases`() {
        val stable = release("v1.00")
        val pre = release("v1.01", prerelease = true)

        assertSame(stable, UpdateCheckerManager.selectLatestRelease(listOf(pre, stable), false))
        assertEquals(
            "v1.01",
            UpdateCheckerManager.selectLatestRelease(listOf(pre, stable), true)?.tagName,
        )
    }

    @Test
    fun `highest version wins regardless of list order`() {
        val releases = listOf(
            release("v0.99", prerelease = true),
            release("v1.02", prerelease = true),
            release("v1.01"),
        )

        assertEquals(
            "v1.02",
            UpdateCheckerManager.selectLatestRelease(releases, true)?.tagName,
        )
        assertEquals(
            "v1.01",
            UpdateCheckerManager.selectLatestRelease(releases, false)?.tagName,
        )
    }

    @Test
    fun `draft releases are excluded`() {
        val stable = release("v1.01")
        val draft = release("v1.02", draft = true)

        assertSame(stable, UpdateCheckerManager.selectLatestRelease(listOf(draft, stable), true))
    }

    @Test
    fun `stable release wins when versions tie`() {
        val stable = release("v1.01", publishedAt = "2026-01-01T00:00:00Z")
        val pre = release("v1.01", prerelease = true, publishedAt = "2026-02-01T00:00:00Z")

        assertSame(stable, UpdateCheckerManager.selectLatestRelease(listOf(pre, stable), true))
    }

    @Test
    fun `newer publication wins between equally stable versions`() {
        val older = release("v1.01", publishedAt = "2026-01-01T00:00:00Z")
        val newer = release("v1.01", publishedAt = "2026-02-01T00:00:00Z")

        assertSame(newer, UpdateCheckerManager.selectLatestRelease(listOf(older, newer), false))
    }

    @Test
    fun `github payload maps draft and prerelease flags`() {
        val json = """
            [
              {"tag_name":"v1.02","body":"pre","assets":[],"prerelease":true,"draft":false,"published_at":"2026-02-01T00:00:00Z"},
              {"tag_name":"v1.03","body":"draft","assets":[],"prerelease":false,"draft":true,"published_at":"2026-03-01T00:00:00Z"},
              {"tag_name":"v1.01","body":"stable","assets":[],"prerelease":false,"draft":false,"published_at":"2026-01-01T00:00:00Z"}
            ]
        """.trimIndent()
        val releases = JsonUtil.fromJsonSafe(json, Array<GitHubRelease>::class.java)!!.toList()

        assertEquals("v1.02", UpdateCheckerManager.selectLatestRelease(releases, true)?.tagName)
        assertEquals("v1.01", UpdateCheckerManager.selectLatestRelease(releases, false)?.tagName)
    }

    @Test
    fun `empty list has no candidate`() {
        assertNull(UpdateCheckerManager.selectLatestRelease(emptyList(), true))
    }

    @Test
    fun `version comparison ignores v prefix and suffixed tags`() {
        assertTrue(UpdateCheckerManager.compareVersions("v1.01", "1.00") > 0)
        assertEquals(0, UpdateCheckerManager.compareVersions("1.01-beta", "1.01"))
        assertEquals(0, UpdateCheckerManager.compareVersions("1.01+build", "v1.01"))
        assertEquals(0, UpdateCheckerManager.compareVersions("1.0", "1.00"))
        assertTrue(UpdateCheckerManager.compareVersions("v2.0", "1.99") > 0)
        assertEquals(0, UpdateCheckerManager.compareVersions("", "v0"))
    }

    private fun release(
        tag: String,
        prerelease: Boolean = false,
        draft: Boolean = false,
        publishedAt: String = "",
    ) = GitHubRelease(
        tagName = tag,
        body = "",
        assets = emptyList(),
        prerelease = prerelease,
        publishedAt = publishedAt,
        draft = draft,
    )
}
