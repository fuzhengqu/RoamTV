package tv.roam.client.feature.settings

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.enums.Language
import tv.roam.client.handler.AppLocaleManager
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.SettingsChangeManager
import tv.roam.client.mvvm.BaseViewModel
import tv.roam.client.root.RootManager

internal enum class SettingSwitch(val key: String, val default: Boolean) {
    SpeedEnabled(AppConfig.PREF_SPEED_ENABLED, false),
    ConfirmRemove(AppConfig.PREF_CONFIRM_REMOVE, false),
    GroupAllDisplay(AppConfig.PREF_GROUP_ALL_DISPLAY, false),
    Ipv6Enabled(AppConfig.PREF_IPV6_ENABLED, false),
    PreferIpv6(AppConfig.PREF_PREFER_IPV6, false),
    LocalDns(AppConfig.PREF_LOCAL_DNS_ENABLED, false),
    FakeDns(AppConfig.PREF_FAKE_DNS_ENABLED, false),
    AppendHttpProxy(AppConfig.PREF_APPEND_HTTP_PROXY, false),
    UseHevTunnel(AppConfig.PREF_USE_HEV_TUNNEL, true),
    Sniffing(AppConfig.PREF_SNIFFING_ENABLED, true),
    RouteOnly(AppConfig.PREF_ROUTE_ONLY_ENABLED, false),
    EnableLocalProxy(AppConfig.PREF_ENABLE_LOCAL_PROXY, true),
    ProxySharing(AppConfig.PREF_PROXY_SHARING, false),
    DynamicSocksPort(AppConfig.PREF_DYNAMIC_SOCKS_PORT, false),
    SocksUdp(AppConfig.PREF_SOCKS_ENABLE_UDP, AppConfig.DEFAULT_SOCKS_ENABLE_UDP),
    MuxEnabled(AppConfig.PREF_MUX_ENABLED, false),
    FragmentEnabled(AppConfig.PREF_FRAGMENT_ENABLED, false),
    StartOnBoot(AppConfig.PREF_IS_BOOTED, false),
    RootMode(AppConfig.PREF_ROOT_MODE_ENABLE, false),
    RootLanSharing(AppConfig.PREF_ROOT_LAN_SHARING, false),
}

internal enum class SettingField(
    val key: String,
    val default: String,
    val numberOnly: Boolean = false,
    val password: Boolean = false,
    val multiline: Boolean = false,
    val duration: Boolean = false,
    val sampling: Boolean = false,
    val fromPhone: Boolean = false,
) {
    VpnDns(AppConfig.PREF_VPN_DNS, AppConfig.DNS_VPN, fromPhone = true),
    VpnMtu(AppConfig.PREF_VPN_MTU, AppConfig.VPN_MTU.toString(), numberOnly = true),
    HevRwTimeout(AppConfig.PREF_HEV_TUNNEL_RW_TIMEOUT, AppConfig.HEVTUN_RW_TIMEOUT, numberOnly = true),
    SocksPort(AppConfig.PREF_SOCKS_PORT, AppConfig.PORT_SOCKS, numberOnly = true),
    SocksUsername(AppConfig.PREF_SOCKS_USERNAME, ""),
    SocksPassword(AppConfig.PREF_SOCKS_PASSWORD, "", password = true),
    RemoteDns(AppConfig.PREF_REMOTE_DNS, AppConfig.DNS_PROXY, fromPhone = true),
    DomesticDns(AppConfig.PREF_DOMESTIC_DNS, AppConfig.DNS_DIRECT, fromPhone = true),
    DnsHosts(AppConfig.PREF_DNS_HOSTS, "", multiline = true, fromPhone = true),
    MuxConcurrency(AppConfig.PREF_MUX_CONCURRENCY, "8", numberOnly = true),
    MuxXudpConcurrency(AppConfig.PREF_MUX_XUDP_CONCURRENCY, AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY, numberOnly = true),
    FragmentLength(AppConfig.PREF_FRAGMENT_LENGTH, "50-100"),
    FragmentInterval(AppConfig.PREF_FRAGMENT_INTERVAL, "10-20"),
    FragmentMaxSplit(AppConfig.PREF_FRAGMENT_MAXSPLIT, "10", numberOnly = true),
    ObsPingInterval(AppConfig.PREF_OBSERVATORY_LEAST_PING_INTERVAL, AppConfig.OBSERVATORY_LEAST_PING_INTERVAL, duration = true),
    ObsLoadInterval(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_INTERVAL, AppConfig.OBSERVATORY_LEAST_LOAD_INTERVAL, duration = true),
    ObsLoadSampling(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_SAMPLING, AppConfig.OBSERVATORY_LEAST_LOAD_SAMPLING, numberOnly = true, sampling = true),
    ObsLoadTimeout(AppConfig.PREF_OBSERVATORY_LEAST_LOAD_TIMEOUT, AppConfig.OBSERVATORY_LEAST_LOAD_TIMEOUT, duration = true),
    DelayTestUrl(AppConfig.PREF_DELAY_TEST_URL, AppConfig.DELAY_TEST_URL, fromPhone = true),
    RealPingConcurrency(AppConfig.PREF_REAL_PING_CONCURRENCY, "16", numberOnly = true),
    IpApiUrl(AppConfig.PREF_IP_API_URL, AppConfig.IP_API_URL, fromPhone = true),
}

