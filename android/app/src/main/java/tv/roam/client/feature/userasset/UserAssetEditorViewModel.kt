package tv.roam.client.feature.userasset

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.AssetUrlItem
import tv.roam.client.handler.MmkvManager
import tv.roam.client.mvvm.BaseViewModel
import tv.roam.client.util.LogUtil
import tv.roam.client.util.Utils
import java.io.File

internal enum class AssetEditorField { Remarks, Url }

internal data class UserAssetEditorState(
    val guid: String = "",
    val remarks: String = "",
    val url: String = "",
    val isFileAsset: Boolean = false,
    val isNew: Boolean = true,
    val isLoading: Boolean = true,
    val errors: Map<AssetEditorField, Int> = emptyMap(),
    @StringRes val generalError: Int? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

internal sealed interface UserAssetEditorAction {
    data class Load(val guid: String) : UserAssetEditorAction
    data class SetValue(val field: AssetEditorField, val value: String) : UserAssetEditorAction
    data object Save : UserAssetEditorAction
    data object Delete : UserAssetEditorAction
    data object ConsumeSaved : UserAssetEditorAction
    data object ConsumeDeleted : UserAssetEditorAction
    data object ConsumeError : UserAssetEditorAction
}

/**
 * Editor for a user asset URL. A local-file asset is delete-only here because
 * its name is the on-disk file name; renaming it in this screen would orphan
 * or overwrite the imported file.
 */
class UserAssetEditorViewModel(application: Application) : BaseViewModel(application) {

    private val _uiState = MutableStateFlow(UserAssetEditorState())
    internal val uiState: StateFlow<UserAssetEditorState> = _uiState.asStateFlow()

    internal fun onAction(action: UserAssetEditorAction) {
        when (action) {
            is UserAssetEditorAction.Load -> load(action.guid)
            is UserAssetEditorAction.SetValue -> _uiState.value = _uiState.value.copy(
                remarks = if (action.field == AssetEditorField.Remarks) action.value else _uiState.value.remarks,
                url = if (action.field == AssetEditorField.Url) action.value else _uiState.value.url,
                errors = _uiState.value.errors - action.field,
                generalError = null,
            )

            UserAssetEditorAction.Save -> save()
            UserAssetEditorAction.Delete -> delete()
            UserAssetEditorAction.ConsumeSaved -> _uiState.value = _uiState.value.copy(saved = false)
            UserAssetEditorAction.ConsumeDeleted -> _uiState.value = _uiState.value.copy(deleted = false)
            UserAssetEditorAction.ConsumeError -> _uiState.value = _uiState.value.copy(generalError = null)
        }
    }

    private fun load(guid: String) {
        _uiState.value = UserAssetEditorState(guid = guid, isLoading = true)
        if (guid.isEmpty()) {
            _uiState.value = UserAssetEditorState(isLoading = false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val item = MmkvManager.decodeAsset(guid)
            _uiState.value = if (item == null) {
                UserAssetEditorState(
                    guid = guid,
                    isLoading = false,
                    generalError = R.string.toast_failure,
                )
            } else {
                UserAssetEditorState(
                    guid = guid,
                    remarks = item.remarks,
                    url = item.url,
                    isFileAsset = item.url == UserAssetViewModel.FILE_ASSET_URL,
                    isNew = false,
                    isLoading = false,
                )
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isFileAsset) return
        val remarks = state.remarks.trim()
        val url = state.url.trim()
        val duplicate = remarks.isNotEmpty() && MmkvManager.decodeAssetUrls().any {
            it.guid != state.guid && it.assetUrl.remarks == remarks
        }
        val errors = validateAssetEditor(remarks, url, duplicate)
        if (errors.isNotEmpty()) {
            _uiState.value = state.copy(errors = errors)
            return
        }

        _uiState.value = state.copy(remarks = remarks, url = url, errors = emptyMap())
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = if (state.guid.isEmpty()) null else MmkvManager.decodeAsset(state.guid)
                if (existing != null) {
                    runCatching { File(assetDir(), existing.remarks).delete() }
                        .onFailure {
                            LogUtil.e(AppConfig.TAG, "Failed to delete edited asset file", it)
                        }
                }
                val assetId = state.guid.ifEmpty { Utils.getUuid() }
                val item = existing ?: AssetUrlItem()
                item.remarks = remarks
                item.url = url
                MmkvManager.encodeAsset(assetId, item)
                _uiState.value = _uiState.value.copy(
                    guid = assetId,
                    isNew = false,
                    saved = true,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to save asset URL", e)
                _uiState.value = _uiState.value.copy(generalError = R.string.toast_failure)
            }
        }
    }

    private fun delete() {
        val state = _uiState.value
        if (state.guid.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item = MmkvManager.decodeAsset(state.guid)
                if (item != null) {
                    runCatching { File(assetDir(), item.remarks).delete() }
                        .onFailure {
                            LogUtil.e(AppConfig.TAG, "Failed to delete asset file", it)
                        }
                }
                MmkvManager.removeAssetUrl(state.guid)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to delete asset source", e)
                _uiState.value = _uiState.value.copy(generalError = R.string.toast_failure)
            }
        }
    }

    private fun assetDir(): File = File(Utils.userAssetPath(app))
}

internal fun validateAssetEditor(
    remarks: String,
    url: String,
    duplicateRemarks: Boolean,
): Map<AssetEditorField, Int> {
    val errors = linkedMapOf<AssetEditorField, Int>()
    if (remarks.isBlank()) {
        errors[AssetEditorField.Remarks] = R.string.tv_editor_error_required
    } else if (duplicateRemarks) {
        errors[AssetEditorField.Remarks] = R.string.msg_remark_is_duplicate
    }
    if (url.isBlank()) {
        errors[AssetEditorField.Url] = R.string.tv_editor_error_required
    }
    return errors
}
