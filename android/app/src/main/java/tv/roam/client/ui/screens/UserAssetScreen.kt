package tv.roam.client.ui.screens

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import tv.roam.client.AppConfig
import tv.roam.client.R
import tv.roam.client.dto.entities.AssetUrlCache
import tv.roam.client.extension.toast
import tv.roam.client.feature.userasset.AssetFileMetadata
import tv.roam.client.feature.userasset.UserAssetViewModel
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvIconAction
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvOptionsDialog
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun UserAssetScreen(
    viewModel: UserAssetViewModel,
    onAddUrl: () -> Unit,
    onEditAsset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    val rowFocus = remember { mutableMapOf<String, FocusRequester>() }
    var focusedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showSource by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFile(uri)
    }

    LaunchedEffect(Unit) {
        viewModel.load()
    }
    LaunchedEffect(state.messageRes) {
        val message = state.messageRes ?: return@LaunchedEffect
        context.toast(message)
        viewModel.consumeMessage()
    }
    LaunchedEffect(state.downloadResult) {
        val result = state.downloadResult ?: return@LaunchedEffect
        if (result.successCount > 0) {
            val quantity = context.resources.getQuantityString(
                R.plurals.title_update_asset_count,
                result.successCount,
                result.successCount,
            )
            context.toast(quantity)
        } else {
            context.toast(R.string.toast_failure)
        }
        viewModel.consumeDownloadResult()
    }
    LaunchedEffect(state.assets.map { it.guid }, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        val editable = state.assets.filter { it.assetUrl.locked != true }
        val target = focusedId?.takeIf { id -> editable.any { it.guid == id } }
            ?: editable.firstOrNull()?.guid
        if (target != null) {
            focusedId = target
            rowFocus[target]?.requestFocus()
        } else {
            firstFocus.requestFocus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.title_user_asset_setting),
                actions = {
                    TvIconAction(
                        iconRes = R.drawable.ic_add_24dp,
                        contentDescription = stringResource(R.string.acc_add_asset),
                        onClick = { showAddMenu = true },
                    )
                    TvIconAction(
                        iconRes = R.drawable.ic_cloud_download_24dp,
                        contentDescription = stringResource(R.string.acc_download_file),
                        onClick = { viewModel.downloadAll() },
                    )
                },
            )
            if (state.isDownloading) {
                Text(
                    text = stringResource(R.string.msg_downloading_content),
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item(key = "geo_source") {
                        TvSettingsRow(
                            label = stringResource(R.string.asset_geo_files_sources),
                            value = state.geoFilesSource,
                            iconRes = R.drawable.ic_routing_24dp,
                            onClick = { showSource = true },
                            modifier = Modifier.focusRequester(firstFocus),
                        )
                    }
                    items(state.assets, key = { it.guid }) { asset ->
                        val requester = remember(asset.guid) { FocusRequester() }
                        SideEffect { rowFocus[asset.guid] = requester }
                        AssetRow(
                            asset = asset,
                            metadata = state.fileMetadata[asset.guid],
                            onClick = if (asset.assetUrl.locked == true) {
                                null
                            } else {
                                { onEditAsset(asset.guid) }
                            },
                            modifier = Modifier
                                .focusRequester(requester)
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) focusedId = asset.guid
                                },
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

    if (showAddMenu) {
        AddAssetDialog(
            onAddUrl = {
                showAddMenu = false
                onAddUrl()
            },
            onAddFile = {
                showAddMenu = false
                runCatching { filePicker.launch(arrayOf("*/*")) }
                    .onFailure { context.toast(R.string.toast_asset_copy_failed) }
            },
            onDismiss = { showAddMenu = false },
        )
    }

    if (showSource) {
        TvOptionsDialog(
            title = stringResource(R.string.asset_geo_files_sources),
            options = AppConfig.GEO_FILES_SOURCES,
            selectedIndex = AppConfig.GEO_FILES_SOURCES.indexOf(state.geoFilesSource),
            onSelectIndex = { index ->
                showSource = false
                AppConfig.GEO_FILES_SOURCES.getOrNull(index)?.let(viewModel::selectGeoSource)
            },
            onDismiss = { showSource = false },
        )
    }
}

@Composable
private fun AddAssetDialog(
    onAddUrl: () -> Unit,
    onAddFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }
    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.acc_add_asset),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            TvSettingsRow(
                label = stringResource(R.string.title_user_asset_add_url),
                value = "",
                iconRes = R.drawable.ic_description_24dp,
                onClick = onAddUrl,
                modifier = Modifier.focusRequester(firstFocus),
            )
            TvSettingsRow(
                label = stringResource(R.string.tv_asset_add_file),
                value = "",
                iconRes = R.drawable.ic_file_24dp,
                onClick = onAddFile,
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

@Composable
private fun AssetRow(
    asset: AssetUrlCache,
    metadata: AssetFileMetadata?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val rowModifier = modifier
        .fillMaxWidth()
        .height(72.dp)
    if (onClick == null) {
        Surface(
            modifier = rowModifier,
            shape = shape,
            colors = SurfaceDefaults.colors(
                containerColor = TvColors.Surface,
                contentColor = TvColors.Text,
            ),
        ) {
            AssetRowContent(asset = asset, metadata = metadata)
        }
    } else {
        Surface(
            onClick = onClick,
            modifier = rowModifier,
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(3.dp, TvColors.Teal),
                    shape = shape,
                )
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvColors.Surface,
                contentColor = TvColors.Text,
                focusedContainerColor = TvColors.SurfaceFocused,
                focusedContentColor = TvColors.Text,
            ),
        ) {
            AssetRowContent(asset = asset, metadata = metadata)
        }
    }
}

@Composable
private fun AssetRowContent(
    asset: AssetUrlCache,
    metadata: AssetFileMetadata?,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val use24Hour = DateFormat.is24HourFormat(context)
    val missingText = stringResource(R.string.msg_file_not_found)
    val metadataText = remember(metadata, locale, use24Hour, missingText) {
        if (metadata == null) {
            missingText
        } else {
            val size = Formatter.formatFileSize(context, metadata.length)
            val skeleton = if (use24Hour) "yMMMdHm" else "yMMMdhm"
            val date = SimpleDateFormat(
                DateFormat.getBestDateTimePattern(locale, skeleton),
                locale,
            ).format(Date(metadata.lastModified))
            "$size · $date"
        }
    }
    val locked = asset.assetUrl.locked == true
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(
                when {
                    locked -> R.drawable.ic_lock_24dp
                    asset.assetUrl.url == UserAssetViewModel.FILE_ASSET_URL -> R.drawable.ic_file_24dp
                    else -> R.drawable.ic_description_24dp
                }
            ),
            contentDescription = null,
            tint = TvColors.Teal,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = asset.assetUrl.remarks,
                style = MaterialTheme.typography.titleSmall,
                color = TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (locked) {
                    "$metadataText · ${stringResource(R.string.tv_asset_builtin)}"
                } else {
                    metadataText
                },
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!locked) {
            Icon(
                painter = painterResource(R.drawable.ic_edit_24dp),
                contentDescription = null,
                tint = TvColors.TextMuted,
            )
        }
    }
}
