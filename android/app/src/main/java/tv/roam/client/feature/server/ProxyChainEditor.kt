package tv.roam.client.feature.server

import androidx.annotation.StringRes
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
import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.enums.EConfigType
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.SettingsManager
import tv.roam.client.util.LogUtil

data class ProxyChainEditorState(
    val guid: String = "",
    val remarks: String = "",
    val members: List<String> = listOf("", ""),
    val options: List<String> = emptyList(),
    val error: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedGuid: String? = null,
    val deleted: Boolean = false,
    @StringRes val generalError: Int? = null,
) {
    val isNew: Boolean get() = guid.isBlank()
}

sealed interface ProxyChainEditorAction {
    data class Load(val guid: String) : ProxyChainEditorAction
    data class SetRemarks(val value: String) : ProxyChainEditorAction
    data class SetMember(val index: Int, val value: String) : ProxyChainEditorAction
    data object AddMember : ProxyChainEditorAction
    data class RemoveMember(val index: Int) : ProxyChainEditorAction
    data object Save : ProxyChainEditorAction
    data object Delete : ProxyChainEditorAction
    data object ConsumeSaved : ProxyChainEditorAction
    data object ConsumeDeleted : ProxyChainEditorAction
    data object ConsumeGeneralError : ProxyChainEditorAction
}

/** Validation shared by the ViewModel and JVM tests. */
object ProxyChainRules {
    @StringRes
    fun validate(members: List<String>, options: List<String>): Int? {
        val trimmed = members.map { it.trim() }
        if (trimmed.any { it.isEmpty() }) {
            return R.string.server_proxy_chain_members_unselected
        }
        if (trimmed.size < 2) {
            return R.string.server_proxy_chain_members_insufficient
        }
        if (trimmed.any { it !in options }) {
            return R.string.server_proxy_chain_members_invalid
        }
        return null
    }
}

interface ProxyChainDataSource {    fun load(guid: String): ProfileItem?
    fun save(guid: String, item: ProfileItem): String
    fun remove(guid: String)
    fun selectedGuid(): String?
    fun eligibleRemarks(): List<String>
}

class ProxyChainRepository : ProxyChainDataSource {

    override fun load(guid: String): ProfileItem? = MmkvManager.decodeServerConfig(guid)

    override fun save(guid: String, item: ProfileItem): String =
        MmkvManager.encodeServerConfig(guid, item)

    override fun remove(guid: String) = MmkvManager.removeServer(guid)

    override fun selectedGuid(): String? = MmkvManager.getSelectServer()

    override fun eligibleRemarks(): List<String> =
        SettingsManager.getProfileRemarks(
            excludeConfigTypes = setOf(
                EConfigType.CUSTOM,
                EConfigType.POLICYGROUP,
                EConfigType.PROXYCHAIN,
            ),
        )
}

class ProxyChainEditorViewModel(
    private val dataSource: ProxyChainDataSource = ProxyChainRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProxyChainEditorState())
    val uiState: StateFlow<ProxyChainEditorState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var initialConfig: ProfileItem? = null

    fun onAction(action: ProxyChainEditorAction) {
        when (action) {
            is ProxyChainEditorAction.Load -> load(action.guid)
            is ProxyChainEditorAction.SetRemarks -> update {
                it.copy(remarks = action.value, error = false)
            }

            is ProxyChainEditorAction.SetMember -> update { state ->
                if (action.index !in state.members.indices) {
                    state
                } else {
                    state.copy(
                        members = state.members.toMutableList().also {
                            it[action.index] = action.value
                        },
                        error = false,
                    )
                }
            }

            ProxyChainEditorAction.AddMember -> update {
                it.copy(members = it.members + "")
            }

            is ProxyChainEditorAction.RemoveMember -> update { state ->
                if (state.members.size <= 2 || action.index !in state.members.indices) {
                    state
                } else {
                    state.copy(
                        members = state.members.toMutableList().also { it.removeAt(action.index) },
                    )
                }
            }

            ProxyChainEditorAction.Save -> save()
            ProxyChainEditorAction.Delete -> delete()
            ProxyChainEditorAction.ConsumeSaved -> update { it.copy(savedGuid = null) }
            ProxyChainEditorAction.ConsumeDeleted -> update { it.copy(deleted = false) }
            ProxyChainEditorAction.ConsumeGeneralError -> update { it.copy(generalError = null) }
        }
    }

    private fun update(block: (ProxyChainEditorState) -> ProxyChainEditorState) {
        _uiState.value = block(_uiState.value)
    }

    private fun load(guid: String) {
        loadJob?.cancel()
        _uiState.value = ProxyChainEditorState(guid = guid, isLoading = true)
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val existing = if (guid.isBlank()) null else dataSource.load(guid)
            initialConfig = existing
            val members = existing?.proxyChainProfiles
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.size >= 2 }
                ?: listOf("", "")
            _uiState.value = ProxyChainEditorState(
                guid = guid,
                remarks = existing?.remarks.orEmpty(),
                members = members,
                options = dataSource.eligibleRemarks(),
                isLoading = false,
            )
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        if (state.remarks.isBlank()) {
            update { it.copy(error = true) }
            return
        }
        val members = state.members.map { it.trim() }
        val validationError = ProxyChainRules.validate(members, state.options)
        if (validationError != null) {
            update { it.copy(generalError = validationError) }
            return
        }
        update { it.copy(isSaving = true, generalError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = initialConfig ?: ProfileItem.create(EConfigType.PROXYCHAIN)
                config.remarks = state.remarks.trim()
                config.proxyChainProfiles = members.joinToString(",")
                config.description = members.joinToString(" -> ")
                val savedGuid = dataSource.save(state.guid, config)
                _uiState.value = _uiState.value.copy(
                    guid = savedGuid,
                    isSaving = false,
                    savedGuid = savedGuid,
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to save proxy chain", e)
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
            if (!ServerEditorMapper.canDelete(state.guid, dataSource.selectedGuid())) {
                _uiState.value = _uiState.value.copy(
                    generalError = R.string.tv_editor_delete_selected_forbidden,
                )
                return@launch
            }
            try {
                dataSource.remove(state.guid)
                _uiState.value = _uiState.value.copy(deleted = true)
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to delete proxy chain", e)
                _uiState.value = _uiState.value.copy(generalError = R.string.toast_failure)
            }
        }
    }
}
