package tv.roam.client.feature.checkupdate

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.CheckUpdateResult
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.UpdateCheckerManager
import tv.roam.client.mvvm.BaseViewModel
import tv.roam.client.util.LogUtil

internal data class UpdateUiState(
    val checkPreRelease: Boolean = false,
    val isLoading: Boolean = false,
    val result: CheckUpdateResult? = null,
    @StringRes val messageRes: Int? = null,
)

internal sealed interface UpdateAction {
    data object Load : UpdateAction
    data class SetPreRelease(val enabled: Boolean) : UpdateAction
    data object Check : UpdateAction
    data object ConsumeResult : UpdateAction
    data object ConsumeMessage : UpdateAction
}

class CheckUpdateViewModel(application: Application) : BaseViewModel(application) {

    private val _uiState = MutableStateFlow(UpdateUiState())
    internal val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        onAction(UpdateAction.Load)
    }

    internal fun onAction(action: UpdateAction) {
        when (action) {
            UpdateAction.Load -> load()
            is UpdateAction.SetPreRelease -> setPreRelease(action.enabled)
            UpdateAction.Check -> check()
            UpdateAction.ConsumeResult -> _uiState.value = _uiState.value.copy(result = null)
            UpdateAction.ConsumeMessage -> _uiState.value = _uiState.value.copy(messageRes = null)
        }
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                checkPreRelease = MmkvManager.decodeSettingsBool(
                    AppConfig.PREF_CHECK_UPDATE_PRE_RELEASE,
                    false,
                ),
            )
        }
    }

    private fun setPreRelease(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(checkPreRelease = enabled)
        viewModelScope.launch(Dispatchers.IO) {
            MmkvManager.encodeSettings(AppConfig.PREF_CHECK_UPDATE_PRE_RELEASE, enabled)
        }
    }

    private fun check() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val result = UpdateCheckerManager.checkForUpdate(
                    _uiState.value.checkPreRelease
                )
                _uiState.value = if (result.hasUpdate) {
                    _uiState.value.copy(isLoading = false, result = result)
                } else {
                    _uiState.value.copy(
                        isLoading = false,
                        messageRes = R.string.update_already_latest_version,
                    )
                }
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to check for updates", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messageRes = R.string.toast_failure,
                )
            }
        }
    }
}
