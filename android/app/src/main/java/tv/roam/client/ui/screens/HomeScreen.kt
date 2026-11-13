package tv.roam.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.MutableStateFlow
import tv.roam.client.R
import tv.roam.client.dto.entities.ServersCache
import tv.roam.client.enums.EConfigType
import tv.roam.client.extension.toast
import tv.roam.client.feature.main.MainAction
import tv.roam.client.feature.main.MainStatus
import tv.roam.client.feature.main.MainViewModel
import tv.roam.client.ui.components.TvChip
import tv.roam.client.ui.components.TvConfirmDialog
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvIconAction
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvPrimaryButton
import tv.roam.client.ui.components.TvQrDialog
import tv.roam.client.ui.components.TvRailItem
import tv.roam.client.ui.components.TvSecondaryButton
import tv.roam.client.ui.components.TvStatusDot
import tv.roam.client.ui.theme.TvColors

private enum class BulkDelete {
    Duplicates,
    Invalid,
    All,
}

/**
 * Television home: group tabs, a focusable server list and a persistent detail
 * pane. Selecting a server persists the selection; the detail pane owns the
 * explicit Connect / Test / Delete actions so browsing cannot accidentally
 * start a tunnel.
 */
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRestartService: () -> Unit,
    onImport: () -> Unit,
    onAddServer: (Int) -> Unit,
    onEditServer: (String, Int, Boolean) -> Unit,
    onAddPolicyGroup: () -> Unit,
    onEditPolicyGroup: (String) -> Unit,
    onAddProxyChain: () -> Unit,
    onEditProxyChain: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val groupId = uiState.selectedGroupId
    val serversFlow = remember(groupId) {
        if (groupId.isBlank()) {
            MutableStateFlow(emptyList<ServersCache>())
        } else {
            mainViewModel.serversForGroup(groupId)
        }
    }
    val servers by serversFlow.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val selectedGuid = uiState.selectedGuid
    val isRunning = uiState.isRunning
    val isTesting = uiState.isTesting
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap
    val statusText = when (val status = uiState.status) {
        MainStatus.Disconnected -> stringResource(R.string.connection_not_connected)
        MainStatus.Connected -> stringResource(R.string.tv_status_connected)
        MainStatus.Testing -> stringResource(R.string.connection_test_testing)
        is MainStatus.TestProgress -> status.progress
        is MainStatus.ConnectionTest -> {
            if (status.result.delayMillis > 0L) {
                stringResource(
                    R.string.connection_test_available,
                    stringResource(R.string.server_test_delay_value, status.result.delayMillis),
                )
            } else {
                stringResource(R.string.tv_status_test_failed)
            }
        }
    }

    var focusedGuid by rememberSaveable { mutableStateOf<String?>(null) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val importFocus = remember { FocusRequester() }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ServersCache?>(null) }
    var bulkDelete by remember { mutableStateOf<BulkDelete?>(null) }

    val detailServer = servers.firstOrNull { it.guid == focusedGuid }
        ?: servers.firstOrNull { it.guid == selectedGuid }
        ?: servers.firstOrNull()
    val detailIsCurrent = detailServer != null && isRunning && selectedGuid == detailServer.guid

    LaunchedEffect(servers.map { it.guid }, selectedGuid, groupId) {
        if (servers.isEmpty()) {
            importFocus.requestFocus()
            return@LaunchedEffect
        }
        val currentFocusIsValid = focusedGuid != null && servers.any { it.guid == focusedGuid }
        if (!currentFocusIsValid) {
            val target = selectedGuid?.takeIf { guid -> servers.any { it.guid == guid } }
                ?: servers.first().guid
            focusedGuid = target
            focusRequesters.getOrPut(target) { FocusRequester() }.requestFocus()
        }
    }

    val connectOrDisconnect: (ServersCache) -> Unit = { server ->
        val isCurrent = isRunning && selectedGuid == server.guid
        when {
            isCurrent -> onStopService()
            selectedGuid != server.guid -> {
                mainViewModel.updateSelectedGuid(server.guid)
                if (isRunning) onRestartService() else onStartService()
            }

            else -> onStartService()
        }
    }

    val selectServer: (ServersCache) -> Unit = { server ->
        val changed = selectedGuid != server.guid
        mainViewModel.updateSelectedGuid(server.guid)
        if (changed && isRunning) onRestartService()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            HomeTopBand(
                statusText = statusText,
                statusColor = when {
                    isTesting -> TvColors.Amber
                    isRunning -> TvColors.Success
                    else -> TvColors.TextMuted
                },
                connectLabel = if (detailIsCurrent) {
                    stringResource(R.string.tv_action_disconnect)
                } else {
                    stringResource(R.string.tv_action_connect)
                },
                connectEnabled = detailServer != null,
                onConnect = { detailServer?.let(connectOrDisconnect) },
                onSearch = { showSearch = true },
                onMore = { showMore = true },
            )

            if (groups.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(groups, key = { it.id }) { group ->
                        TvChip(
                            text = group.remarks.ifBlank {
                                stringResource(R.string.tv_home_ungrouped)
                            },
                            selected = group.id == groupId,
                            onClick = {
                                mainViewModel.onAction(MainAction.SelectGroup(group.id))
                            },
                        )
                    }
                }
            } else {
                Text(
                    text = if (servers.size == 1) {
                        stringResource(R.string.tv_home_server_count_one)
                    } else {
                        stringResource(R.string.tv_home_server_count, servers.size)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (servers.isEmpty()) {
                    EmptyHome(
                        modifier = Modifier.fillMaxSize(),
                        isLoading = isLoading,
                        isSearching = searchQuery.isNotBlank(),
                        importFocus = importFocus,
                        onImport = onImport,
                    )
                } else if (maxWidth >= 600.dp) {
                    Row(Modifier.fillMaxSize()) {
                        ServerList(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            servers = servers,
                            selectedGuid = selectedGuid,
                            focusRequesters = focusRequesters,
                            onSelect = selectServer,
                            onFocused = { focusedGuid = it },
                        )
                        Spacer(Modifier.width(16.dp))
                        DetailPane(
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight(),
                            server = detailServer,
                            isRunning = isRunning,
                            selectedGuid = selectedGuid,
                            onConnect = { detailServer?.let(connectOrDisconnect) },
                            onTest = {
                                val server = detailServer
                                if (server != null && isRunning && selectedGuid == server.guid) {
                                    mainViewModel.testCurrentServerRealPing()
                                } else {
                                    context.toast(R.string.tv_home_test_requires_connection)
                                }
                            },
                            onDelete = { server -> deleteTarget = server },
                        )
                    }
                } else {
                    // 720p televisions do not have room for a 300dp detail
                    // pane next to the rail. The list keeps the full width and
                    // the same actions move into the More dialog.
                    ServerList(
                        modifier = Modifier.fillMaxSize(),
                        servers = servers,
                        selectedGuid = selectedGuid,
                        focusRequesters = focusRequesters,
                        onSelect = selectServer,
                        onFocused = { focusedGuid = it },
                    )
                }
            }
        }

        TvKeyHintBar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            hints = listOf(
                stringResource(R.string.tv_key_ok) to stringResource(R.string.tv_key_ok_action),
                stringResource(R.string.tv_key_menu) to stringResource(R.string.tv_key_menu_action),
                stringResource(R.string.tv_key_back) to stringResource(R.string.tv_key_back_action),
            ),
        )
    }

    if (showSearch) {
        SearchDialog(
            query = searchQuery,
            onQueryChange = { query ->
                searchQuery = query
                mainViewModel.onAction(MainAction.Search(query))
            },
            onClose = {
                showSearch = false
                searchQuery = ""
                mainViewModel.onAction(MainAction.Search(""))
            },
            onDismiss = {
                showSearch = false
                searchQuery = ""
                mainViewModel.onAction(MainAction.Search(""))
            },
        )
    }

    if (showMore) {
        val selectedServer = servers.firstOrNull { it.guid == selectedGuid }
        MoreDialog(
            isRunning = isRunning,
            isTesting = isTesting,
            selectedServer = selectedServer,
            onTestSelected = {
                showMore = false
                if (selectedServer != null && isRunning && selectedGuid == selectedServer.guid) {
                    mainViewModel.testCurrentServerRealPing()
                } else {
                    context.toast(R.string.tv_home_test_requires_connection)
                }
            },
            onDeleteSelected = {
                showMore = false
                selectedServer?.let { deleteTarget = it }
            },
            onShareSelected = {
                showMore = false
                selectedServer?.let {
                    mainViewModel.onAction(MainAction.ShareQRCode(it.guid))
                }
            },
            onAddServer = {
                showMore = false
                showTypePicker = true
            },
            onEditSelected = {
                showMore = false
                selectedServer?.let {
                    when (it.profile.configType) {
                        EConfigType.POLICYGROUP -> onEditPolicyGroup(it.guid)
                        EConfigType.PROXYCHAIN -> onEditProxyChain(it.guid)
                        else -> onEditServer(
                            it.guid,
                            it.profile.configType.value,
                            isRunning && selectedGuid == it.guid,
                        )
                    }
                }
            },
            onDismiss = { showMore = false },
            onImport = {
                showMore = false
                onImport()
            },
            onAction = { action ->
                showMore = false
                mainViewModel.onAction(action)
            },
            onRestart = {
                showMore = false
                onRestartService()
            },
            onBulkDelete = { target ->
                showMore = false
                bulkDelete = target
            },
        )
    }

    if (showTypePicker) {
        ServerTypeDialog(
            onSelect = { type ->
                showTypePicker = false
                onAddServer(type.value)
            },
            onSelectPolicyGroup = {
                showTypePicker = false
                onAddPolicyGroup()
            },
            onSelectProxyChain = {
                showTypePicker = false
                onAddProxyChain()
            },
            onDismiss = { showTypePicker = false },
        )
    }

    deleteTarget?.let { server ->
        TvConfirmDialog(
            title = stringResource(R.string.action_delete),
            message = stringResource(R.string.confirm_delete_profile),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                deleteTarget = null
                mainViewModel.onAction(MainAction.RemoveServer(server.guid))
            },
            onDismiss = { deleteTarget = null },
            danger = true,
        )
    }

    shareQRCodeBitmap?.let { bitmap ->
        TvQrDialog(
            bitmap = bitmap,
            onDismiss = { mainViewModel.onAction(MainAction.DismissQRCodeDialog) },
        )
    }

    bulkDelete?.let { target ->
        TvConfirmDialog(
            title = stringResource(
                when (target) {
                    BulkDelete.Duplicates -> R.string.title_del_duplicate_config
                    BulkDelete.Invalid -> R.string.title_del_invalid_config
                    BulkDelete.All -> R.string.title_del_all_config
                }
            ),
            message = stringResource(R.string.tv_confirm_bulk_message),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                bulkDelete = null
                mainViewModel.onAction(
                    when (target) {
                        BulkDelete.Duplicates -> MainAction.RemoveDuplicateServers
                        BulkDelete.Invalid -> MainAction.RemoveInvalidServers
                        BulkDelete.All -> MainAction.RemoveAllServers
                    }
                )
            },
            onDismiss = { bulkDelete = null },
            danger = true,
        )
    }
}

