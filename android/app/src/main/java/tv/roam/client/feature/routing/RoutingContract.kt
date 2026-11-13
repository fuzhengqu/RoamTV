package tv.roam.client.feature.routing

import androidx.annotation.StringRes
import tv.roam.client.R
import tv.roam.client.dto.entities.RulesetItem
import java.util.UUID

enum class RoutingField(
    @StringRes val labelRes: Int,
    val kind: RoutingFieldKind = RoutingFieldKind.Text,
) {
    Remarks(R.string.sub_setting_remarks),
    Enabled(R.string.sub_setting_enable, RoutingFieldKind.Switch),
    Locked(R.string.routing_settings_locked, RoutingFieldKind.Switch),
    Domain(R.string.routing_settings_domain),
    Ip(R.string.routing_settings_ip),
    Process(R.string.routing_settings_process),
    Port(R.string.routing_settings_port),
    Protocol(R.string.routing_settings_protocol),
    Network(R.string.routing_settings_network, RoutingFieldKind.Dropdown),
    Outbound(R.string.routing_settings_outbound_tag, RoutingFieldKind.Dropdown),
}

enum class RoutingFieldKind { Text, Dropdown, Switch }

data class RoutingEditorState(
    val ruleId: String = "",
    val values: Map<RoutingField, String> = emptyMap(),
    val switches: Map<RoutingField, Boolean> = emptyMap(),
    val errors: Map<RoutingField, Int> = emptyMap(),
    val outboundOptions: List<String> = listOf("proxy", "direct", "block"),
    val validationAttempt: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    @StringRes val generalError: Int? = null,
) {
    fun value(field: RoutingField): String = values[field].orEmpty()

    fun isOn(field: RoutingField): Boolean = switches[field] == true

    val isNew: Boolean get() = ruleId.isBlank()
}

sealed interface RoutingEditorAction {
    data class Load(val ruleId: String) : RoutingEditorAction
    data class SetValue(val field: RoutingField, val value: String) : RoutingEditorAction
    data class SetSwitch(val field: RoutingField, val value: Boolean) : RoutingEditorAction
    data object Save : RoutingEditorAction
    data object Delete : RoutingEditorAction
    data object ConsumeSaved : RoutingEditorAction
    data object ConsumeDeleted : RoutingEditorAction
    data object ConsumeGeneralError : RoutingEditorAction
}

/** Pure routing-rule mapping and validation. */
object RoutingRules {
    const val DEFAULT_NETWORK = "tcp,udp"
    const val DEFAULT_OUTBOUND = "proxy"

    fun splitList(value: String): List<String>? =
        value.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .takeIf { it.isNotEmpty() }

    fun ensureIds(rules: MutableList<RulesetItem>): Boolean {
        var changed = false
        rules.forEach { rule ->
            if (rule.id.isBlank()) {
                rule.id = UUID.randomUUID().toString()
                changed = true
            }
        }
        return changed
    }

    fun fromRuleset(rule: RulesetItem): RoutingEditorState {
        val values = linkedMapOf(
            RoutingField.Remarks to (rule.remarks ?: ""),
            RoutingField.Domain to rule.domain.orEmpty().joinToString(","),
            RoutingField.Ip to rule.ip.orEmpty().joinToString(","),
            RoutingField.Process to rule.process.orEmpty().joinToString(","),
            RoutingField.Port to (rule.port ?: ""),
            RoutingField.Protocol to rule.protocol.orEmpty().joinToString(","),
            RoutingField.Network to (rule.network ?: DEFAULT_NETWORK),
            RoutingField.Outbound to rule.outboundTag.ifBlank { DEFAULT_OUTBOUND },
        )
        return RoutingEditorState(
            ruleId = rule.id,
            values = values,
            switches = mapOf(
                RoutingField.Enabled to rule.enabled,
                RoutingField.Locked to (rule.locked == true),
            ),
            isLoading = false,
        )
    }

    fun newState(): RoutingEditorState = RoutingEditorState(
        values = mapOf(
            RoutingField.Remarks to "",
            RoutingField.Domain to "",
            RoutingField.Ip to "",
            RoutingField.Process to "",
            RoutingField.Port to "",
            RoutingField.Protocol to "",
            RoutingField.Network to DEFAULT_NETWORK,
            RoutingField.Outbound to DEFAULT_OUTBOUND,
        ),
        switches = mapOf(
            RoutingField.Enabled to true,
            RoutingField.Locked to false,
        ),
        isLoading = false,
    )

    fun validate(state: RoutingEditorState): Map<RoutingField, Int> {
        val errors = linkedMapOf<RoutingField, Int>()
        if (state.value(RoutingField.Remarks).isBlank()) {
            errors[RoutingField.Remarks] = R.string.tv_editor_error_required
        }
        return errors
    }

    fun toRuleset(state: RoutingEditorState, existing: RulesetItem? = null): RulesetItem {
        val rule = existing?.copy() ?: RulesetItem()
        rule.id = existing?.id?.ifBlank { UUID.randomUUID().toString() }
            ?: UUID.randomUUID().toString()
        rule.remarks = state.value(RoutingField.Remarks).trim()
        rule.enabled = state.isOn(RoutingField.Enabled)
        rule.locked = state.isOn(RoutingField.Locked)
        rule.domain = splitList(state.value(RoutingField.Domain))
        rule.ip = splitList(state.value(RoutingField.Ip))
        rule.process = splitList(state.value(RoutingField.Process))
        rule.protocol = splitList(state.value(RoutingField.Protocol))
        rule.port = state.value(RoutingField.Port).trim().ifBlank { null }
        rule.network = state.value(RoutingField.Network).trim().ifBlank { null }
        rule.outboundTag = state.value(RoutingField.Outbound).trim()
            .ifBlank { DEFAULT_OUTBOUND }
        return rule
    }
}
