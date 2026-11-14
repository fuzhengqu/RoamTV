package tv.roam.client.feature.userasset

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.UrlContentRequest
import tv.roam.client.dto.entities.AssetUrlCache
import tv.roam.client.dto.entities.AssetUrlItem
import tv.roam.client.extension.concatUrl
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.SettingsManager
import tv.roam.client.mvvm.BaseViewModel
import tv.roam.client.util.HttpUtil
import tv.roam.client.util.LogUtil
import tv.roam.client.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import java.io.File

internal data class AssetFileMetadata(val length: Long, val lastModified: Long)

internal data class UserAssetUiState(
    val assets: List<AssetUrlCache> = emptyList(),
    val fileMetadata: Map<String, AssetFileMetadata> = emptyMap(),
    val geoFilesSource: String = "",
    val isLoading: Boolean = true,
    val isDownloading: Boolean = false,
    @StringRes val messageRes: Int? = null,
    val downloadResult: UserAssetViewModel.GeoDownloadResult? = null,
)

/**
 * Resolves a user-visible asset name to a single safe file name inside the
 * asset directory. The picker may report a decorated or nested display name.
 */
internal fun sanitizeAssetFileName(raw: String?): String? {
    val name = raw
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.trim()
    return name?.takeIf { it.isNotEmpty() && it != "." && it != ".." }
}

class UserAssetViewModel(application: Application) : BaseViewModel(application) {
    private val builtInGeoFiles = listOf(AppConfig.GEOSITE_DAT, AppConfig.GEOIP_DAT, AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT)

    private val _uiState = MutableStateFlow(UserAssetUiState())
    internal val uiState: StateFlow<UserAssetUiState> = _uiState.asStateFlow()
    private var reloadJob: Job? = null

