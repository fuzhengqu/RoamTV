package tv.roam.client.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.roam.client.R
import tv.roam.client.dto.SubscriptionUpdateResult
import tv.roam.client.util.LogUtil
import tv.roam.client.AppConfig

data class SubscriptionRow(
    val guid: String,
    val remarks: String,
    val url: String,
    val enabled: Boolean,
    val autoUpdate: Boolean,
    val updateInterval: Long,
)

data class SubscriptionsState(
    val rows: List<SubscriptionRow> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val updateResult: SubscriptionUpdateResult? = null,
    val messageRes: Int? = null,
)

sealed interface SubscriptionsAction {
    data object Load : SubscriptionsAction
    data object UpdateAll : SubscriptionsAction
    data class Delete(val guid: String) : SubscriptionsAction
    data object ConsumeResult : SubscriptionsAction
    data object ConsumeMessage : SubscriptionsAction
}

class SubscriptionListViewModel(
    private val dataSource: SubscriptionDataSource = SubscriptionRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsState())
    val uiState: StateFlow<SubscriptionsState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun onAction(action: SubscriptionsAction) {
        when (action) {
            SubscriptionsAction.Load -> load()
            SubscriptionsAction.UpdateAll -> updateAll()
            is SubscriptionsAction.Delete -> delete(action.guid)
            SubscriptionsAction.ConsumeResult -> _uiState.value =
                _uiState.value.copy(updateResult = null)

            SubscriptionsAction.ConsumeMessage -> _uiState.value =
                _uiState.value.copy(messageRes = null)
        }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val rows = dataSource.load().map { cache ->
                SubscriptionRow(
                    guid = cache.guid,
                    remarks = cache.subscription.remarks.ifBlank { cache.guid },
                    url = cache.subscription.url,
                    enabled = cache.subscription.enabled,
                    autoUpdate = cache.subscription.autoUpdate,
                    updateInterval = cache.subscription.updateInterval,
                )
            }
            _uiState.value = _uiState.value.copy(rows = rows, isLoading = false)
        }
    }

    private fun updateAll() {
        val state = _uiState.value
        if (state.isUpdating || state.isLoading) return
        _uiState.value = state.copy(isUpdating = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = dataSource.updateAll()
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    updateResult = result,
                )
                load()
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to update subscriptions", e)
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    messageRes = R.string.toast_failure,
                )
            }
        }
    }

    private fun delete(guid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataSource.delete(guid)
                load()
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to delete subscription", e)
                _uiState.value = _uiState.value.copy(messageRes = R.string.toast_failure)
            }
        }
    }
}
