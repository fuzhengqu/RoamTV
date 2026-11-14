package tv.roam.client.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.feature.main.MainAction
import tv.roam.client.feature.about.AboutViewModel
import tv.roam.client.feature.backup.BackupViewModel
import tv.roam.client.feature.checkupdate.CheckUpdateViewModel
import tv.roam.client.feature.logcat.LogcatViewModel
import tv.roam.client.feature.main.MainViewModel
import tv.roam.client.feature.pairing.PairingViewModel
import tv.roam.client.feature.perappproxy.PerAppProxyViewModel
import tv.roam.client.feature.routing.RoutingEditorViewModel
import tv.roam.client.feature.routing.RoutingListViewModel
import tv.roam.client.feature.server.PolicyGroupEditorViewModel
import tv.roam.client.feature.server.ProxyChainEditorViewModel
import tv.roam.client.feature.server.ServerEditorViewModel
import tv.roam.client.feature.subscription.SubscriptionEditorViewModel
import tv.roam.client.feature.subscription.SubscriptionListViewModel
import tv.roam.client.feature.settings.SettingsViewModel
import tv.roam.client.feature.userasset.UserAssetEditorViewModel
import tv.roam.client.feature.userasset.UserAssetViewModel
import tv.roam.client.handler.SettingsChangeManager
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvOverscan
import tv.roam.client.ui.components.TvRailItem
import tv.roam.client.ui.components.TvRailLayout
import tv.roam.client.ui.navigation.TvDestination
import tv.roam.client.ui.navigation.TvNavRail
import tv.roam.client.ui.navigation.TvRoute
import tv.roam.client.ui.navigation.TvSecondaryDestination
import tv.roam.client.ui.navigation.TvNavigator
import tv.roam.client.ui.navigation.isTopLevel
import tv.roam.client.ui.navigation.rememberTvNavigator
import tv.roam.client.ui.screens.AboutScreen
import tv.roam.client.ui.screens.BackupScreen
import tv.roam.client.ui.screens.HomeScreen
import tv.roam.client.ui.screens.ImportScreen
import tv.roam.client.ui.screens.LogcatScreen
import tv.roam.client.ui.screens.PlaceholderScreen
import tv.roam.client.ui.screens.PerAppProxyScreen
import tv.roam.client.ui.screens.PolicyGroupEditorScreen
import tv.roam.client.ui.screens.ProxyChainEditorScreen
import tv.roam.client.ui.screens.RoutingEditorScreen
import tv.roam.client.ui.screens.RoutingSettingsScreen
import tv.roam.client.ui.screens.ServerEditorScreen
import tv.roam.client.ui.screens.SettingsScreen
import tv.roam.client.ui.screens.SubscriptionEditorScreen
import tv.roam.client.ui.screens.SubscriptionsScreen
import tv.roam.client.ui.screens.UpdateScreen
import tv.roam.client.ui.screens.UserAssetEditorScreen
import tv.roam.client.ui.screens.UserAssetScreen
import tv.roam.client.ui.theme.TvColors

