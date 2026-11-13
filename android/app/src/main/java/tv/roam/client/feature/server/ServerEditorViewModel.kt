package tv.roam.client.feature.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tv.roam.client.R
import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.enums.EConfigType
import tv.roam.client.util.LogUtil
import tv.roam.client.AppConfig

/**
 * Owns the TV server editor form, validation and persistence.
 *
 * The editor is activity-scoped because [tv.roam.client.ui.TvApp] has a single
 * Activity. [load] resets the form for each route so switching between servers
 * cannot leak values from the previous editor.
 */
class ServerEditorViewModel(
    private val dataSource: ServerEditorDataSource = ServerEditorRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerEditorState())
    val uiState: StateFlow<ServerEditorState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var initialConfig: ProfileItem = ProfileItem.create(EConfigType.VMESS)
    private var restartOnSave = false

    fun onAction(action: ServerEditorAction) {
        when (action) {
            is ServerEditorAction.Load -> load(
                action.guid,
                action.configType,
                action.restartOnSave,
            )

            is ServerEditorAction.SetValue -> updateValue(action.field, action.value)
            is ServerEditorAction.SetSwitch -> updateSwitch(action.field, action.value)
            ServerEditorAction.Save -> save()
            ServerEditorAction.Delete -> delete()
            ServerEditorAction.ConsumeSaved -> _uiState.value =
                _uiState.value.copy(savedGuid = null)

            ServerEditorAction.ConsumeDeleted -> _uiState.value =
                _uiState.value.copy(deleted = false)

            ServerEditorAction.ConsumeGeneralError -> _uiState.value =
                _uiState.value.copy(generalError = null)
        }
    }

    private fun load(guid: String, configType: EConfigType, restartOnSave: Boolean) {
        this.restartOnSave = restartOnSave
        loadJob?.cancel()
        _uiState.value = ServerEditorState(
            guid = guid,
            configType = configType,
            isLoading = true,
        )
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val existing = if (guid.isBlank()) null else dataSource.load(guid)
            initialConfig = existing ?: ProfileItem.create(configType)
            val state = if (existing != null) {
                ServerEditorMapper.fromProfile(existing).copy(
                    guid = guid,
                    configType = existing.configType,
                )
            } else {
                ServerEditorMapper.initialState(configType).copy(guid = guid)
            }
            val withRaw = if (existing?.configType == EConfigType.CUSTOM) {
                state.copy(
                    values = state.values + (
                        ServerField.Content to dataSource.loadRaw(guid).orEmpty()
                        ),
                )
            } else {
                state
            }
            _uiState.value = withRaw.copy(isLoading = false)
        }
    }

    private fun updateValue(field: ServerField, value: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            values = current.values + (field to value),
            errors = current.errors - field,
            generalError = null,
        )
    }

    private fun updateSwitch(field: ServerField, value: Boolean) {
        val current = _uiState.value
        _uiState.value = current.copy(
            switches = current.switches + (field to value),
            errors = current.errors - field,
            generalError = null,
        )
    }

    private fun save() {
        val current = _uiState.value
        if (current.isSaving || current.isLoading) return
        val errors = ServerEditorMapper.validate(current)
        if (errors.isNotEmpty()) {
            _uiState.value = current.copy(
                errors = errors,
                validationAttempt = current.validationAttempt + 1,
            )
            return
        }
        _uiState.value = current.copy(isSaving = true, generalError = null)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedGuid: String
                if (current.configType == EConfigType.CUSTOM) {
                    val content = current.value(ServerField.Content)
                    val parsed = dataSource.parseCustom(content)
                    val item = parsed.copy(
                        configType = EConfigType.CUSTOM,
                        remarks = current.value(ServerField.Remarks).trim(),
                    )
                    item.description = dataSource.description(item)
                    savedGuid = dataSource.save(current.guid, item)
                    dataSource.saveRaw(savedGuid, content)
                } else {
                    val item = ServerEditorMapper.toProfile(current, initialConfig)
                    item.description = dataSource.description(item)
                    savedGuid = dataSource.save(current.guid, item)
                }
                _uiState.value = _uiState.value.copy(
                    guid = savedGuid,
                    isSaving = false,
                    savedGuid = savedGuid,
                    restartRequired = restartOnSave,
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to save server profile", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    generalError = if (current.configType == EConfigType.CUSTOM) {
                        R.string.toast_malformed_json
                    } else {
                        R.string.toast_failure
                    },
                )
            }
        }
    }

    private fun delete() {
        val current = _uiState.value
        if (current.guid.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (!ServerEditorMapper.canDelete(current.guid, dataSource.selectedGuid())) {
                _uiState.value = _uiState.value.copy(
                    generalError = R.string.tv_editor_delete_selected_forbidden,
                )
                return@launch
            }
            try {
                dataSource.remove(current.guid)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to delete server profile", e)
                _uiState.value = _uiState.value.copy(generalError = R.string.toast_failure)
            }
        }
    }
}