    /**
     * Loads the asset list and the persisted geo-file source for the TV screen.
     */
    fun load() {
        reloadJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true)
        reloadJob = viewModelScope.launch(Dispatchers.IO) {
            refresh(currentGeoFilesSource(), assetDir())
        }
    }

    fun selectGeoSource(source: String) {
        reloadJob?.cancel()
        _uiState.value = _uiState.value.copy(geoFilesSource = source, isLoading = true)
        reloadJob = viewModelScope.launch(Dispatchers.IO) {
            MmkvManager.encodeSettings(AppConfig.PREF_GEO_FILES_SOURCES, source)
            refresh(source, assetDir())
        }
    }

    fun downloadAll() {
        if (_uiState.value.isDownloading) return
        _uiState.value = _uiState.value.copy(isDownloading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val source = currentGeoFilesSource()
                val dir = assetDir()
                val snapshot = buildAssetList(MmkvManager.decodeAssetUrls(), source)
                _uiState.value = _uiState.value.copy(
                    assets = snapshot,
                    geoFilesSource = source,
                )
                val result = downloadGeoFiles(
                    extDir = dir,
                    httpPort = SettingsManager.getHttpPort(),
                    proxyUsername = SettingsManager.getSocksUsername(),
                    proxyPassword = SettingsManager.getSocksPassword(),
                )
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadResult = result,
                )
                refresh(source, dir)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to download asset files", e)
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    messageRes = R.string.toast_failure,
                )
            }
        }
    }

    /**
     * Copies a document picked by the system file chooser into the asset
     * directory and records it as a local-file asset. The record is removed
     * again when the copy fails, so the list never points at a partial file.
     */
    fun importFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = assetDir()
            val name = sanitizeAssetFileName(queryDisplayName(uri))
                ?: sanitizeAssetFileName(uri.lastPathSegment)
            if (name == null) {
                _uiState.value = _uiState.value.copy(messageRes = R.string.toast_asset_copy_failed)
                return@launch
            }
            if (MmkvManager.decodeAssetUrls().any { it.assetUrl.remarks == name }) {
                _uiState.value = _uiState.value.copy(messageRes = R.string.msg_remark_is_duplicate)
                return@launch
            }

            val assetId = Utils.getUuid()
            MmkvManager.encodeAsset(assetId, AssetUrlItem(name, FILE_ASSET_URL))
            val copied = runCatching {
                val input = app.contentResolver.openInputStream(uri)
                    ?: return@runCatching false
                input.use { source ->
                    File(dir, name).outputStream().use { target ->
                        source.copyTo(target)
                    }
                }
                true
            }.getOrElse { e ->
                LogUtil.e(AppConfig.TAG, "Failed to copy asset file: $name", e)
                false
            }

            if (!copied) {
                MmkvManager.removeAssetUrl(assetId)
                File(dir, name).delete()
            }
            _uiState.value = _uiState.value.copy(
                messageRes = if (copied) R.string.toast_success else R.string.toast_asset_copy_failed,
            )
            refresh(_uiState.value.geoFilesSource.ifEmpty { currentGeoFilesSource() }, dir)
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(messageRes = null)
    }

    fun consumeDownloadResult() {
        _uiState.value = _uiState.value.copy(downloadResult = null)
    }

    private suspend fun refresh(geoFilesSource: String, extDir: File) {
        val snapshot = buildAssetList(MmkvManager.decodeAssetUrls(), geoFilesSource)
        val files = extDir.listFiles().orEmpty().associateBy { it.name }
        val metadata = snapshot.mapNotNull { asset ->
            files[asset.assetUrl.remarks]?.let { file ->
                asset.guid to AssetFileMetadata(file.length(), file.lastModified())
            }
        }.toMap()
        currentCoroutineContext().ensureActive()
        _uiState.value = _uiState.value.copy(
            assets = snapshot,
            fileMetadata = metadata,
            geoFilesSource = geoFilesSource,
            isLoading = false,
        )
    }

    private fun currentGeoFilesSource(): String =
        MmkvManager.decodeSettingsString(AppConfig.PREF_GEO_FILES_SOURCES)
            ?: AppConfig.GEO_FILES_SOURCES.first()

    private fun assetDir(): File = File(Utils.userAssetPath(app))

    private fun queryDisplayName(uri: Uri): String? = try {
        app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else {
                null
            }
        }
    } catch (e: Exception) {
        LogUtil.e(AppConfig.TAG, "Failed to read picked asset name", e)
        null
    }

    internal fun buildAssetList(
        decodedAssets: List<AssetUrlCache>?,
        geoFilesSource: String
    ): List<AssetUrlCache> {
        val savedAssets = decodedAssets ?: emptyList()
        val builtInItems = builtInGeoFiles
            .filter { geoFile -> savedAssets.none { it.assetUrl.remarks == geoFile } }
            .map {
                AssetUrlCache(
                    // Built-in rows have no persisted GUID; keep their UI identity across reloads.
                    "$BUILTIN_GUID_PREFIX$it",
                    AssetUrlItem(
                        it,
                        String.format(AppConfig.GITHUB_DOWNLOAD_URL, geoFilesSource).concatUrl(it),
                        locked = true
                    )
                )
            }
        // Force update URL for geoip-only-cn-private.dat
        return (builtInItems + savedAssets).map { cache ->
            if (cache.assetUrl.remarks == AppConfig.GEOIP_ONLY_CN_PRIVATE_DAT) {
                cache.copy(
                    assetUrl = cache.assetUrl.copy(
                        url = AppConfig.GEOIP_ONLY_CN_PRIVATE_URL
                    )
                )
            } else {
                cache
            }
        }
    }

    fun downloadGeoFiles(
        extDir: File,
        httpPort: Int,
        proxyUsername: String? = null,
        proxyPassword: String? = null
    ): GeoDownloadResult {
        val snapshot = uiState.value.assets
        var successCount = 0
        val failures = mutableListOf<String>()

        snapshot.forEach { cache ->
            val item = cache.assetUrl
            val portsToTry = if (httpPort == 0) listOf(0) else listOf(httpPort, 0)
            if (portsToTry.any { tryDownload(item, extDir, it, proxyUsername, proxyPassword) }) {
                successCount++
            } else {
                failures.add(item.remarks)
            }
        }

        return GeoDownloadResult(successCount, failures.size, failures)
    }

    private fun tryDownload(
        item: AssetUrlItem,
        extDir: File,
        httpPort: Int,
        proxyUsername: String? = null,
        proxyPassword: String? = null
    ): Boolean {
        val targetTemp = File(extDir, item.remarks + "_temp")
        val target = File(extDir, item.remarks)
        try {
            if (
                HttpUtil.downloadToFile(
                    UrlContentRequest(
                        url = item.url,
                        timeout = 15000,
                        httpPort = httpPort,
                        proxyUsername = proxyUsername,
                        proxyPassword = proxyPassword
                    ),
                    targetTemp
                )
            ) {
                targetTemp.renameTo(target)
                return true
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to download geo file: ${item.remarks}", e)
        }
        return false
    }

    data class GeoDownloadResult(
        val successCount: Int,
        val failureCount: Int,
        val failedAssets: List<String>
    )

    companion object {
        internal const val BUILTIN_GUID_PREFIX = "builtin:"
        internal const val FILE_ASSET_URL = "file"
    }
}