internal enum class SettingDropdown(
    val key: String,
    val default: String,
    @ArrayRes val entriesRes: Int,
    @ArrayRes val valuesRes: Int,
) {
    VpnBypassLan(
        AppConfig.PREF_VPN_BYPASS_LAN,
        AppConfig.DEFAULT_VPN_BYPASS_LAN,
        R.array.vpn_bypass_lan,
        R.array.vpn_bypass_lan_value,
    ),
    VpnInterfaceAddress(
        AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX,
        "0",
        R.array.vpn_interface_address,
        R.array.vpn_interface_address_value,
    ),
    HevLogLevel(
        AppConfig.PREF_HEV_TUNNEL_LOGLEVEL,
        AppConfig.DEFAULT_HEV_TUNNEL_LOGLEVEL,
        R.array.hev_tunnel_loglevel,
        R.array.hev_tunnel_loglevel,
    ),
    CoreLogLevel(
        AppConfig.PREF_LOGLEVEL,
        "warning",
        R.array.core_loglevel,
        R.array.core_loglevel,
    ),
    OutboundResolveMethod(
        AppConfig.PREF_OUTBOUND_DOMAIN_RESOLVE_METHOD,
        AppConfig.DEFAULT_OUTBOUND_DOMAIN_RESOLVE_METHOD,
        R.array.outbound_domain_resolve_method,
        R.array.outbound_domain_resolve_method_value,
    ),
    MuxXudpQuic(
        AppConfig.PREF_MUX_XUDP_QUIC,
        "reject",
        R.array.mux_xudp_quic_entries,
        R.array.mux_xudp_quic_value,
    ),
    FragmentPackets(
        AppConfig.PREF_FRAGMENT_PACKETS,
        "tlshello",
        R.array.fragment_packets,
        R.array.fragment_packets,
    ),
    ObsLoadMethod(
        AppConfig.PREF_OBSERVATORY_LEAST_LOAD_METHOD,
        AppConfig.OBSERVATORY_LEAST_LOAD_METHOD,
        R.array.observatory_least_load_method,
        R.array.observatory_least_load_method,
    ),
    Mode(
        AppConfig.PREF_MODE,
        AppConfig.VPN,
        R.array.mode_entries,
        R.array.mode_value,
    ),
}

internal sealed interface SettingsAction {
    data object Load : SettingsAction
    data class SetSwitch(val setting: SettingSwitch, val value: Boolean) : SettingsAction
    data class SetField(val setting: SettingField, val value: String) : SettingsAction
    data class SetDropdown(val setting: SettingDropdown, val value: String) : SettingsAction
    data object ConsumeMessage : SettingsAction
}

internal data class SettingsUiState(
    val switches: Map<SettingSwitch, Boolean> = emptyMap(),
    val fields: Map<SettingField, String> = emptyMap(),
    val dropdowns: Map<SettingDropdown, String> = emptyMap(),
    val isLoading: Boolean = true,
    val systemVpnSettingsAvailable: Boolean = false,
    @StringRes val messageRes: Int? = null,
) {
    fun isOn(setting: SettingSwitch): Boolean = switches[setting] ?: setting.default

    fun fieldValue(setting: SettingField): String = fields[setting] ?: setting.default

    fun dropdownValue(setting: SettingDropdown): String = dropdowns[setting] ?: setting.default
}

internal val SettingsUiState.isVpn: Boolean
    get() = dropdownValue(SettingDropdown.Mode) == AppConfig.VPN

/** HEV TUN requires the local proxy, matching the legacy settings contract. */
internal val SettingsUiState.hevTunnelEnabled: Boolean
    get() = isVpn && isOn(SettingSwitch.UseHevTunnel)

internal val SettingsUiState.effectiveLocalProxy: Boolean
    get() = isOn(SettingSwitch.EnableLocalProxy) || hevTunnelEnabled

internal val SettingsUiState.muxXudpConcurrency: Int
    get() = fieldValue(SettingField.MuxXudpConcurrency).toIntOrNull()
        ?: AppConfig.DEFAULT_MUX_XUDP_CONCURRENCY.toInt()

