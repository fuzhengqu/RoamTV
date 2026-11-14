package tv.roam.client.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.extension.toastError
import tv.roam.client.feature.pairing.PairingViewModel
import tv.roam.client.feature.settings.SettingDropdown
import tv.roam.client.feature.settings.SettingField
import tv.roam.client.feature.settings.SettingSwitch
import tv.roam.client.feature.settings.SettingsAction
import tv.roam.client.feature.settings.SettingsUiState
import tv.roam.client.feature.settings.SettingsViewModel
import tv.roam.client.feature.settings.effectiveLocalProxy
import tv.roam.client.feature.settings.hevTunnelEnabled
import tv.roam.client.feature.settings.isVpn
import tv.roam.client.feature.settings.muxXudpConcurrency
import tv.roam.client.ui.components.PairingDialog
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvOptionsDialog
import tv.roam.client.ui.components.TvRailItem
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTextEditDialog
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    pairingViewModel: PairingViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val pairingState by pairingViewModel.uiState.collectAsStateWithLifecycle()
    val languageEntries = stringArrayResource(R.array.language_select).toList()
    val languageValues = stringArrayResource(R.array.language_select_value).toList()
    val languageLabel = languageLabel(currentLanguage, languageValues, languageEntries)

    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    var focusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var lastKey by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var activeField by remember { mutableStateOf<SettingField?>(null) }
    var activeDropdown by remember { mutableStateOf<SettingDropdown?>(null) }
    var editSeed by remember { mutableStateOf("") }
    var pairingField by remember { mutableStateOf<SettingField?>(null) }
    var showPairing by remember { mutableStateOf(false) }

    val dropdownOptions = SettingDropdown.entries.associateWith { dropdown ->
        stringArrayResource(dropdown.entriesRes).toList() to
            stringArrayResource(dropdown.valuesRes).toList()
    }

    val localNetworkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pairingViewModel.start()
            showPairing = true
        } else {
            context.toast(R.string.tv_pairing_permission_denied)
        }
    }

    val startPairing: (SettingField) -> Unit = { field ->
        pairingField = field
        if (Build.VERSION.SDK_INT >= 37 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            localNetworkLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            pairingViewModel.start()
            showPairing = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onAction(SettingsAction.Load)
    }
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        val target = focusedKey ?: "speed"
        rowFocus[target]?.requestFocus()
    }
    LaunchedEffect(state.messageRes) {
        val message = state.messageRes ?: return@LaunchedEffect
        context.toast(message)
        viewModel.onAction(SettingsAction.ConsumeMessage)
    }
    LaunchedEffect(activeField, activeDropdown, showLanguageDialog, showPairing) {
        if (activeField == null && activeDropdown == null && !showLanguageDialog && !showPairing) {
            lastKey?.let { rowFocus[it]?.requestFocus() }
        }
    }
    LaunchedEffect(Unit) {
        pairingViewModel.received.collect { text ->
            val field = pairingField
            showPairing = false
            pairingField = null
            if (field != null) {
                editSeed = text
                activeField = field
                lastKey = "field_${field.name}"
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { pairingViewModel.stop() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(title = stringResource(R.string.title_settings))
            if (state.isLoading) {
                Text(
                    text = stringResource(R.string.tv_editor_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(key = "hint") {
                        Text(
                            text = stringResource(R.string.tv_settings_restart_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.TextMuted,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }

                    item(key = "ui_header") { SectionHeader(R.string.title_ui_settings) }
                    switchItem(
                        key = "speed",
                        titleRes = R.string.title_pref_speed_enabled,
                        summaryRes = R.string.summary_pref_speed_enabled,
                        setting = SettingSwitch.SpeedEnabled,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "confirm_remove",
                        titleRes = R.string.title_pref_confirm_remove,
                        summaryRes = R.string.summary_pref_confirm_remove,
                        setting = SettingSwitch.ConfirmRemove,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "group_all",
                        titleRes = R.string.title_pref_group_all_display,
                        summaryRes = R.string.summary_pref_group_all_display,
                        setting = SettingSwitch.GroupAllDisplay,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    item(key = "language") {
                        FocusedSetting("language", rowFocus, { focusedKey = it }) { rowModifier ->
                            TvSettingsRow(
                                label = stringResource(R.string.title_language),
                                value = languageLabel,
                                iconRes = R.drawable.ic_translate_24dp,
                                onClick = {
                                    lastKey = "language"
                                    showLanguageDialog = true
                                },
                                modifier = rowModifier,
                            )
                        }
                    }

                    item(key = "vpn_header") { SectionHeader(R.string.title_vpn_settings) }
                    switchItem(
                        key = "ipv6",
                        titleRes = R.string.title_pref_ipv6_enabled,
                        summaryRes = R.string.summary_pref_ipv6_enabled,
                        setting = SettingSwitch.Ipv6Enabled,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "prefer_ipv6",
                        titleRes = R.string.title_pref_prefer_ipv6,
                        summaryRes = R.string.summary_pref_prefer_ipv6,
                        setting = SettingSwitch.PreferIpv6,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "local_dns",
                        titleRes = R.string.title_pref_local_dns_enabled,
                        summaryRes = R.string.summary_pref_local_dns_enabled,
                        setting = SettingSwitch.LocalDns,
                        state = state,
                        enabled = state.isVpn,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "fake_dns",
                        titleRes = R.string.title_pref_fake_dns_enabled,
                        summaryRes = R.string.summary_pref_fake_dns_enabled,
                        setting = SettingSwitch.FakeDns,
                        state = state,
                        enabled = state.isVpn && state.isOn(SettingSwitch.LocalDns),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    fieldItem(
                        key = "field_vpn_dns",
                        setting = SettingField.VpnDns,
                        state = state,
                        enabled = state.isVpn && !state.isOn(SettingSwitch.LocalDns),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    switchItem(
                        key = "append_http_proxy",
                        titleRes = R.string.title_pref_append_http_proxy,
                        summaryRes = R.string.summary_pref_append_http_proxy,
                        setting = SettingSwitch.AppendHttpProxy,
                        state = state,
                        enabled = state.effectiveLocalProxy,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    dropdownItem(
                        key = "vpn_bypass_lan",
                        setting = SettingDropdown.VpnBypassLan,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.VpnBypassLan),
                        enabled = state.isVpn,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )
                    dropdownItem(
                        key = "vpn_interface",
                        setting = SettingDropdown.VpnInterfaceAddress,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.VpnInterfaceAddress),
                        enabled = state.isVpn,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )
                    fieldItem(
                        key = "field_vpn_mtu",
                        setting = SettingField.VpnMtu,
                        state = state,
                        enabled = state.isVpn,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    switchItem(
                        key = "use_hev",
                        titleRes = R.string.title_pref_use_hev_tunnel,
                        summaryRes = R.string.summary_pref_use_hev_tunnel,
                        setting = SettingSwitch.UseHevTunnel,
                        state = state,
                        enabled = state.isVpn,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    dropdownItem(
                        key = "hev_loglevel",
                        setting = SettingDropdown.HevLogLevel,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.HevLogLevel),
                        enabled = state.hevTunnelEnabled,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )
                    fieldItem(
                        key = "field_hev_rw_timeout",
                        setting = SettingField.HevRwTimeout,
                        state = state,
                        enabled = state.hevTunnelEnabled,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )

                    item(key = "core_header") { SectionHeader(R.string.title_core_settings) }
                    switchItem(
                        key = "sniffing",
                        titleRes = R.string.title_pref_sniffing_enabled,
                        summaryRes = R.string.summary_pref_sniffing_enabled,
                        setting = SettingSwitch.Sniffing,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "route_only",
                        titleRes = R.string.title_pref_route_only_enabled,
                        summaryRes = R.string.summary_pref_route_only_enabled,
                        setting = SettingSwitch.RouteOnly,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "enable_local_proxy",
                        titleRes = R.string.title_pref_enable_local_proxy,
                        summaryRes = R.string.summary_pref_enable_local_proxy,
                        setting = SettingSwitch.EnableLocalProxy,
                        state = state,
                        enabled = !state.hevTunnelEnabled,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "proxy_sharing",
                        titleRes = R.string.title_pref_proxy_sharing_enabled,
                        summaryRes = R.string.summary_pref_proxy_sharing_enabled,
                        setting = SettingSwitch.ProxySharing,
                        state = state,
                        enabled = state.effectiveLocalProxy,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "dynamic_socks",
                        titleRes = R.string.title_pref_dynamic_socks_port,
                        summaryRes = R.string.summary_pref_dynamic_socks_port,
                        setting = SettingSwitch.DynamicSocksPort,
                        state = state,
                        enabled = state.effectiveLocalProxy,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    fieldItem(
                        key = "field_socks_port",
                        setting = SettingField.SocksPort,
                        state = state,
                        enabled = state.effectiveLocalProxy &&
                            !state.isOn(SettingSwitch.DynamicSocksPort),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_socks_user",
                        setting = SettingField.SocksUsername,
                        state = state,
                        enabled = state.effectiveLocalProxy,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_socks_password",
                        setting = SettingField.SocksPassword,
                        state = state,
                        enabled = state.effectiveLocalProxy,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    switchItem(
                        key = "socks_udp",
                        titleRes = R.string.title_pref_socks_enable_udp,
                        summaryRes = R.string.summary_pref_socks_enable_udp,
                        setting = SettingSwitch.SocksUdp,
                        state = state,
                        enabled = state.effectiveLocalProxy,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    fieldItem(
                        key = "field_remote_dns",
                        setting = SettingField.RemoteDns,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_domestic_dns",
                        setting = SettingField.DomesticDns,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_dns_hosts",
                        setting = SettingField.DnsHosts,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    dropdownItem(
                        key = "core_loglevel",
                        setting = SettingDropdown.CoreLogLevel,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.CoreLogLevel),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )
                    dropdownItem(
                        key = "outbound_resolve",
                        setting = SettingDropdown.OutboundResolveMethod,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.OutboundResolveMethod),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )

                    item(key = "mux_header") { SectionHeader(R.string.title_mux_settings) }
                    switchItem(
                        key = "mux_enabled",
                        titleRes = R.string.title_pref_mux_enabled,
                        summaryRes = R.string.summary_pref_mux_enabled,
                        setting = SettingSwitch.MuxEnabled,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    fieldItem(
                        key = "field_mux_concurrency",
                        setting = SettingField.MuxConcurrency,
                        state = state,
                        enabled = state.isOn(SettingSwitch.MuxEnabled),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_mux_xudp_concurrency",
                        setting = SettingField.MuxXudpConcurrency,
                        state = state,
                        enabled = state.isOn(SettingSwitch.MuxEnabled),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    dropdownItem(
                        key = "mux_xudp_quic",
                        setting = SettingDropdown.MuxXudpQuic,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.MuxXudpQuic),
                        enabled = state.isOn(SettingSwitch.MuxEnabled) &&
                            state.muxXudpConcurrency >= 0,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )

                    item(key = "fragment_header") { SectionHeader(R.string.title_fragment_settings) }
                    switchItem(
                        key = "fragment_enabled",
                        titleRes = R.string.title_pref_fragment_enabled,
                        setting = SettingSwitch.FragmentEnabled,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    dropdownItem(
                        key = "fragment_packets",
                        setting = SettingDropdown.FragmentPackets,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.FragmentPackets),
                        enabled = state.isOn(SettingSwitch.FragmentEnabled),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )
                    fieldItem(
                        key = "field_fragment_length",
                        setting = SettingField.FragmentLength,
                        state = state,
                        enabled = state.isOn(SettingSwitch.FragmentEnabled),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_fragment_interval",
                        setting = SettingField.FragmentInterval,
                        state = state,
                        enabled = state.isOn(SettingSwitch.FragmentEnabled),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_fragment_maxsplit",
                        setting = SettingField.FragmentMaxSplit,
                        state = state,
                        enabled = state.isOn(SettingSwitch.FragmentEnabled),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )

                    item(key = "observatory_header") {
                        SectionHeader(R.string.title_observatory_settings)
                    }
                    fieldItem(
                        key = "field_obs_ping",
                        setting = SettingField.ObsPingInterval,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_obs_load_interval",
                        setting = SettingField.ObsLoadInterval,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    dropdownItem(
                        key = "obs_load_method",
                        setting = SettingDropdown.ObsLoadMethod,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.ObsLoadMethod),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )
                    fieldItem(
                        key = "field_obs_sampling",
                        setting = SettingField.ObsLoadSampling,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_obs_timeout",
                        setting = SettingField.ObsLoadTimeout,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )

                    item(key = "advanced_header") { SectionHeader(R.string.title_advanced) }
                    switchItem(
                        key = "start_on_boot",
                        titleRes = R.string.title_pref_is_booted,
                        summaryRes = R.string.summary_pref_is_booted,
                        setting = SettingSwitch.StartOnBoot,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    if (state.systemVpnSettingsAvailable) {
                        item(key = "system_vpn") {
                            FocusedSetting("system_vpn", rowFocus, { focusedKey = it }) { rowModifier ->
                                TvSettingsRow(
                                    label = stringResource(R.string.title_system_vpn_settings),
                                    value = "",
                                    iconRes = R.drawable.ic_settings_24dp,
                                    onClick = {
                                        lastKey = "system_vpn"
                                        openSystemVpnSettings(context)
                                    },
                                    modifier = rowModifier,
                                )
                            }
                        }
                        item(key = "system_vpn_summary") {
                            Text(
                                text = stringResource(R.string.summary_system_vpn_settings),
                                style = MaterialTheme.typography.bodySmall,
                                color = TvColors.TextMuted,
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                            )
                        }
                    }
                    fieldItem(
                        key = "field_delay_url",
                        setting = SettingField.DelayTestUrl,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_real_ping_concurrency",
                        setting = SettingField.RealPingConcurrency,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )
                    fieldItem(
                        key = "field_ip_api_url",
                        setting = SettingField.IpApiUrl,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onEdit = { field, seed ->
                            lastKey = "field_${field.name}"
                            editSeed = seed
                            activeField = field
                        },
                    )

                    item(key = "mode_header") { SectionHeader(R.string.title_mode_settings) }
                    dropdownItem(
                        key = "mode",
                        setting = SettingDropdown.Mode,
                        state = state,
                        options = dropdownOptions.getValue(SettingDropdown.Mode),
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onOpen = {
                            lastKey = "dropdown_${it.name}"
                            activeDropdown = it
                        },
                    )
                    switchItem(
                        key = "root_mode",
                        titleRes = R.string.title_root_mode_enabled,
                        summaryRes = R.string.summary_root_mode_enabled,
                        setting = SettingSwitch.RootMode,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )
                    switchItem(
                        key = "root_lan",
                        titleRes = R.string.title_root_lan_sharing,
                        summaryRes = R.string.summary_root_lan_sharing,
                        setting = SettingSwitch.RootLanSharing,
                        state = state,
                        rowFocus = rowFocus,
                        onFocused = { focusedKey = it },
                        onToggle = { setting, value ->
                            lastKey = "switch_${setting.name}"
                            viewModel.onAction(SettingsAction.SetSwitch(setting, value))
                        },
                    )

                    item(key = "bottom_space") { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        TvKeyHintBar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            hints = listOf(
                stringResource(R.string.tv_key_ok) to stringResource(R.string.tv_key_ok_action),
                stringResource(R.string.tv_key_back) to stringResource(R.string.tv_key_back_action),
            ),
        )
    }

    activeField?.let { field ->
        TvTextEditDialog(
            title = stringResource(fieldTitle(field)),
            initialValue = editSeed,
            multiline = field.multiline,
            numberOnly = field.numberOnly,
            password = field.password,
            onSave = { value ->
                viewModel.onAction(SettingsAction.SetField(field, value))
                activeField = null
            },
            onFromPhone = if (field.fromPhone) {
                { startPairing(field) }
            } else {
                null
            },
            onDismiss = { activeField = null },
        )
    }

    activeDropdown?.let { dropdown ->
        val (entries, values) = dropdownOptions.getValue(dropdown)
        TvOptionsDialog(
            title = stringResource(dropdownTitle(dropdown)),
            options = entries,
            selectedIndex = values.indexOf(state.dropdownValue(dropdown)),
            onSelectIndex = { index ->
                values.getOrNull(index)?.let { value ->
                    viewModel.onAction(SettingsAction.SetDropdown(dropdown, value))
                }
                activeDropdown = null
            },
            onDismiss = { activeDropdown = null },
        )
    }

    if (showPairing) {
        PairingDialog(
            state = pairingState,
            onCancel = {
                pairingViewModel.stop()
                showPairing = false
                pairingField = null
            },
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = currentLanguage,
            languageEntries = languageEntries,
            languageValues = languageValues,
            onSelect = { code ->
                showLanguageDialog = false
                viewModel.setLanguage(code)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

private fun LazyListScope.switchItem(
    key: String,
    @StringRes titleRes: Int,
    @StringRes summaryRes: Int? = null,
    setting: SettingSwitch,
    state: SettingsUiState,
    enabled: Boolean = true,
    rowFocus: MutableMap<String, FocusRequester>,
    onFocused: (String) -> Unit,
    onToggle: (SettingSwitch, Boolean) -> Unit,
) {
    item(key = key) {
        FocusedSetting(key, rowFocus, onFocused) { rowModifier ->
            SwitchRow(
                label = stringResource(titleRes),
                summary = summaryRes?.let { stringResource(it) },
                checked = state.isOn(setting),
                enabled = enabled,
                onClick = { onToggle(setting, !state.isOn(setting)) },
                modifier = rowModifier,
            )
        }
    }
}

private fun LazyListScope.fieldItem(
    key: String,
    setting: SettingField,
    state: SettingsUiState,
    enabled: Boolean = true,
    rowFocus: MutableMap<String, FocusRequester>,
    onFocused: (String) -> Unit,
    onEdit: (SettingField, String) -> Unit,
) {
    item(key = key) {
        FocusedSetting(key, rowFocus, onFocused) { rowModifier ->
            FieldRow(
                label = stringResource(fieldTitle(setting)),
                value = state.fieldValue(setting),
                masked = setting.password,
                enabled = enabled,
                onClick = { onEdit(setting, state.fieldValue(setting)) },
                modifier = rowModifier,
            )
        }
    }
}

private fun LazyListScope.dropdownItem(
    key: String,
    setting: SettingDropdown,
    state: SettingsUiState,
    options: Pair<List<String>, List<String>>,
    enabled: Boolean = true,
    rowFocus: MutableMap<String, FocusRequester>,
    onFocused: (String) -> Unit,
    onOpen: (SettingDropdown) -> Unit,
) {
    val (entries, values) = options
    val index = values.indexOf(state.dropdownValue(setting))
    val display = entries.getOrElse(index) { state.dropdownValue(setting) }
    item(key = key) {
        FocusedSetting(key, rowFocus, onFocused) { rowModifier ->
            DropdownRow(
                label = stringResource(dropdownTitle(setting)),
                value = display,
                enabled = enabled,
                onClick = { onOpen(setting) },
                modifier = rowModifier,
            )
        }
    }
}

@Composable
private fun FocusedSetting(
    key: String,
    focusMap: MutableMap<String, FocusRequester>,
    onFocused: (String) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val requester = remember(key) { FocusRequester() }
    SideEffect { focusMap[key] = requester }
    content(
        Modifier
            .focusRequester(requester)
            .onFocusChanged { focus ->
                if (focus.isFocused) onFocused(key)
            }
    )
}

@Composable
private fun SectionHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.labelLarge,
        color = TvColors.TextMuted,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(
    label: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TvSettingsRow(
            label = label,
            value = stringResource(if (checked) R.string.tv_editor_on else R.string.tv_editor_off),
            iconRes = R.drawable.ic_qu_switch_24dp,
            onClick = { if (enabled) onClick() },
            modifier = if (enabled) Modifier else Modifier.alpha(0.45f),
        )
        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextMuted,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    masked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = when {
        masked && value.isNotEmpty() -> stringResource(R.string.tv_password_masked)
        value.isEmpty() -> stringResource(R.string.tv_editor_not_set)
        else -> value
    }
    TvSettingsRow(
        label = label,
        value = display,
        iconRes = if (masked) R.drawable.ic_lock_24dp else R.drawable.ic_edit_24dp,
        onClick = { if (enabled) onClick() },
        modifier = modifier.then(if (enabled) Modifier else Modifier.alpha(0.45f)),
    )
}

@Composable
private fun DropdownRow(
    label: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvSettingsRow(
        label = label,
        value = value.ifEmpty { stringResource(R.string.tv_editor_not_set) },
        iconRes = R.drawable.ic_expand_more_24dp,
        onClick = { if (enabled) onClick() },
        modifier = modifier.then(if (enabled) Modifier else Modifier.alpha(0.45f)),
    )
}

@StringRes
private fun fieldTitle(field: SettingField): Int = when (field) {
    SettingField.VpnDns -> R.string.title_pref_vpn_dns
    SettingField.VpnMtu -> R.string.title_pref_vpn_mtu
    SettingField.HevRwTimeout -> R.string.title_pref_hev_tunnel_rw_timeout
    SettingField.SocksPort -> R.string.title_pref_socks_port
    SettingField.SocksUsername -> R.string.title_pref_socks_username
    SettingField.SocksPassword -> R.string.title_pref_socks_password
    SettingField.RemoteDns -> R.string.title_pref_remote_dns
    SettingField.DomesticDns -> R.string.title_pref_domestic_dns
    SettingField.DnsHosts -> R.string.title_pref_dns_hosts
    SettingField.MuxConcurrency -> R.string.title_pref_mux_concurrency
    SettingField.MuxXudpConcurrency -> R.string.title_pref_mux_xudp_concurrency
    SettingField.FragmentLength -> R.string.title_pref_fragment_length
    SettingField.FragmentInterval -> R.string.title_pref_fragment_interval
    SettingField.FragmentMaxSplit -> R.string.title_pref_fragment_maxsplit
    SettingField.ObsPingInterval -> R.string.title_pref_observatory_least_ping_interval
    SettingField.ObsLoadInterval -> R.string.title_pref_observatory_least_load_interval
    SettingField.ObsLoadSampling -> R.string.title_pref_observatory_least_load_sampling
    SettingField.ObsLoadTimeout -> R.string.title_pref_observatory_least_load_timeout
    SettingField.DelayTestUrl -> R.string.title_pref_delay_test_url
    SettingField.RealPingConcurrency -> R.string.title_pref_real_ping_concurrency
    SettingField.IpApiUrl -> R.string.title_pref_ip_api_url
}

@StringRes
private fun dropdownTitle(dropdown: SettingDropdown): Int = when (dropdown) {
    SettingDropdown.VpnBypassLan -> R.string.title_pref_vpn_bypass_lan
    SettingDropdown.VpnInterfaceAddress -> R.string.title_pref_vpn_interface_address
    SettingDropdown.HevLogLevel -> R.string.title_pref_hev_tunnel_loglevel
    SettingDropdown.CoreLogLevel -> R.string.title_core_loglevel
    SettingDropdown.OutboundResolveMethod -> R.string.title_outbound_domain_resolve_method
    SettingDropdown.MuxXudpQuic -> R.string.title_pref_mux_xudp_quic
    SettingDropdown.FragmentPackets -> R.string.title_pref_fragment_packets
    SettingDropdown.ObsLoadMethod -> R.string.title_pref_observatory_least_load_method
    SettingDropdown.Mode -> R.string.title_mode
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    languageEntries: List<String>,
    languageValues: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedFocus = remember { FocusRequester() }
    val selectedIndex = languageValues.indexOf(currentLanguage).takeIf { it >= 0 } ?: 0
    LaunchedEffect(Unit) { selectedFocus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.title_language),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                languageValues.forEachIndexed { index, code ->
                    val label = if (code == "auto") {
                        stringResource(R.string.language_follow_system)
                    } else {
                        languageEntries.getOrElse(index) { code }
                    }
                    TvRailItem(
                        label = label,
                        iconRes = R.drawable.ic_translate_24dp,
                        selected = code == currentLanguage,
                        onClick = { onSelect(code) },
                        modifier = if (index == selectedIndex) {
                            Modifier.focusRequester(selectedFocus)
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun languageLabel(
    currentLanguage: String,
    languageValues: List<String>,
    languageEntries: List<String>,
): String {
    if (currentLanguage == "auto") return stringResource(R.string.language_follow_system)
    val index = languageValues.indexOf(currentLanguage)
    return languageEntries.getOrElse(index) { currentLanguage }
}

private fun openSystemVpnSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
    }.onFailure { error ->
        context.toastError(R.string.toast_system_vpn_settings_unavailable)
        tv.roam.client.util.LogUtil.e(
            tv.roam.client.AppConfig.TAG,
            "Cannot open system VPN settings",
            error,
        )
    }
}
