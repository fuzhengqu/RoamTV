package tv.roam.client.feature.backup

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.tencent.mmkv.MMKV
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.WebDavConfig
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.SettingsChangeManager
import tv.roam.client.handler.SettingsManager
import tv.roam.client.handler.WebDavManager
import tv.roam.client.mvvm.BaseViewModel
import tv.roam.client.util.LogUtil
import tv.roam.client.util.ZipUtil

internal data class BackupUiState(
    val webDavConfig: WebDavConfig? = null,
    val isBusy: Boolean = false,
    @StringRes val messageRes: Int? = null,
    val message: String? = null,
    val shareFilePath: String? = null,
    val restored: Boolean = false,
)

internal sealed interface BackupAction {
    data object Load : BackupAction
    data class SaveWebDav(val config: WebDavConfig) : BackupAction
    data class BackupLocal(val targetUri: Uri) : BackupAction
    data object BackupWebDav : BackupAction
    data object Share : BackupAction
    data class RestoreLocal(val sourceUri: Uri) : BackupAction
    data object RestoreWebDav : BackupAction
    data object CleanupProfiles : BackupAction
    data object ConsumeMessage : BackupAction
    data object ConsumeShare : BackupAction
    data object ConsumeRestore : BackupAction
}

class BackupViewModel(application: Application) : BaseViewModel(application) {