@Composable
fun TvApp(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    pairingViewModel: PairingViewModel,
    serverEditorViewModel: ServerEditorViewModel,
    policyGroupEditorViewModel: PolicyGroupEditorViewModel,
    proxyChainEditorViewModel: ProxyChainEditorViewModel,
    subscriptionListViewModel: SubscriptionListViewModel,
    subscriptionEditorViewModel: SubscriptionEditorViewModel,
    routingListViewModel: RoutingListViewModel,
    routingEditorViewModel: RoutingEditorViewModel,
    perAppProxyViewModel: PerAppProxyViewModel,
    userAssetViewModel: UserAssetViewModel,
    userAssetEditorViewModel: UserAssetEditorViewModel,
    backupViewModel: BackupViewModel,
    logcatViewModel: LogcatViewModel,
    aboutViewModel: AboutViewModel,
    checkUpdateViewModel: CheckUpdateViewModel,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRestartService: () -> Unit,
    navigator: TvNavigator = rememberTvNavigator(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var showMore by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (!navigator.pop()) {
            activity?.moveTaskToBack(true)
        }
    }

    val current = navigator.current

    // Settings writes only persist values; apply pending service/group work
    // once the user leaves the Settings screen so a burst of changes causes at
    // most one restart.
    LaunchedEffect(current) {
        if (current == TvRoute.Settings) return@LaunchedEffect
        val refreshGroups = SettingsChangeManager.consumeSetupGroupTab()
        val restart = SettingsChangeManager.consumeRestartService()
        mainViewModel.refreshUiSettings()
        if (refreshGroups) mainViewModel.onAction(MainAction.RefreshGroups)
        if (restart && mainViewModel.uiState.value.isRunning) onRestartService()
    }

    val screen: @Composable () -> Unit = {
        when (val route = current) {
            TvRoute.Home -> HomeScreen(
                mainViewModel = mainViewModel,
                onStartService = onStartService,
                onStopService = onStopService,
                onRestartService = onRestartService,
                onImport = { navigator.push(TvRoute.Import) },
                onAddServer = { type ->
                    navigator.push(
                        TvRoute.ServerEditor(
                            guid = "",
                            configType = type,
                            restartOnSave = false,
                        )
                    )
                },
                onEditServer = { guid, type, restart ->
                    navigator.push(
                        TvRoute.ServerEditor(
                            guid = guid,
                            configType = type,
                            restartOnSave = restart,
                        )
                    )
                },
                onAddPolicyGroup = {
                    navigator.push(TvRoute.PolicyGroupEditor())
                },
                onEditPolicyGroup = { guid ->
                    navigator.push(TvRoute.PolicyGroupEditor(guid))
                },
                onAddProxyChain = {
                    navigator.push(TvRoute.ProxyChainEditor())
                },
                onEditProxyChain = { guid ->
                    navigator.push(TvRoute.ProxyChainEditor(guid))
                },
            )

            TvRoute.Subscriptions -> SubscriptionsScreen(
                viewModel = subscriptionListViewModel,
                onAdd = { navigator.push(TvRoute.SubscriptionEditor()) },
                onEdit = { guid -> navigator.push(TvRoute.SubscriptionEditor(guid)) },
            )

            TvRoute.Routing -> RoutingSettingsScreen(
                viewModel = routingListViewModel,
                onAddRule = { navigator.push(TvRoute.RoutingEditor()) },
                onEditRule = { ruleId -> navigator.push(TvRoute.RoutingEditor(ruleId)) },
            )

            TvRoute.PerApp -> PerAppProxyScreen(viewModel = perAppProxyViewModel)

            TvRoute.Assets -> UserAssetScreen(
                viewModel = userAssetViewModel,
                onAddUrl = { navigator.push(TvRoute.AssetEditor()) },
                onEditAsset = { guid -> navigator.push(TvRoute.AssetEditor(guid)) },
            )

            TvRoute.Settings -> SettingsScreen(
                viewModel = settingsViewModel,
                pairingViewModel = pairingViewModel,
            )

            TvRoute.Import -> ImportScreen(
                mainViewModel = mainViewModel,
                pairingViewModel = pairingViewModel,
                onBack = { navigator.pop() },
                onImported = { navigator.openRoot(TvRoute.Home) },
            )

            TvRoute.Backup -> BackupScreen(
                viewModel = backupViewModel,
                onBack = { navigator.pop() },
            )

            TvRoute.Logs -> LogcatScreen(
                viewModel = logcatViewModel,
                onBack = { navigator.pop() },
            )

            TvRoute.About -> AboutScreen(
                viewModel = aboutViewModel,
                onBack = { navigator.pop() },
            )

            TvRoute.Update -> UpdateScreen(
                viewModel = checkUpdateViewModel,
                onBack = { navigator.pop() },
            )

            is TvRoute.ServerEditor -> ServerEditorScreen(
                route = route,
                viewModel = serverEditorViewModel,
                pairingViewModel = pairingViewModel,
                onBack = { navigator.pop() },
                onSaved = { _, restart ->
                    navigator.pop()
                    mainViewModel.onAction(MainAction.RefreshGroups)
                    if (restart) onRestartService()
                },
                onDeleted = {
                    navigator.pop()
                    mainViewModel.onAction(MainAction.RefreshGroups)
                },
            )

            is TvRoute.PolicyGroupEditor -> PolicyGroupEditorScreen(
                route = route,
                viewModel = policyGroupEditorViewModel,
                onBack = { navigator.pop() },
                onSaved = {
                    navigator.pop()
                    mainViewModel.onAction(MainAction.RefreshGroups)
                },
                onDeleted = {
                    navigator.pop()
                    mainViewModel.onAction(MainAction.RefreshGroups)
                },
            )

            is TvRoute.ProxyChainEditor -> ProxyChainEditorScreen(
                route = route,
                viewModel = proxyChainEditorViewModel,
                onBack = { navigator.pop() },
                onSaved = {
                    navigator.pop()
                    mainViewModel.onAction(MainAction.RefreshGroups)
                },
                onDeleted = {
                    navigator.pop()
                    mainViewModel.onAction(MainAction.RefreshGroups)
                },
            )

            is TvRoute.SubscriptionEditor -> SubscriptionEditorScreen(
                route = route,
                viewModel = subscriptionEditorViewModel,
                pairingViewModel = pairingViewModel,
                onBack = { navigator.pop() },
                onSaved = {
                    navigator.pop()
                    mainViewModel.onAction(MainAction.RefreshGroups)
                },
                onDeleted = {
                    navigator.pop()
                    mainViewModel.onAction(MainAction.RefreshGroups)
                },
            )

            is TvRoute.RoutingEditor -> RoutingEditorScreen(
                route = route,
                viewModel = routingEditorViewModel,
                pairingViewModel = pairingViewModel,
                onBack = { navigator.pop() },
                onSaved = { navigator.pop() },
                onDeleted = { navigator.pop() },
            )

            is TvRoute.AssetEditor -> UserAssetEditorScreen(
                route = route,
                viewModel = userAssetEditorViewModel,
                pairingViewModel = pairingViewModel,
                onBack = { navigator.pop() },
                onSaved = { navigator.pop() },
                onDeleted = { navigator.pop() },
            )

            else -> PlaceholderScreen(
                title = routeTitle(route),
                onBack = if (navigator.canGoBack) {
                    { navigator.pop() }
                } else {
                    null
                },
            )
        }
    }

    if (current.isTopLevel) {
        TvRailLayout(
            rail = {
                TvNavRail(
                    current = current,
                    onSelect = { route -> navigator.openRoot(route) },
                    onMore = { showMore = true },
                )
            },
            content = screen,
        )
    } else {
        // Secondary flows replace the rail with a back top bar, keeping the
        // whole width for the import/editor content.
        TvOverscan {
            screen()
        }
    }

    if (showMore) {
        MoreDialog(
            onDismiss = { showMore = false },
            onSelect = { route ->
                showMore = false
                navigator.push(route)
            },
        )
    }
}

