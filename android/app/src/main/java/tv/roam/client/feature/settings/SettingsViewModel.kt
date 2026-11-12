package tv.roam.client.feature.settings
import android.app.Application
import android.content.Intent
import android.provider.Settings
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.enums.Language
import tv.roam.client.handler.AppLocaleManager
import tv.roam.client.handler.MmkvManager
import tv.roam.client.root.RootManager
import tv.roam.client.mvvm.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : BaseViewModel(application) {

    private val _systemVpnSettingsAvailable = MutableStateFlow(false)
    val systemVpnSettingsAvailable = _systemVpnSettingsAvailable.asStateFlow()

    private val _language = MutableStateFlow(currentLanguageCode())
    val language = _language.asStateFlow()

    /**
     * Applies an app-level locale. AppCompat owns the recreation path, so the
     * activity is recreated with the new configuration and the route stack is
     * restored by TvNavigator's Saver.
     */
    fun setLanguage(code: String) {
        val normalized = Language.fromCode(code).code
        if (normalized == _language.value) return
        AppLocaleManager.setApplicationLanguage(normalized)
        _language.value = normalized
    }

    private fun currentLanguageCode(): String = runCatching {
        Language.fromCode(
            MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.AUTO.code
        ).code
    }.getOrDefault(Language.AUTO.code)

    suspend fun refreshSystemVpnSettingsAvailability() {
        _systemVpnSettingsAvailable.value = withContext(Dispatchers.IO) {
            // Android exposes the VPN page, not a direct link to the Always-on VPN switch.
            Intent(Settings.ACTION_VPN_SETTINGS).resolveActivity(getApplication<Application>().packageManager) != null
        }
    }

    /**
     * Checks for root access and requests it if necessary.
     * Updates [isLoading] during the process.
     */
    fun checkAndRequestRoot(onSuccess: () -> Unit) {
        launchLoading {
            val hasRoot = withContext(Dispatchers.IO) {
                RootManager.refresh()
            }
            if (hasRoot) {
                onSuccess()
            } else {
                toastError(R.string.toast_root_required)
            }
        }
    }

    /**
     * Validates if the given string is a valid observatory duration.
     * Shows error toast if invalid.
     * @return The trimmed value if valid, null otherwise.
     */
    fun validateObservatoryDuration(value: String): String? {
        val duration = value.trim()
        return if (AppConfig.OBSERVATORY_DURATION_PATTERN.matches(duration)) {
            duration
        } else {
            toastError(R.string.toast_invalid_observatory_duration)
            null
        }
    }

    /**
     * Validates if the given string is a valid observatory sampling value.
     * Shows error toast if invalid.
     * @return The value if valid, null otherwise.
     */
    fun validateObservatorySampling(value: String): String? {
        val sampling = value.trim().toIntOrNull()?.takeIf { it > 0 }
        return if (sampling != null) {
            sampling.toString()
        } else {
            toastError(R.string.toast_invalid_observatory_sampling)
            null
        }
    }
}
