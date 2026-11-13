package tv.roam.client.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.SubscriptionItem
import tv.roam.client.util.LogUtil

class SubscriptionEditorViewModel(
    private val dataSource: SubscriptionDataSource = SubscriptionRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionEditorState())
    val uiState: StateFlow<SubscriptionEditorState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var initial: SubscriptionItem? = null

    fun onAction(action: SubscriptionEditorAction) {
        when (action) {
            is SubscriptionEditorAction.Load -> load(action.guid)
            is SubscriptionEditorAction.SetValue -> update { state ->
                state.copy(
                    values = state.values + (action.field to action.value),
                    errors = state.errors - action.field,
                    generalError = null,
                )
            }

            is SubscriptionEditorAction.SetSwitch -> update { state ->
                state.copy(
                    switches = state.switches + (action.field to action.value),
                    errors = state.errors - action.field,
                )
            }

            SubscriptionEditorAction.Save -> save()
            SubscriptionEditorAction.Delete -> delete()
            SubscriptionEditorAction.ConsumeSaved -> update { it.copy(savedGuid = null) }
            SubscriptionEditorAction.ConsumeDeleted -> update { it.copy(deleted = false) }
            SubscriptionEditorAction.ConsumeGeneralError -> update {
                it.copy(generalError = null)
            }
        }
    }

    private fun update(block: (SubscriptionEditorState) -> SubscriptionEditorState) {
        _uiState.value = block(_uiState.value)
    }

    private fun load(guid: String) {
        loadJob?.cancel()
        _uiState.value = SubscriptionEditorState(guid = guid, isLoading = true)
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val item = dataSource.loadOne(guid)
            initial = item
            val values = item?.toEditorValues() ?: linkedMapOf(
                SubscriptionField.Remarks to "",
                SubscriptionField.Url to "",
                SubscriptionField.UserAgent to "",
                SubscriptionField.Headers to "",
                SubscriptionField.Filter to "",
                SubscriptionField.Interval to "1440",
                SubscriptionField.PrevProfile to "",
                SubscriptionField.NextProfile to "",
            )
            _uiState.value = SubscriptionEditorState(
                guid = guid,
                values = values,
                switches = mapOf(
                    SubscriptionField.Enabled to (item?.enabled ?: true),
                    SubscriptionField.AutoUpdate to (item?.autoUpdate ?: false),
                    SubscriptionField.AllowInsecure to (item?.allowInsecureUrl ?: false),
                ),
                profiles = dataSource.suggestions(),
                isLoading = false,
            )
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val remarks = state.value(SubscriptionField.Remarks).trim()
        val url = state.value(SubscriptionField.Url).trim()
        val allowInsecure = state.isOn(SubscriptionField.AllowInsecure)
        val autoUpdate = state.isOn(SubscriptionField.AutoUpdate)
        val interval = state.value(SubscriptionField.Interval).toLongOrNull() ?: 0L
        val errors = linkedMapOf<SubscriptionField, Int>()
        if (!SubscriptionRules.validateRemarks(remarks)) {
            errors[SubscriptionField.Remarks] = R.string.tv_editor_error_required
        }
        if (!SubscriptionRules.validateUrl(url, allowInsecure)) {
            errors[SubscriptionField.Url] = R.string.toast_invalid_url
        }
        if (!SubscriptionRules.validateInterval(autoUpdate, interval)) {
            errors[SubscriptionField.Interval] = R.string.toast_invalid_update_interval
        }
        if (errors.isNotEmpty()) {
            update {
                it.copy(
                    errors = errors,
                    validationAttempt = it.validationAttempt + 1,
                )
            }
            return
        }
        update { it.copy(isSaving = true, generalError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item = initial ?: SubscriptionItem()
                item.remarks = remarks
                item.url = url
                item.userAgent = state.value(SubscriptionField.UserAgent)
                item.requestHeaders = state.value(SubscriptionField.Headers)
                item.filter = state.value(SubscriptionField.Filter)
                item.enabled = state.isOn(SubscriptionField.Enabled)
                item.autoUpdate = autoUpdate
                item.updateInterval = interval
                item.allowInsecureUrl = allowInsecure
                item.prevProfile = state.value(SubscriptionField.PrevProfile).ifBlank { null }
                item.nextProfile = state.value(SubscriptionField.NextProfile).ifBlank { null }
                val savedGuid = dataSource.save(state.guid, item)
                if (state.guid.isNotBlank()) {
                    dataSource.syncOne(savedGuid)
                }
                _uiState.value = _uiState.value.copy(
                    guid = savedGuid,
                    isSaving = false,
                    savedGuid = savedGuid,
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to save subscription", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    generalError = R.string.toast_failure,
                )
            }
        }
    }

    private fun delete() {
        val state = _uiState.value
        if (state.guid.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataSource.delete(state.guid)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to delete subscription", e)
                _uiState.value = _uiState.value.copy(generalError = R.string.toast_failure)
            }
        }
    }
}
