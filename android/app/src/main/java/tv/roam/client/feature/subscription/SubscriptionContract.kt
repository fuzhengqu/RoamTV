package tv.roam.client.feature.subscription

import androidx.annotation.StringRes
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.SubscriptionItem
import java.net.URI

/** Pure rules shared by the subscription editor and JVM tests. */
object SubscriptionRules {
    fun validateRemarks(remarks: String): Boolean = remarks.isNotBlank()

    fun validateUrl(url: String, allowInsecure: Boolean): Boolean {
        if (url.isBlank()) return true
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host ?: return false
        if (host.isBlank()) return false
        if (scheme == "https") return true
        if (scheme != "http") return false
        if (allowInsecure) return true
        return isPrivateHost(host)
    }

    fun validateInterval(autoUpdate: Boolean, minutes: Long): Boolean =
        !autoUpdate || minutes >= AppConfig.SUBSCRIPTION_MIN_INTERVAL_MINUTES

    private fun isPrivateHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") return true
        val parts = host.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false
        return octets[0] == 10 ||
            (octets[0] == 172 && octets[1] in 16..31) ||
            (octets[0] == 192 && octets[1] == 168)
    }
}

enum class SubscriptionField(
    @StringRes val labelRes: Int,
    val kind: SubscriptionFieldKind = SubscriptionFieldKind.Text,
) {
    Remarks(R.string.sub_setting_remarks),
    Url(R.string.sub_setting_url),
    UserAgent(R.string.sub_setting_user_agent),
    Headers(R.string.sub_setting_request_headers, SubscriptionFieldKind.Multiline),
    Filter(R.string.sub_setting_filter),
    Enabled(R.string.sub_setting_enable, SubscriptionFieldKind.Switch),
    AutoUpdate(R.string.sub_auto_update, SubscriptionFieldKind.Switch),
    Interval(R.string.title_pref_auto_update_interval, SubscriptionFieldKind.Number),
    AllowInsecure(R.string.sub_allow_insecure_url, SubscriptionFieldKind.Switch),
    PrevProfile(R.string.sub_setting_pre_profile, SubscriptionFieldKind.Dropdown),
    NextProfile(R.string.sub_setting_next_profile, SubscriptionFieldKind.Dropdown),
}

enum class SubscriptionFieldKind { Text, Multiline, Number, Switch, Dropdown }

data class SubscriptionEditorState(
    val guid: String = "",
    val values: Map<SubscriptionField, String> = emptyMap(),
    val switches: Map<SubscriptionField, Boolean> = emptyMap(),
    val errors: Map<SubscriptionField, Int> = emptyMap(),
    val profiles: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val validationAttempt: Int = 0,
    val savedGuid: String? = null,
    val deleted: Boolean = false,
    @StringRes val generalError: Int? = null,
) {
    fun value(field: SubscriptionField): String = values[field].orEmpty()

    fun isOn(field: SubscriptionField): Boolean = switches[field] == true

    val isNew: Boolean get() = guid.isBlank()
}

sealed interface SubscriptionEditorAction {
    data class Load(val guid: String) : SubscriptionEditorAction
    data class SetValue(val field: SubscriptionField, val value: String) : SubscriptionEditorAction
    data class SetSwitch(val field: SubscriptionField, val value: Boolean) : SubscriptionEditorAction
    data object Save : SubscriptionEditorAction
    data object Delete : SubscriptionEditorAction
    data object ConsumeSaved : SubscriptionEditorAction
    data object ConsumeDeleted : SubscriptionEditorAction
    data object ConsumeGeneralError : SubscriptionEditorAction
}

fun SubscriptionItem.toEditorValues(): Map<SubscriptionField, String> = linkedMapOf(
    SubscriptionField.Remarks to remarks,
    SubscriptionField.Url to url,
    SubscriptionField.UserAgent to userAgent.orEmpty(),
    SubscriptionField.Headers to requestHeaders.orEmpty(),
    SubscriptionField.Filter to filter.orEmpty(),
    SubscriptionField.Interval to updateInterval.toString(),
    SubscriptionField.PrevProfile to prevProfile.orEmpty(),
    SubscriptionField.NextProfile to nextProfile.orEmpty(),
)
