package tv.roam.client.feature.routing

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.RulesetItem
import tv.roam.client.enums.RoutingType
import tv.roam.client.util.LogUtil

data class RoutingRuleRow(
    val id: String,
    val remarks: String,
    val outboundTag: String,
    val enabled: Boolean,
    val locked: Boolean,
)

data class RoutingListState(
    val rows: List<RoutingRuleRow> = emptyList(),
    val domainStrategy: String = "",
    val strategies: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    @StringRes val messageRes: Int? = null,
)

sealed interface RoutingListAction {
    data object Load : RoutingListAction
    data class SetDomainStrategy(val index: Int) : RoutingListAction
    data class Delete(val id: String) : RoutingListAction
    data class ImportPreset(val type: RoutingType) : RoutingListAction
    data object ImportClipboard : RoutingListAction
    data object ExportClipboard : RoutingListAction
    data object ConsumeMessage : RoutingListAction
}

class RoutingListViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSource: RoutingDataSource = RoutingRepository(application)

    private val _uiState = MutableStateFlow(RoutingListState())
    val uiState: StateFlow<RoutingListState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun onAction(action: RoutingListAction) {
        when (action) {
            RoutingListAction.Load -> load()
            is RoutingListAction.SetDomainStrategy -> setDomainStrategy(action.index)
            is RoutingListAction.Delete -> delete(action.id)
            is RoutingListAction.ImportPreset -> importPreset(action.type)
            RoutingListAction.ImportClipboard -> importClipboard()
            RoutingListAction.ExportClipboard -> exportClipboard()
            RoutingListAction.ConsumeMessage -> _uiState.value =
                _uiState.value.copy(messageRes = null)
        }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val rules = dataSource.loadRules()
                val strategies = dataSource.strategies()
                _uiState.value = _uiState.value.copy(
                    rows = rules.map { rule ->
                        RoutingRuleRow(
                            id = rule.id,
                            remarks = rule.remarks.orEmpty().ifBlank { rule.id },
                            outboundTag = rule.outboundTag.ifBlank {
                                RoutingRules.DEFAULT_OUTBOUND
                            },
                            enabled = rule.enabled,
                            locked = rule.locked == true,
                        )
                    },
                    domainStrategy = dataSource.domainStrategy(),
                    strategies = strategies,
                    isLoading = false,
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to load routing rules", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messageRes = R.string.toast_failure,
                )
            }
        }
    }

    private fun setDomainStrategy(index: Int) {
        val value = _uiState.value.strategies.getOrNull(index) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dataSource.setDomainStrategy(value)
            _uiState.value = _uiState.value.copy(domainStrategy = value)
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataSource.deleteRule(id)
                load()
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to delete routing rule", e)
                _uiState.value = _uiState.value.copy(messageRes = R.string.toast_failure)
            }
        }
    }

    private fun importPreset(type: RoutingType) {
        if (_uiState.value.isImporting) return
        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataSource.importPreset(type)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    messageRes = R.string.toast_success,
                )
                load()
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to import routing preset", e)
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    messageRes = R.string.toast_failure,
                )
            }
        }
    }

    private fun importClipboard() {
        if (_uiState.value.isImporting) return
        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch(Dispatchers.IO) {
            val success = try {
                dataSource.importFromClipboard()
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to import routing rules from clipboard", e)
                false
            }
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                messageRes = if (success) R.string.toast_success else R.string.toast_failure,
            )
            if (success) load()
        }
    }

    private fun exportClipboard() {
        viewModelScope.launch(Dispatchers.IO) {
            val success = try {
                dataSource.exportToClipboard()
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to export routing rules", e)
                false
            }
            _uiState.value = _uiState.value.copy(
                messageRes = if (success) R.string.toast_success else R.string.toast_failure,
            )
        }
    }
}

class RoutingEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSource: RoutingDataSource = RoutingRepository(application)

    private val _uiState = MutableStateFlow(RoutingEditorState())
    val uiState: StateFlow<RoutingEditorState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var initial: RulesetItem? = null

    fun onAction(action: RoutingEditorAction) {
        when (action) {
            is RoutingEditorAction.Load -> load(action.ruleId)
            is RoutingEditorAction.SetValue -> update { state ->
                state.copy(
                    values = state.values + (action.field to action.value),
                    errors = state.errors - action.field,
                    generalError = null,
                )
            }

            is RoutingEditorAction.SetSwitch -> update { state ->
                state.copy(switches = state.switches + (action.field to action.value))
            }

            RoutingEditorAction.Save -> save()
            RoutingEditorAction.Delete -> delete()
            RoutingEditorAction.ConsumeSaved -> update { it.copy(saved = false) }
            RoutingEditorAction.ConsumeDeleted -> update { it.copy(deleted = false) }
            RoutingEditorAction.ConsumeGeneralError -> update {
                it.copy(generalError = null)
            }
        }
    }

    private fun update(block: (RoutingEditorState) -> RoutingEditorState) {
        _uiState.value = block(_uiState.value)
    }

    private fun load(ruleId: String) {
        loadJob?.cancel()
        _uiState.value = RoutingEditorState(isLoading = true)
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val rule = dataSource.loadRule(ruleId)
            initial = rule
            val state = if (rule != null) {
                RoutingRules.fromRuleset(rule)
            } else {
                RoutingRules.newState()
            }
            _uiState.value = state.copy(
                outboundOptions = dataSource.outboundSuggestions(),
                isLoading = false,
            )
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val errors = RoutingRules.validate(state)
        if (errors.isNotEmpty()) {
            _uiState.value = state.copy(
                errors = errors,
                validationAttempt = state.validationAttempt + 1,
            )
            return
        }
        _uiState.value = state.copy(isSaving = true, generalError = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rule = RoutingRules.toRuleset(state, initial)
                dataSource.saveRule(rule)
                initial = rule
                _uiState.value = _uiState.value.copy(
                    ruleId = rule.id,
                    isSaving = false,
                    saved = true,
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to save routing rule", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    generalError = R.string.toast_failure,
                )
            }
        }
    }

    private fun delete() {
        val state = _uiState.value
        if (state.ruleId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataSource.deleteRule(state.ruleId)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to delete routing rule", e)
                _uiState.value = _uiState.value.copy(generalError = R.string.toast_failure)
            }
        }
    }
}