internal fun isSystemVpnSettingsAvailable(resolve: (Intent) -> Boolean): Boolean =
    resolve(Intent(Settings.ACTION_VPN_SETTINGS))

class SettingsViewModel(application: Application) : BaseViewModel(application) {

    private val _language = MutableStateFlow(currentLanguageCode())
    val language = _language.asStateFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    internal val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        onAction(SettingsAction.Load)
    }

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

    internal fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.Load -> load()
            is SettingsAction.SetSwitch -> setSwitch(action.setting, action.value)
            is SettingsAction.SetField -> setField(action.setting, action.value)
            is SettingsAction.SetDropdown -> setDropdown(action.setting, action.value)
            SettingsAction.ConsumeMessage -> _uiState.value = _uiState.value.copy(messageRes = null)
        }
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val switches = SettingSwitch.entries.associateWith {
                MmkvManager.decodeSettingsBool(it.key, it.default)
            }
            val fields = SettingField.entries.associateWith {
                MmkvManager.decodeSettingsString(it.key, it.default) ?: it.default
            }
            val dropdowns = SettingDropdown.entries.associateWith {
                MmkvManager.decodeSettingsString(it.key, it.default) ?: it.default
            }
            val vpnSettingsAvailable = runCatching {
                isSystemVpnSettingsAvailable { intent ->
                    intent.resolveActivity(app.packageManager) != null
                }
            }.getOrDefault(false)
            _uiState.value = SettingsUiState(
                switches = switches,
                fields = fields,
                dropdowns = dropdowns,
                isLoading = false,
                systemVpnSettingsAvailable = vpnSettingsAvailable,
                messageRes = _uiState.value.messageRes,
            )
        }
    }

    private fun setSwitch(setting: SettingSwitch, value: Boolean) {
        if ((setting == SettingSwitch.RootMode || setting == SettingSwitch.RootLanSharing) &&
            value &&
            !RootManager.cachedRoot()
        ) {
            viewModelScope.launch {
                val granted = withContext(Dispatchers.IO) { RootManager.refresh() }
                if (granted) {
                    persistSwitch(setting, true)
                } else {
                    _uiState.value = _uiState.value.copy(messageRes = R.string.toast_root_required)
                }
            }
            return
        }
        persistSwitch(setting, value)
        if (setting == SettingSwitch.UseHevTunnel && value &&
            !_uiState.value.isOn(SettingSwitch.EnableLocalProxy)
        ) {
            persistSwitch(SettingSwitch.EnableLocalProxy, true)
        }
        if (setting == SettingSwitch.EnableLocalProxy && !value &&
            _uiState.value.isOn(SettingSwitch.AppendHttpProxy)
        ) {
            persistSwitch(SettingSwitch.AppendHttpProxy, false)
        }
    }

    private fun setField(setting: SettingField, value: String) {
        val normalized = if (setting.password) value else value.trim()
        if (setting.duration && !AppConfig.OBSERVATORY_DURATION_PATTERN.matches(normalized)) {
            _uiState.value = _uiState.value.copy(messageRes = R.string.toast_invalid_observatory_duration)
            return
        }
        if (setting.sampling && normalized.toIntOrNull()?.takeIf { it > 0 } == null) {
            _uiState.value = _uiState.value.copy(messageRes = R.string.toast_invalid_observatory_sampling)
            return
        }
        _uiState.value = _uiState.value.copy(
            fields = _uiState.value.fields + (setting to normalized),
        )
        viewModelScope.launch(Dispatchers.IO) {
            MmkvManager.encodeSettings(setting.key, normalized)
            SettingsChangeManager.notifySettingChanged(setting.key)
        }
    }

    private fun setDropdown(setting: SettingDropdown, value: String) {
        _uiState.value = _uiState.value.copy(
            dropdowns = _uiState.value.dropdowns + (setting to value),
        )
        viewModelScope.launch(Dispatchers.IO) {
            MmkvManager.encodeSettings(setting.key, value)
            SettingsChangeManager.notifySettingChanged(setting.key)
        }
    }

    private fun persistSwitch(setting: SettingSwitch, value: Boolean) {
        _uiState.value = _uiState.value.copy(
            switches = _uiState.value.switches + (setting to value),
        )
        viewModelScope.launch(Dispatchers.IO) {
            MmkvManager.encodeSettings(setting.key, value)
            SettingsChangeManager.notifySettingChanged(setting.key)
        }
    }

    private fun currentLanguageCode(): String = runCatching {
        Language.fromCode(
            MmkvManager.decodeSettingsString(AppConfig.PREF_LANGUAGE) ?: Language.AUTO.code
        ).code
    }.getOrDefault(Language.AUTO.code)
}