@Composable
private fun HomeTopBand(
    statusText: String,
    statusColor: Color,
    connectLabel: String,
    connectEnabled: Boolean,
    onConnect: () -> Unit,
    onSearch: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.tv_nav_home),
            style = MaterialTheme.typography.headlineSmall,
            color = TvColors.Text,
        )
        Spacer(Modifier.width(20.dp))
        TvStatusDot(color = statusColor)
        Spacer(Modifier.width(10.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleSmall,
            color = TvColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TvPrimaryButton(
            text = connectLabel,
            onClick = onConnect,
            enabled = connectEnabled,
        )
        Spacer(Modifier.width(8.dp))
        TvIconAction(
            iconRes = R.drawable.ic_search_24dp,
            contentDescription = stringResource(R.string.acc_search),
            onClick = onSearch,
        )
        TvIconAction(
            iconRes = R.drawable.ic_more_vert_24dp,
            contentDescription = stringResource(R.string.acc_more),
            onClick = onMore,
        )
    }
}

@Composable
private fun EmptyHome(
    modifier: Modifier,
    isLoading: Boolean,
    isSearching: Boolean,
    importFocus: FocusRequester,
    onImport: () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            colors = SurfaceDefaults.colors(
                containerColor = TvColors.Surface,
                contentColor = TvColors.Text,
            ),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = when {
                        isLoading -> stringResource(R.string.tv_status_loading)
                        isSearching -> stringResource(R.string.tv_home_search_empty)
                        else -> stringResource(R.string.tv_home_empty_title)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = TvColors.Text,
                )
                if (!isSearching) {
                    Text(
                        text = stringResource(R.string.tv_home_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextMuted,
                    )
                    TvPrimaryButton(
                        text = stringResource(R.string.tv_action_import),
                        onClick = onImport,
                        modifier = Modifier.focusRequester(importFocus),
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerList(
    modifier: Modifier,
    servers: List<ServersCache>,
    selectedGuid: String?,
    focusRequesters: MutableMap<String, FocusRequester>,
    onSelect: (ServersCache) -> Unit,
    onFocused: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(servers, key = { it.guid }) { server ->
            val focusRequester = remember(server.guid) {
                focusRequesters.getOrPut(server.guid) { FocusRequester() }
            }
            ServerRow(
                server = server,
                selected = server.guid == selectedGuid,
                focusRequester = focusRequester,
                onClick = { onSelect(server) },
                onFocused = { onFocused(server.guid) },
            )
        }
    }
}

@Composable
private fun ServerRow(
    server: ServersCache,
    selected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onFocused: () -> Unit,
) {
    val profile = server.profile
    val protocol = buildString {
        append(profile.configType.name)
        profile.network?.takeIf { it.isNotBlank() && !it.equals("tcp", true) }?.let {
            append(" / ")
            append(it)
        }
    }
    val address = listOf(profile.server.orEmpty(), profile.serverPort.orEmpty())
        .filter { it.isNotBlank() }
        .joinToString(":")

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, TvColors.Teal),
                shape = RoundedCornerShape(14.dp),
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) TvColors.Teal.copy(alpha = 0.10f) else TvColors.Surface,
            contentColor = TvColors.Text,
            focusedContainerColor = TvColors.SurfaceFocused,
            focusedContentColor = TvColors.Text,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.remarks.ifBlank { address },
                    style = MaterialTheme.typography.titleMedium,
                    color = TvColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$protocol · $address",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LatencyBadge(server.testDelayMillis)
            if (selected) {
                Spacer(Modifier.width(10.dp))
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_action_done),
                    contentDescription = null,
                    tint = TvColors.Teal,
                )
            }
        }
    }
}

