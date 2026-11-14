package tv.roam.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.dto.AppInfo
import tv.roam.client.extension.toast
import tv.roam.client.feature.perappproxy.PerAppProxyViewModel
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvIconAction
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTextEditDialog
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors
import tv.roam.client.util.Utils

@Composable
fun PerAppProxyScreen(
    viewModel: PerAppProxyViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
    val blacklist by viewModel.blacklist.collectAsStateWithLifecycle()
    val perAppEnabled by viewModel.perAppProxyEnabled.collectAsStateWithLifecycle()
    val bypassApps by viewModel.bypassApps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    val appFocus = remember { mutableMapOf<String, FocusRequester>() }
    var focusedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadApps(context)
    }
    LaunchedEffect(apps.map { it.packageName }, focusedPackage) {
        val current = focusedPackage ?: return@LaunchedEffect
        val target = current.takeIf { pkg -> apps.any { it.packageName == pkg } }
            ?: apps.firstOrNull()?.packageName
        if (target != null) {
            focusedPackage = target
            appFocus[target]?.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.per_app_proxy_settings),
                actions = {
                    TvIconAction(
                        iconRes = R.drawable.ic_search_24dp,
                        contentDescription = stringResource(R.string.menu_item_search),
                        onClick = { showSearch = true },
                    )
                    TvIconAction(
                        iconRes = R.drawable.ic_more_vert_24dp,
                        contentDescription = stringResource(R.string.acc_more),
                        onClick = { showMore = true },
                    )
                },
            )
            TvSettingsRow(
                label = stringResource(R.string.per_app_proxy_settings_enable),
                value = stringResource(
                    if (perAppEnabled) R.string.tv_editor_on else R.string.tv_editor_off
                ),
                iconRes = R.drawable.ic_qu_switch_24dp,
                onClick = { viewModel.setPerAppProxyEnabled(!perAppEnabled) },
                modifier = Modifier.focusRequester(firstFocus),
            )
            TvSettingsRow(
                label = stringResource(R.string.switch_bypass_apps_mode),
                value = stringResource(
                    if (bypassApps) R.string.tv_editor_on else R.string.tv_editor_off
                ),
                iconRes = R.drawable.ic_qu_switch_24dp,
                onClick = { viewModel.setBypassAppsEnabled(!bypassApps) },
            )
            Text(
                text = stringResource(R.string.summary_pref_per_app_proxy),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (isLoading) {
                Text(
                    text = stringResource(R.string.tv_editor_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.Amber,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (!isLoading && apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.tv_per_app_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextMuted,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        val requester = remember(app.packageName) { FocusRequester() }
                        SideEffect { appFocus[app.packageName] = requester }
                        AppRow(
                            app = app,
                            selected = app.packageName in blacklist,
                            modifier = Modifier
                                .focusRequester(requester)
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) focusedPackage = app.packageName
                                },
                            onClick = { viewModel.toggle(app.packageName) },
                        )
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

    if (showSearch) {
        TvTextEditDialog(
            title = stringResource(R.string.menu_item_search),
            initialValue = searchQuery,
            onSave = { value ->
                searchQuery = value
                viewModel.filterApps(value)
                showSearch = false
            },
            onDismiss = { showSearch = false },
        )
    }

    if (showMore) {
        PerAppMoreDialog(
            onSelectAll = {
                showMore = false
                viewModel.selectAll()
            },
            onInvert = {
                showMore = false
                viewModel.invertSelection()
            },
            onAutoSelect = {
                showMore = false
                viewModel.selectProxyAppAuto(context)
            },
            onImport = {
                showMore = false
                val text = Utils.getClipboard(context)
                if (text.isBlank()) {
                    context.toast(R.string.toast_failure)
                } else {
                    viewModel.importProxyApp(text, context)
                }
            },
            onExport = {
                showMore = false
                Utils.setClipboard(context, viewModel.exportProxyApp())
                context.toast(R.string.toast_success)
            },
            onDismiss = { showMore = false },
        )
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, TvColors.Teal),
                shape = RoundedCornerShape(14.dp),
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            contentColor = TvColors.Text,
            focusedContainerColor = TvColors.SurfaceFocused,
            focusedContentColor = TvColors.Text,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_per_apps_24dp),
                contentDescription = null,
                tint = TvColors.Teal,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TvColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName + if (app.isSystemApp) " · system" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_action_done),
                    contentDescription = stringResource(R.string.tv_editor_on),
                    tint = TvColors.Teal,
                )
            }
            Text(
                text = stringResource(
                    if (selected) R.string.tv_editor_on else R.string.tv_editor_off
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TvColors.Teal else TvColors.TextMuted,
            )
        }
    }
}

@Composable
private fun PerAppMoreDialog(
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onAutoSelect: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
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
                label = stringResource(R.string.menu_item_select_all),
                value = "",
                iconRes = R.drawable.ic_select_all_24dp,
                onClick = onSelectAll,
                modifier = Modifier.focusRequester(firstFocus),
            )
            TvSettingsRow(
                label = stringResource(R.string.menu_item_invert_selection),
                value = "",
                iconRes = R.drawable.ic_qu_switch_24dp,
                onClick = onInvert,
            )
            TvSettingsRow(
                label = stringResource(R.string.menu_item_select_proxy_app),
                value = "",
                iconRes = R.drawable.ic_per_apps_24dp,
                onClick = onAutoSelect,
            )
            TvSettingsRow(
                label = stringResource(R.string.menu_item_import_proxy_app),
                value = "",
                iconRes = R.drawable.ic_copy,
                onClick = onImport,
            )
            TvSettingsRow(
                label = stringResource(R.string.menu_item_export_proxy_app),
                value = "",
                iconRes = R.drawable.ic_save_24dp,
                onClick = onExport,
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
