package tv.roam.client.handler

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tv.roam.client.AppConfig
import tv.roam.client.BuildConfig
import tv.roam.client.dto.CheckUpdateResult
import tv.roam.client.dto.GitHubRelease
import tv.roam.client.dto.UrlContentRequest
import tv.roam.client.extension.concatUrl
import tv.roam.client.util.HttpUtil
import tv.roam.client.util.JsonUtil
import tv.roam.client.util.LogUtil

object UpdateCheckerManager {
    private const val RELEASES_PAGE_SIZE = 100

    suspend fun checkForUpdate(includePreRelease: Boolean = false): CheckUpdateResult =
        withContext(Dispatchers.IO) {
            val releases = loadReleases()
            var latest = if (!releases.isNullOrEmpty()) {
                selectLatestRelease(releases, includePreRelease)
            } else {
                null
            }
            if (latest == null && !includePreRelease) {
                latest = loadLatestStable()
                    ?: throw IllegalStateException("Failed to get latest release")
            }
            if (latest == null) {
                throw IllegalStateException("No matching release found")
            }

            val latestVersion = latest.tagName.removePrefix("v").removePrefix("V")
            LogUtil.i(
                AppConfig.TAG,
                "Found new version: $latestVersion (current: ${BuildConfig.VERSION_NAME})"
            )

            if (compareVersions(latest.tagName, BuildConfig.VERSION_NAME) <= 0) {
                return@withContext CheckUpdateResult(hasUpdate = false)
            }

            val downloadUrl = getDownloadUrl(
                latest,
                Build.SUPPORTED_ABIS.firstOrNull(),
            )
            return@withContext CheckUpdateResult(
                hasUpdate = true,
                latestVersion = latestVersion,
                releaseNotes = latest.body,
                downloadUrl = downloadUrl,
                isPreRelease = latest.prerelease,
            )
        }

    /**
     * Selects the highest semantic version. When versions tie, a stable
     * release wins over a pre-release, then the newer publication date wins.
     */
    internal fun selectLatestRelease(
        releases: List<GitHubRelease>,
        includePreRelease: Boolean,
    ): GitHubRelease? = releases.asSequence()
        .filter { !it.draft }
        .filter { includePreRelease || !it.prerelease }
        .maxWithOrNull { left, right ->
            val version = compareVersions(left.tagName, right.tagName)
            when {
                version != 0 -> version
                left.prerelease != right.prerelease -> if (left.prerelease) -1 else 1
                else -> left.publishedAt.compareTo(right.publishedAt)
            }
        }

    internal fun compareVersions(left: String, right: String): Int {
        val leftParts = parseVersion(left)
        val rightParts = parseVersion(right)
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val leftPart = leftParts.getOrElse(index) { 0 }
            val rightPart = rightParts.getOrElse(index) { 0 }
            if (leftPart != rightPart) return leftPart.compareTo(rightPart)
        }
        return 0
    }

    private fun parseVersion(tag: String): List<Int> {
        val normalized = tag.trim().removePrefix("v").removePrefix("V")
        val numeric = normalized.substringBefore('-').substringBefore('+')
        return numeric.split('.').map { segment ->
            segment.takeWhile(Char::isDigit).toIntOrNull() ?: 0
        }
    }

    private suspend fun loadReleases(): List<GitHubRelease>? {
        val url = "${AppConfig.APP_API_URL}?per_page=$RELEASES_PAGE_SIZE"
        return try {
            val response = fetchContent(url) ?: return null
            JsonUtil.fromJsonSafe(response, Array<GitHubRelease>::class.java)?.toList()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to get release list", e)
            null
        }
    }

    private suspend fun loadLatestStable(): GitHubRelease? = try {
        val response = fetchContent(AppConfig.APP_API_URL.concatUrl("latest"))
        response?.let { JsonUtil.fromJsonSafe(it, GitHubRelease::class.java) }
    } catch (e: Exception) {
        LogUtil.e(AppConfig.TAG, "Failed to get latest release", e)
        null
    }

    private suspend fun fetchContent(url: String): String? {
        var response = HttpUtil.getUrlContent(
            UrlContentRequest(
                url = url,
                timeout = 5000,
            )
        )
        if (response.isNullOrEmpty()) {
            response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 5000,
                    httpPort = SettingsManager.getHttpPort(),
                    proxyUsername = SettingsManager.getSocksUsername(),
                    proxyPassword = SettingsManager.getSocksPassword(),
                )
            )
        }
        return response
    }

    private fun getDownloadUrl(release: GitHubRelease, abi: String?): String? {
        if (abi.isNullOrBlank()) return null
        val isFdroid = BuildConfig.APPLICATION_ID.contains("fdroid", ignoreCase = true)
        val candidates = release.assets.filter { asset ->
            asset.name.endsWith(".apk", ignoreCase = true) &&
                asset.name.contains("fdroid", ignoreCase = true) == isFdroid
        }
        val exact = candidates.firstOrNull {
            it.name.contains("_$abi.apk", ignoreCase = true)
        }
        val universal = candidates.firstOrNull {
            it.name.contains("_universal.apk", ignoreCase = true)
        }
        return (exact ?: universal)?.browserDownloadUrl
    }
}