@Composable
private fun LatencyBadge(delayMillis: Long) {
    val color = when {
        delayMillis <= 0L -> TvColors.TextMuted
        delayMillis < 200L -> TvColors.Success
        delayMillis < 500L -> TvColors.Amber
        else -> TvColors.Error
    }
    val text = if (delayMillis > 0L) {
        stringResource(R.string.server_test_delay_value, delayMillis)
    } else {
        stringResource(R.string.tv_home_untested)
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun DetailPane(
    modifier: Modifier,
    server: ServersCache?,
    isRunning: Boolean,
    selectedGuid: String?,
    onConnect: () -> Unit,
    onTest: () -> Unit,
    onDelete: (ServersCache) -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(
            containerColor = TvColors.Surface,
            contentColor = TvColors.Text,
        ),
    ) {
        if (server == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.tv_home_select_server),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextMuted,
                )
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = server.profile.remarks.ifBlank { server.profile.server.orEmpty() },
                style = MaterialTheme.typography.titleLarge,
                color = TvColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(
                    server.profile.server.orEmpty(),
                    server.profile.serverPort.orEmpty()
                )
                    .filter { it.isNotBlank() }
                    .joinToString(":"),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = server.profile.configType.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = TvColors.TextMuted,
                )
                LatencyBadge(server.testDelayMillis)
            }

            Spacer(Modifier.height(2.dp))

            val isCurrent = isRunning && selectedGuid == server.guid
            TvPrimaryButton(
                text = if (isCurrent) {
                    stringResource(R.string.tv_action_disconnect)
                } else {
                    stringResource(R.string.tv_action_connect)
                },
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
            )
            TvSecondaryButton(
                text = stringResource(R.string.tv_action_test),
                onClick = onTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = isCurrent,
            )
            TvSecondaryButton(
                text = stringResource(R.string.action_delete),
                onClick = { onDelete(server) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedGuid != server.guid,
            )
        }
    }
}