@Composable
private fun MoreDialog(
    onDismiss: () -> Unit,
    onSelect: (TvRoute) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 320.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.tv_nav_more),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            TvSecondaryDestination.entries.forEachIndexed { index, destination ->
                TvRailItem(
                    label = stringResource(destination.labelRes),
                    iconRes = secondaryIcon(destination),
                    selected = false,
                    onClick = { onSelect(destination.route) },
                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
    }
}

private fun secondaryIcon(destination: TvSecondaryDestination): Int = when (destination) {
    TvSecondaryDestination.Logs -> R.drawable.ic_logcat_24dp
    TvSecondaryDestination.Backup -> R.drawable.ic_restore_24dp
    TvSecondaryDestination.Update -> R.drawable.ic_check_update_24dp
    TvSecondaryDestination.About -> R.drawable.ic_about_24dp
}

@Composable
private fun routeTitle(route: TvRoute): String = when (route) {
    TvRoute.Home -> stringResource(R.string.tv_nav_home)
    TvRoute.Subscriptions -> stringResource(R.string.title_sub_setting)
    TvRoute.Routing -> stringResource(R.string.routing_settings_title)
    TvRoute.PerApp -> stringResource(R.string.per_app_proxy_settings)
    TvRoute.Assets -> stringResource(R.string.title_user_asset_setting)
    TvRoute.Settings -> stringResource(R.string.title_settings)
    TvRoute.Logs -> stringResource(R.string.title_logcat)
    TvRoute.Backup -> stringResource(R.string.title_configuration_backup_restore)
    TvRoute.About -> stringResource(R.string.title_about)
    TvRoute.Update -> stringResource(R.string.update_check_for_update)
    TvRoute.Import -> stringResource(R.string.tv_action_import)
    is TvRoute.ServerEditor -> stringResource(R.string.menu_item_edit_config)
    is TvRoute.PolicyGroupEditor ->
        stringResource(R.string.menu_item_import_config_policy_group)

    is TvRoute.ProxyChainEditor ->
        stringResource(R.string.menu_item_import_config_proxy_chain)

    is TvRoute.SubscriptionEditor -> stringResource(R.string.title_sub_setting)
    is TvRoute.RoutingEditor -> stringResource(R.string.routing_settings_rule_title)
    is TvRoute.AssetEditor -> stringResource(R.string.title_user_asset_add_url)
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Kept for future rail-selection helpers that need the same mapping.
internal fun TvDestination.matches(route: TvRoute): Boolean = this.route.key == route.key