    private val _uiState = MutableStateFlow(BackupUiState())
    internal val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        onAction(BackupAction.Load)
    }

    internal fun onAction(action: BackupAction) {
        when (action) {
            BackupAction.Load -> load()
            is BackupAction.SaveWebDav -> saveWebDav(action.config)
            is BackupAction.BackupLocal -> backupLocal(action.targetUri)
            BackupAction.BackupWebDav -> backupWebDav()
            BackupAction.Share -> share()
            is BackupAction.RestoreLocal -> restoreLocal(action.sourceUri)
            BackupAction.RestoreWebDav -> restoreWebDav()
            BackupAction.CleanupProfiles -> cleanupProfiles()
            BackupAction.ConsumeMessage -> _uiState.value =
                _uiState.value.copy(messageRes = null, message = null)
            BackupAction.ConsumeShare -> _uiState.value = _uiState.value.copy(shareFilePath = null)
            BackupAction.ConsumeRestore -> _uiState.value = _uiState.value.copy(restored = false)
        }
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                webDavConfig = MmkvManager.decodeWebDavConfig(),
            )
        }
    }

    private fun saveWebDav(config: WebDavConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            MmkvManager.encodeWebDavConfig(config)
            _uiState.value = _uiState.value.copy(
                webDavConfig = config,
                messageRes = R.string.toast_success,
            )
        }
    }

    private fun backupLocal(targetUri: Uri) {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            var zipFile: File? = null
            try {
                val result = backupConfigurationToCache()
                if (!result.first) {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        messageRes = R.string.toast_failure,
                    )
                    return@launch
                }
                zipFile = File(result.second)
                val output = app.contentResolver.openOutputStream(targetUri)
                    ?: throw IllegalStateException("No output stream")
                output.use { target ->
                    zipFile.inputStream().use { input -> input.copyTo(target) }
                }
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    messageRes = R.string.toast_success,
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to export backup", e)
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    messageRes = R.string.toast_failure,
                )
            } finally {
                zipFile?.delete()
            }
        }
    }

    private fun backupWebDav() {
        val config = _uiState.value.webDavConfig
        if (config == null || config.baseUrl.isEmpty()) {
            _uiState.value = _uiState.value.copy(messageRes = R.string.title_webdav_config_setting_unknown)
            return
        }
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            var tempFile: File? = null
            try {
                val result = backupConfigurationToCache()
                if (!result.first) {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        messageRes = R.string.toast_failure,
                    )
                    return@launch
                }
                tempFile = File(result.second)
                WebDavManager.init(config)
                val ok = WebDavManager.uploadFile(tempFile, AppConfig.WEBDAV_BACKUP_FILE_NAME)
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    messageRes = if (ok) R.string.toast_success else R.string.toast_failure,
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "WebDAV backup error", e)
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    messageRes = R.string.toast_failure,
                )
            } finally {
                tempFile?.delete()
            }
        }
    }

    private fun share() {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupConfigurationToCache()
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                shareFilePath = if (result.first) result.second else null,
                messageRes = if (result.first) null else R.string.toast_failure,
            )
        }
    }

    private fun restoreLocal(sourceUri: Uri) {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val temp = File(app.cacheDir, "restore_upload_${System.currentTimeMillis()}.zip")
            try {
                val input = app.contentResolver.openInputStream(sourceUri)
                    ?: throw IllegalStateException("No input stream")
                input.use { source ->
                    temp.outputStream().use { target -> source.copyTo(target) }
                }
                finishRestore(temp)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Error during file restore", e)
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    messageRes = R.string.toast_failure,
                )
            } finally {
                temp.delete()
            }
        }
    }

    private fun restoreWebDav() {
        val config = _uiState.value.webDavConfig
        if (config == null || config.baseUrl.isEmpty()) {
            _uiState.value = _uiState.value.copy(messageRes = R.string.title_webdav_config_setting_unknown)
            return
        }
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            var target: File? = null
            try {
                target = File(app.cacheDir, "download_${System.currentTimeMillis()}.zip")
                WebDavManager.init(config)
                val ok = WebDavManager.downloadFile(AppConfig.WEBDAV_BACKUP_FILE_NAME, target)
                if (!ok) {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        messageRes = R.string.toast_failure,
                    )
                    return@launch
                }
                finishRestore(target)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "WebDAV download error", e)
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    messageRes = R.string.toast_failure,
                )
            } finally {
                target?.delete()
            }
        }
    }

    private suspend fun finishRestore(zipFile: File) {
        val success = performRestore(zipFile)
        if (success) {
            SettingsManager.initApp(app)
        }
        _uiState.value = _uiState.value.copy(
            isBusy = false,
            messageRes = if (success) R.string.toast_success else R.string.toast_failure,
            restored = success,
        )
    }

    private fun cleanupProfiles() {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(isBusy = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val removed = MmkvManager.removeOrphanedServerProfiles()
                _uiState.value = if (removed == null) {
                    _uiState.value.copy(
                        isBusy = false,
                        messageRes = R.string.toast_profile_storage_cleanup_skipped,
                    )
                } else {
                    _uiState.value.copy(
                        isBusy = false,
                        message = getString(R.string.toast_profile_storage_cleanup, removed),
                    )
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to clean up profile storage", e)
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    messageRes = R.string.toast_failure,
                )
            }
        }
    }

    private fun backupConfigurationToCache(): Pair<Boolean, String> {
        val appName = getString(R.string.app_name)
        val dateFormatted = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss",
            Locale.getDefault()
        ).format(System.currentTimeMillis())
        val folderName = "${appName}_$dateFormatted"
        val backupDir = app.cacheDir.absolutePath + "/$folderName"
        val outputZipFilePath = "${app.cacheDir.absolutePath}/$folderName.zip"

        val count = MMKV.backupAllToDirectory(backupDir)
        if (count <= 0) {
            return Pair(false, "")
        }

        return if (ZipUtil.zipFromFolder(backupDir, outputZipFilePath)) {
            File(backupDir).deleteRecursively()
            Pair(true, outputZipFilePath)
        } else {
            File(backupDir).deleteRecursively()
            Pair(false, "")
        }
    }

    private suspend fun performRestore(zipFile: File): Boolean = runCatching {
        val backupDir = File(app.cacheDir, "restore_${System.nanoTime()}")
        try {
            if (!ZipUtil.unzipToFolder(zipFile, backupDir.absolutePath)) {
                return@runCatching false
            }
            val count = MMKV.restoreAllFromDirectory(backupDir.absolutePath)
            SettingsChangeManager.makeSetupGroupTab()
            SettingsChangeManager.makeRestartService()
            count > 0
        } finally {
            backupDir.deleteRecursively()
        }
    }.getOrElse { e ->
        LogUtil.e(AppConfig.TAG, "Failed to restore backup", e)
        false
    }
}