@Composable
private fun SearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.menu_item_search),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onClose() }),
                textStyle = TextStyle(
                    color = TvColors.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(TvColors.Teal),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .background(TvColors.SurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tv_home_search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TvColors.TextMuted,
                            )
                        }
                        inner()
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TvSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                )
                TvPrimaryButton(
                    text = stringResource(R.string.menu_item_search),
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun MoreDialog(
    isRunning: Boolean,
    isTesting: Boolean,
    selectedServer: ServersCache?,
    onTestSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onAddServer: () -> Unit,
    onEditSelected: () -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onAction: (MainAction) -> Unit,
    onRestart: () -> Unit,
    onBulkDelete: (BulkDelete) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.tv_nav_more),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MenuRow(
                    label = stringResource(R.string.menu_item_add_config),
                    iconRes = R.drawable.ic_add_24dp,
                    onClick = onAddServer,
                    modifier = Modifier.focusRequester(firstFocus),
                )
                MenuRow(
                    label = stringResource(R.string.tv_action_import),
                    iconRes = R.drawable.ic_add_24dp,
                    onClick = onImport,
                )
                MenuRow(
                    label = stringResource(R.string.title_ping_all_server),
                    iconRes = R.drawable.ic_fab_check,
                    onClick = { onAction(MainAction.TestAllServers) },
                )
                MenuRow(
                    label = stringResource(R.string.title_real_ping_all_server),
                    iconRes = R.drawable.ic_fab_check,
                    onClick = { onAction(MainAction.TestRealAllServers) },
                )
                if (selectedServer != null) {
                    MenuRow(
                        label = stringResource(R.string.menu_item_edit_config) +
                            " · " + selectedServer.profile.remarks,
                        iconRes = R.drawable.ic_edit_24dp,
                        onClick = onEditSelected,
                    )
                    MenuRow(
                        label = stringResource(R.string.tv_action_test) +
                            " · " + selectedServer.profile.remarks,
                        iconRes = R.drawable.ic_fab_check,
                        onClick = onTestSelected,
                    )
                    MenuRow(
                        label = stringResource(R.string.share_method_qrcode) +
                            " · " + selectedServer.profile.remarks,
                        iconRes = R.drawable.ic_share_24dp,
                        onClick = onShareSelected,
                    )
                    MenuRow(
                        label = stringResource(R.string.action_delete) +
                            " · " + selectedServer.profile.remarks,
                        iconRes = R.drawable.ic_delete_24dp,
                        onClick = onDeleteSelected,
                    )
                }
                if (isTesting) {
                    MenuRow(
                        label = stringResource(R.string.action_cancel),
                        iconRes = R.drawable.ic_stop_24dp,
                        onClick = { onAction(MainAction.CancelTesting) },
                    )
                }
                MenuRow(
                    label = stringResource(R.string.title_sort_by_test_results),
                    iconRes = R.drawable.ic_check_update_24dp,
                    onClick = { onAction(MainAction.SortByTestResults) },
                )
                MenuRow(
                    label = stringResource(R.string.title_sub_update),
                    iconRes = R.drawable.ic_refresh_24dp,
                    onClick = { onAction(MainAction.UpdateSubscriptions) },
                )
                if (isRunning) {
                    MenuRow(
                        label = stringResource(R.string.title_service_restart),
                        iconRes = R.drawable.ic_play_24dp,
                        onClick = onRestart,
                    )
                }
                MenuRow(
                    label = stringResource(R.string.title_del_duplicate_config),
                    iconRes = R.drawable.ic_delete_24dp,
                    onClick = { onBulkDelete(BulkDelete.Duplicates) },
                )
                MenuRow(
                    label = stringResource(R.string.title_del_invalid_config),
                    iconRes = R.drawable.ic_delete_24dp,
                    onClick = { onBulkDelete(BulkDelete.Invalid) },
                )
                MenuRow(
                    label = stringResource(R.string.title_del_all_config),
                    iconRes = R.drawable.ic_delete_24dp,
                    onClick = { onBulkDelete(BulkDelete.All) },
                )
                MenuRow(
                    label = stringResource(R.string.action_cancel),
                    iconRes = R.drawable.ic_arrow_back_24dp,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun ServerTypeDialog(
    onSelect: (EConfigType) -> Unit,
    onSelectPolicyGroup: () -> Unit,
    onSelectProxyChain: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }
    val entries = listOf(
        EConfigType.VMESS to R.string.menu_item_import_config_manually_vmess,
        EConfigType.VLESS to R.string.menu_item_import_config_manually_vless,
        EConfigType.SHADOWSOCKS to R.string.menu_item_import_config_manually_ss,
        EConfigType.SOCKS to R.string.menu_item_import_config_manually_socks,
        EConfigType.HTTP to R.string.menu_item_import_config_manually_http,
        EConfigType.TROJAN to R.string.menu_item_import_config_manually_trojan,
        EConfigType.WIREGUARD to R.string.menu_item_import_config_manually_wireguard,
        EConfigType.HYSTERIA2 to R.string.menu_item_import_config_manually_hysteria2,
        EConfigType.CUSTOM to R.string.tv_editor_protocol_custom,
    )

    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.menu_item_add_config),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                entries.forEachIndexed { index, (type, labelRes) ->
                    MenuRow(
                        label = stringResource(labelRes),
                        iconRes = R.drawable.ic_add_24dp,
                        onClick = { onSelect(type) },
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstFocus)
                        } else {
                            Modifier
                        },
                    )
                }
                MenuRow(
                    label = stringResource(R.string.menu_item_import_config_policy_group),
                    iconRes = R.drawable.ic_add_24dp,
                    onClick = onSelectPolicyGroup,
                )
                MenuRow(
                    label = stringResource(R.string.menu_item_import_config_proxy_chain),
                    iconRes = R.drawable.ic_add_24dp,
                    onClick = onSelectProxyChain,
                )
                MenuRow(
                    label = stringResource(R.string.action_cancel),
                    iconRes = R.drawable.ic_arrow_back_24dp,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun MenuRow(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvRailItem(
        label = label,
        iconRes = iconRes,
        selected = false,
        onClick = onClick,
        modifier = modifier,
    )
}
