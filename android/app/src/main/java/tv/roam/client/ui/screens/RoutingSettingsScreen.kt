package tv.roam.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.enums.RoutingType
import tv.roam.client.feature.routing.RoutingListAction
import tv.roam.client.feature.routing.RoutingListViewModel
import tv.roam.client.feature.routing.RoutingRuleRow
import tv.roam.client.ui.components.TvConfirmDialog
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvIconAction
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvOptionsDialog
import tv.roam.client.ui.components.TvPrimaryButton
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors

@Composable
fun RoutingSettingsScreen(
    viewModel: RoutingListViewModel,
    onAddRule: () -> Unit,
    onEditRule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showMore by remember { mutableStateOf(false) }
    var showStrategy by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }
    var pendingPreset by remember { mutableStateOf<RoutingType?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var focusedId by rememberSaveable { mutableStateOf<String?>(null) }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val strategyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.onAction(RoutingListAction.Load)
    }
    LaunchedEffect(state.messageRes) {
        val message = state.messageRes ?: return@LaunchedEffect
        context.toast(message)
        viewModel.onAction(RoutingListAction.ConsumeMessage)
    }
    LaunchedEffect(state.rows.map { it.id }, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        if (state.rows.isEmpty()) {
            strategyFocus.requestFocus()
            return@LaunchedEffect
        }
        val target = focusedId?.takeIf { id -> state.rows.any { it.id == id } }
            ?: state.rows.first().id
        focusedId = target
        requesters[target]?.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.routing_settings_title),
                actions = {
                    TvIconAction(
                        iconRes = R.drawable.ic_add_24dp,
                        contentDescription = stringResource(R.string.acc_add_rule),
                        onClick = onAddRule,
                    )
                    TvIconAction(
                        iconRes = R.drawable.ic_more_vert_24dp,
                        contentDescription = stringResource(R.string.acc_more),
                        onClick = { showMore = true },
                    )
                },
            )
            if (state.isImporting) {
                Text(
                    text = stringResource(R.string.tv_editor_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.Amber,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "domain_strategy") {
                        TvSettingsRow(
                            label = stringResource(R.string.routing_settings_domain_strategy),
                            value = state.domainStrategy,
                            iconRes = R.drawable.ic_routing_24dp,
                            onClick = { showStrategy = true },
                            modifier = Modifier.focusRequester(strategyFocus),
                        )
                    }
                    if (state.rows.isEmpty()) {
                        item(key = "empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.tv_routing_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TvColors.TextMuted,
                                )
                                TvPrimaryButton(
                                    text = stringResource(R.string.routing_settings_add_rule),
                                    onClick = onAddRule,
                                )
                            }
                        }
                    } else {
                        items(state.rows, key = { it.id }) { rule ->
                            val requester = remember(rule.id) { FocusRequester() }
                            SideEffect { requesters[rule.id] = requester }
                            RoutingRuleRowView(
                                rule = rule,
                                modifier = Modifier
                                    .focusRequester(requester)
                                    .onFocusChanged { focus ->
                                        if (focus.isFocused) focusedId = rule.id
                                    },
                                onClick = { onEditRule(rule.id) },
                            )
                        }
                    }
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

    if (showMore) {
        RoutingMoreDialog(
            onImportPreset = {
                showMore = false
                showPresets = true
            },
            onImportClipboard = {
                showMore = false
                showImportConfirm = true
            },
            onExportClipboard = {
                showMore = false
                viewModel.onAction(RoutingListAction.ExportClipboard)
            },
            onDismiss = { showMore = false },
        )
    }

    if (showStrategy) {
        TvOptionsDialog(
            title = stringResource(R.string.routing_settings_domain_strategy),
            options = state.strategies,
            selectedIndex = state.strategies.indexOf(state.domainStrategy),
            onSelectIndex = { index ->
                viewModel.onAction(RoutingListAction.SetDomainStrategy(index))
                showStrategy = false
            },
            onDismiss = { showStrategy = false },
        )
    }

    if (showPresets) {
        val presets = listOf(
            RoutingType.WHITE to R.string.routing_preset_china_whitelist,
            RoutingType.BLACK to R.string.routing_preset_china_blacklist,
            RoutingType.GLOBAL to R.string.routing_preset_global,
            RoutingType.WHITE_IRAN to R.string.routing_preset_iran_whitelist,
            RoutingType.WHITE_RUSSIA to R.string.routing_preset_russia_whitelist,
        )
        TvOptionsDialog(
            title = stringResource(R.string.routing_settings_import_predefined_rulesets),
            options = presets.map { stringResource(it.second) },
            selectedIndex = -1,
            onSelectIndex = { index ->
                pendingPreset = presets.getOrNull(index)?.first
                showPresets = false
            },
            onDismiss = { showPresets = false },
        )
    }

    if (showImportConfirm) {
        TvConfirmDialog(
            title = stringResource(R.string.routing_settings_import_rulesets_from_clipboard),
            message = stringResource(R.string.routing_settings_import_rulesets_tip),
            confirmText = stringResource(R.string.tv_action_import),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showImportConfirm = false
                viewModel.onAction(RoutingListAction.ImportClipboard)
            },
            onDismiss = { showImportConfirm = false },
            danger = true,
        )
    }

    pendingPreset?.let { type ->
        TvConfirmDialog(
            title = stringResource(R.string.routing_settings_import_predefined_rulesets),
            message = stringResource(R.string.routing_settings_import_rulesets_tip),
            confirmText = stringResource(R.string.tv_action_import),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                pendingPreset = null
                viewModel.onAction(RoutingListAction.ImportPreset(type))
            },
            onDismiss = { pendingPreset = null },
            danger = true,
        )
    }
}

@Composable
private fun RoutingRuleRowView(
    rule: RoutingRuleRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateText = stringResource(
        if (rule.enabled) R.string.tv_editor_on else R.string.tv_editor_off
    )
    TvSettingsRow(
        label = rule.remarks,
        value = "${rule.outboundTag} · $stateText",
        iconRes = if (rule.locked) R.drawable.ic_lock_24dp else R.drawable.ic_routing_24dp,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun RoutingMoreDialog(
    onImportPreset: () -> Unit,
    onImportClipboard: () -> Unit,
    onExportClipboard: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }
    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.tv_nav_more),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            TvSettingsRow(
                label = stringResource(R.string.routing_settings_import_predefined_rulesets),
                value = "",
                iconRes = R.drawable.ic_restore_24dp,
                onClick = onImportPreset,
                modifier = Modifier.focusRequester(firstFocus),
            )
            TvSettingsRow(
                label = stringResource(R.string.routing_settings_import_rulesets_from_clipboard),
                value = "",
                iconRes = R.drawable.ic_copy,
                onClick = onImportClipboard,
            )
            TvSettingsRow(
                label = stringResource(R.string.routing_settings_export_rulesets_to_clipboard),
                value = "",
                iconRes = R.drawable.ic_save_24dp,
                onClick = onExportClipboard,
            )
            TvSettingsRow(
                label = stringResource(R.string.action_cancel),
                value = "",
                iconRes = R.drawable.ic_arrow_back_24dp,
                onClick = onDismiss,
            )
        }
    }
}
