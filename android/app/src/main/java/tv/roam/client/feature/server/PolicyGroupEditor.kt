package tv.roam.client.feature.server

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
import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.enums.EConfigType
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.SettingsManager
import tv.roam.client.util.LogUtil

data class EditorOption(val id: String, val label: String)

enum class PolicyField(@StringRes val labelRes: Int) {
    Remarks(R.string.server_lab_remarks),
    Filter(R.string.title_policy_group_subscription_filter),
    Type(R.string.title_policy_group_type),
    Subscription(R.string.title_policy_group_subscription_id),
    TestOutbounds(R.string.title_policy_group_test_outbounds),
    Fallback(R.string.title_policy_group_fallback),
}

data class PolicyGroupEditorState(
    val guid: String = "",
    val remarks: String = "",
    val filter: String = "",
    val typeIndex: Int = 0,
    val subscriptionIndex: Int = 0,
    val testOutbounds: Boolean = false,
    val fallbackIndex: Int = 0,
    val subscriptions: List<EditorOption> = emptyList(),
    val fallbacks: List<String> = emptyList(),
    val typeLabels: List<String> = emptyList(),
    val errors: Set<PolicyField> = emptySet(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedGuid: String? = null,
    val deleted: Boolean = false,
    @StringRes val generalError: Int? = null,
) {
    val isNew: Boolean get() = guid.isBlank()
}

sealed interface PolicyGroupEditorAction {
    data class Load(val guid: String) : PolicyGroupEditorAction
    data class SetRemarks(val value: String) : PolicyGroupEditorAction
    data class SetFilter(val value: String) : PolicyGroupEditorAction
    data class SetType(val index: Int) : PolicyGroupEditorAction
    data class SetSubscription(val index: Int) : PolicyGroupEditorAction
    data class SetTestOutbounds(val value: Boolean) : PolicyGroupEditorAction
    data class SetFallback(val index: Int) : PolicyGroupEditorAction
    data object Save : PolicyGroupEditorAction
    data object Delete : PolicyGroupEditorAction
    data object ConsumeSaved : PolicyGroupEditorAction
    data object ConsumeDeleted : PolicyGroupEditorAction
    data object ConsumeGeneralError : PolicyGroupEditorAction
}

interface PolicyGroupDataSource {
    fun load(guid: String): ProfileItem?
    fun save(guid: String, item: ProfileItem): String
    fun remove(guid: String)
    fun selectedGuid(): String?
    fun subscriptions(allLabel: String): List<EditorOption>
    fun fallbacks(): List<String>
    fun typeLabels(): List<String>
}

class PolicyGroupRepository(private val app: Application) : PolicyGroupDataSource {

    override fun load(guid: String): ProfileItem? = MmkvManager.decodeServerConfig(guid)

    override fun save(guid: String, item: ProfileItem): String =
        MmkvManager.encodeServerConfig(guid, item)

    override fun remove(guid: String) = MmkvManager.removeServer(guid)

    override fun selectedGuid(): String? = MmkvManager.getSelectServer()

    override fun subscriptions(allLabel: String): List<EditorOption> {
        val options = mutableListOf(EditorOption("", allLabel))
        MmkvManager.decodeSubscriptions().forEach { sub ->
            val label = sub.subscription.remarks.ifBlank { sub.guid }
            options += EditorOption(sub.guid, label)
        }
        return options
    }

    override fun fallbacks(): List<String> {
        val profiles = SettingsManager.getProfileRemarks(
            excludeConfigTypes = setOf(EConfigType.CUSTOM, EConfigType.POLICYGROUP),
        ).filter { it != AppConfig.TAG_PROXY }
        return (listOf("") + AppConfig.BUILTIN_OUTBOUND_TAGS + profiles).distinct()
    }

    override fun typeLabels(): List<String> =
        app.resources.getStringArray(R.array.policy_group_type).toList()
}

class PolicyGroupEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSource: PolicyGroupDataSource = PolicyGroupRepository(application)

    private val _uiState = MutableStateFlow(PolicyGroupEditorState())
    val uiState: StateFlow<PolicyGroupEditorState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var initialConfig: ProfileItem? = null

    fun onAction(action: PolicyGroupEditorAction) {
        when (action) {
            is PolicyGroupEditorAction.Load -> load(action.guid)
            is PolicyGroupEditorAction.SetRemarks -> update {
                it.copy(remarks = action.value, errors = it.errors - PolicyField.Remarks)
            }

            is PolicyGroupEditorAction.SetFilter -> update { it.copy(filter = action.value) }
            is PolicyGroupEditorAction.SetType -> update { it.copy(typeIndex = action.index) }
            is PolicyGroupEditorAction.SetSubscription -> update {
                it.copy(subscriptionIndex = action.index)
            }

            is PolicyGroupEditorAction.SetTestOutbounds -> update {
                it.copy(testOutbounds = action.value)
            }

            is PolicyGroupEditorAction.SetFallback -> update {
                it.copy(fallbackIndex = action.index)
            }

            PolicyGroupEditorAction.Save -> save()
            PolicyGroupEditorAction.Delete -> delete()
            PolicyGroupEditorAction.ConsumeSaved -> update { it.copy(savedGuid = null) }
            PolicyGroupEditorAction.ConsumeDeleted -> update { it.copy(deleted = false) }
            PolicyGroupEditorAction.ConsumeGeneralError -> update { it.copy(generalError = null) }
        }
    }

    private fun update(block: (PolicyGroupEditorState) -> PolicyGroupEditorState) {
        _uiState.value = block(_uiState.value)
    }

    private fun load(guid: String) {
        loadJob?.cancel()
        _uiState.value = PolicyGroupEditorState(guid = guid, isLoading = true)
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val existing = if (guid.isBlank()) null else dataSource.load(guid)
            initialConfig = existing
            val allLabel = getApplication<Application>().getString(R.string.filter_config_all)
            val subscriptions = dataSource.subscriptions(allLabel)
            val fallbacks = dataSource.fallbacks()
            val typeLabels = dataSource.typeLabels()
            val subscriptionIndex = subscriptions.indexOfFirst {
                it.id == existing?.policyGroupSubscriptionId
            }.coerceAtLeast(0)
            val fallbackIndex = fallbacks.indexOf(
                existing?.policyGroupFallbackTag.orEmpty()
            ).coerceAtLeast(0)
            _uiState.value = PolicyGroupEditorState(
                guid = _uiState.value.guid,
                remarks = existing?.remarks.orEmpty(),
                filter = existing?.policyGroupFilter.orEmpty(),
                typeIndex = existing?.policyGroupType?.toIntOrNull() ?: 0,
                subscriptionIndex = subscriptionIndex,
                testOutbounds = existing?.policyGroupTestOutbounds == true,
                fallbackIndex = fallbackIndex,
                subscriptions = subscriptions,
                fallbacks = fallbacks,
                typeLabels = typeLabels,
                isLoading = false,
            )
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val errors = if (state.remarks.isBlank()) setOf(PolicyField.Remarks) else emptySet()
        if (errors.isNotEmpty()) {
            update { it.copy(errors = errors) }
            return
        }
        update { it.copy(isSaving = true, generalError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = initialConfig ?: ProfileItem.create(EConfigType.POLICYGROUP)
                val subscription = state.subscriptions.getOrNull(state.subscriptionIndex)
                config.remarks = state.remarks.trim()
                config.policyGroupFilter = state.filter.trim()
                config.policyGroupType = state.typeIndex.toString()
                config.policyGroupSubscriptionId = subscription?.id
                config.policyGroupTestOutbounds = state.testOutbounds
                config.policyGroupFallbackTag =
                    state.fallbacks.getOrNull(state.fallbackIndex)?.takeIf { it.isNotEmpty() }
                config.description = buildString {
                    append(state.typeLabels.getOrNull(state.typeIndex).orEmpty())
                    append(" - ")
                    append(subscription?.label.orEmpty())
                    append(" - ")
                    append(config.policyGroupFilter)
                }
                val savedGuid = dataSource.save(state.guid, config)
                _uiState.value = _uiState.value.copy(
                    guid = savedGuid,
                    isSaving = false,
                    savedGuid = savedGuid,
                )
            } catch (e: Exception) {
                LogUtil.e(AppConfig.TAG, "Failed to save policy group", e)
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
                LogUtil.e(AppConfig.TAG, "Failed to delete policy group", e)
                _uiState.value = _uiState.value.copy(generalError = R.string.toast_failure)
            }
        }
    }
}
