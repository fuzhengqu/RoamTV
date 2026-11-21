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
import tv.roam.client.dto.TrafficSample
import tv.roam.client.dto.entities.ServersCache
import tv.roam.client.enums.EConfigType
import tv.roam.client.extension.toSpeedString
import tv.roam.client.extension.toast
import tv.roam.client.feature.main.MainAction
import tv.roam.client.feature.main.MainStatus
import tv.roam.client.feature.main.MainViewModel
import tv.roam.client.ui.components.TvChip
import tv.roam.client.ui.components.TvCompactButton
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
 * Television home: group tabs, a focusable server list and a persistent action
 * pane. Moving the list cursor only previews a server; the pane owns the
 * explicit Connect / Edit / Share / Test / Delete actions so browsing cannot
 * accidentally switch the active tunnel.
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
    val detailPrimaryFocus = remember { FocusRequester() }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showBatch by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ServersCache?>(null) }
    var bulkDelete by remember { mutableStateOf<BulkDelete?>(null) }

    val activeServer = servers.firstOrNull { it.guid == selectedGuid }
    val detailServer = servers.firstOrNull { it.guid == focusedGuid }
        ?: activeServer
        ?: servers.firstOrNull()
    val detailIsActive = detailServer != null && isRunning && selectedGuid == detailServer.guid

    LaunchedEffect(servers.map { it.guid }, selectedGuid, groupId, uiState.lastImportedGuid) {
        if (servers.isEmpty()) {
            mainViewModel.clearLastImportedGuid()
            importFocus.requestFocus()
            return@LaunchedEffect
        }
        val imported = uiState.lastImportedGuid
            ?.takeIf { guid -> servers.any { it.guid == guid } }
        if (imported != null) {
            focusedGuid = imported
            focusRequesters.getOrPut(imported) { FocusRequester() }.requestFocus()
            mainViewModel.clearLastImportedGuid()
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

    val openDetail: (ServersCache) -> Unit = { server ->
        focusedGuid = server.guid
        detailPrimaryFocus.requestFocus()
    }

    val editServer: (ServersCache) -> Unit = { server ->
        when (server.profile.configType) {
            EConfigType.POLICYGROUP -> onEditPolicyGroup(server.guid)
            EConfigType.PROXYCHAIN -> onEditProxyChain(server.guid)
            else -> onEditServer(
                server.guid,
                server.profile.configType.value,
                isRunning && selectedGuid == server.guid,
            )
        }
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
                activeName = activeServer?.profile?.remarks?.takeIf {
                    isRunning && it.isNotBlank()
                },
                showSpeed = uiState.speedEnabled,
                traffic = uiState.traffic,
                onSearch = { showSearch = true },
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
            }

            ServerListHeader(
                serverCount = servers.size,
                onImport = onImport,
                onAdd = { showTypePicker = true },
                onBatch = { showBatch = true },
            )

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
                } else {
                    // Keep the list and the action pane side by side. The TV
                    // rail leaves enough width even on a 1280x720 panel; the
                    // pane narrows a little instead of falling back to a hidden
                    // action menu.
                    val paneWidth = if (maxWidth >= 700.dp) 320.dp else 280.dp
                    Row(Modifier.fillMaxSize()) {
                        ServerList(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            servers = servers,
                            activeGuid = if (isRunning) selectedGuid else null,
                            focusRequesters = focusRequesters,
                            onOpenDetail = openDetail,
                            onFocused = { focusedGuid = it },
                        )
                        Spacer(Modifier.width(16.dp))
                        DetailPane(
                            modifier = Modifier
                                .width(paneWidth)
                                .fillMaxHeight(),
                            server = detailServer,
                            isActive = detailIsActive,
                            primaryFocus = detailPrimaryFocus,
                            onConnect = { detailServer?.let(connectOrDisconnect) },
                            onTest = {
                                if (detailIsActive) {
                                    mainViewModel.testCurrentServerRealPing()
                                } else {
                                    context.toast(R.string.tv_home_test_requires_connection)
                                }
                            },
                            onEdit = { detailServer?.let(editServer) },
                            onShare = {
                                detailServer?.let {
                                    mainViewModel.onAction(MainAction.ShareQRCode(it.guid))
                                }
                            },
                            onUpdateSubscription = {
                                detailServer?.profile?.subscriptionId
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let {
                                        mainViewModel.onAction(MainAction.UpdateGroupSubscription(it))
                                    }
                            },
                            onDelete = { server -> deleteTarget = server },
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

    if (showBatch) {
        BatchDialog(
            isRunning = isRunning,
            isTesting = isTesting,
            onDismiss = { showBatch = false },
            onAction = { action ->
                showBatch = false
                mainViewModel.onAction(action)
            },
            onRestart = {
                showBatch = false
                onRestartService()
            },
            onBulkDelete = { target ->
                showBatch = false
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
private fun ServerListHeader(
    serverCount: Int,
    onImport: () -> Unit,
    onAdd: () -> Unit,
    onBatch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (serverCount == 1) {
                stringResource(R.string.tv_home_server_count_one)
            } else {
                stringResource(R.string.tv_home_server_count, serverCount)
            },
            style = MaterialTheme.typography.labelLarge,
            color = TvColors.TextMuted,
            modifier = Modifier.weight(1f),
        )
        TvSecondaryButton(
            text = stringResource(R.string.tv_action_import),
            onClick = onImport,
        )
        TvSecondaryButton(
            text = stringResource(R.string.tv_home_add_node),
            onClick = onAdd,
        )
        TvSecondaryButton(
            text = stringResource(R.string.tv_home_batch),
            onClick = onBatch,
        )
    }
}

@Composable
private fun HomeTopBand(
    statusText: String,
    statusColor: Color,
    activeName: String?,
    showSpeed: Boolean,
    traffic: TrafficSample?,
    onSearch: () -> Unit,
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
            text = if (activeName == null) statusText else "$statusText · $activeName",
            style = MaterialTheme.typography.titleSmall,
            color = TvColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showSpeed && traffic != null) {
            SpeedReadout(traffic = traffic)
            Spacer(Modifier.width(12.dp))
        }
        TvIconAction(
            iconRes = R.drawable.ic_search_24dp,
            contentDescription = stringResource(R.string.acc_search),
            onClick = onSearch,
        )
    }
}

@Composable
private fun SpeedReadout(traffic: TrafficSample) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = stringResource(R.string.tv_speed_proxy) + " " + stringResource(
                R.string.tv_speed_value,
                traffic.proxyUp.toSpeedString(),
                traffic.proxyDown.toSpeedString(),
            ),
            style = MaterialTheme.typography.labelLarge,
            color = TvColors.Teal,
            maxLines = 1,
        )
        if (traffic.directUp + traffic.directDown > 0L) {
            Text(
                text = stringResource(R.string.tv_speed_direct) + " " + stringResource(
                    R.string.tv_speed_value,
                    traffic.directUp.toSpeedString(),
                    traffic.directDown.toSpeedString(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = TvColors.TextMuted,
                maxLines = 1,
            )
        }
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
    activeGuid: String?,
    focusRequesters: MutableMap<String, FocusRequester>,
    onOpenDetail: (ServersCache) -> Unit,
    onFocused: (String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(servers, key = { it.guid }) { server ->
            val focusRequester = remember(server.guid) {
                focusRequesters.getOrPut(server.guid) { FocusRequester() }
            }
            ServerRow(
                server = server,
                selected = server.guid == activeGuid,
                focusRequester = focusRequester,
                onClick = { onOpenDetail(server) },
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
                inset = 2.dp,
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
    isActive: Boolean,
    primaryFocus: FocusRequester,
    onConnect: () -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onUpdateSubscription: () -> Unit,
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

        val scrollState = rememberScrollState()
        LaunchedEffect(server.guid) { scrollState.scrollTo(0) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = server.profile.remarks.ifBlank { server.profile.server.orEmpty() },
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildString {
                        append(server.profile.configType.name)
                        val address = listOf(
                            server.profile.server.orEmpty(),
                            server.profile.serverPort.orEmpty()
                        )
                            .filter { it.isNotBlank() }
                            .joinToString(":")
                        if (address.isNotBlank()) {
                            append(" · ")
                            append(address)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isActive) {
                    Text(
                        text = stringResource(R.string.tv_status_connected),
                        style = MaterialTheme.typography.labelSmall,
                        color = TvColors.Success,
                    )
                } else {
                    LatencyBadge(server.testDelayMillis)
                }
            }

            Spacer(Modifier.height(2.dp))

            TvPrimaryButton(
                text = if (isActive) {
                    stringResource(R.string.tv_action_disconnect)
                } else {
                    stringResource(R.string.tv_action_connect)
                },
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(primaryFocus),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TvCompactButton(
                    text = stringResource(R.string.tv_action_test),
                    onClick = onTest,
                    modifier = Modifier.weight(1f),
                )
                TvCompactButton(
                    text = stringResource(R.string.menu_item_edit_config),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TvCompactButton(
                    text = stringResource(R.string.share_method_qrcode),
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                )
                if (server.profile.subscriptionId.isNotBlank()) {
                    TvCompactButton(
                        text = stringResource(R.string.tv_action_update_subscription),
                        onClick = onUpdateSubscription,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            TvCompactButton(
                text = stringResource(R.string.action_delete),
                onClick = { onDelete(server) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive,
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
private fun BatchDialog(
    isRunning: Boolean,
    isTesting: Boolean,
    onDismiss: () -> Unit,
    onAction: (MainAction) -> Unit,
    onRestart: () -> Unit,
    onBulkDelete: (BulkDelete) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.tv_home_batch_title),
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
                    label = stringResource(R.string.title_ping_all_server),
                    iconRes = R.drawable.ic_fab_check,
                    onClick = { onAction(MainAction.TestAllServers) },
                    modifier = Modifier.focusRequester(firstFocus),
                )
                MenuRow(
                    label = stringResource(R.string.title_real_ping_all_server),
                    iconRes = R.drawable.ic_fab_check,
                    onClick = { onAction(MainAction.TestRealAllServers) },
                )
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
                if (isTesting) {
                    MenuRow(
                        label = stringResource(R.string.action_cancel),
                        iconRes = R.drawable.ic_stop_24dp,
                        onClick = { onAction(MainAction.CancelTesting) },
                    )
                }
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
