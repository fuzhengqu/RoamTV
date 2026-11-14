package tv.roam.client.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.feature.logcat.LogcatEntry
import tv.roam.client.feature.logcat.LogcatViewModel
import tv.roam.client.ui.components.TvConfirmDialog
import tv.roam.client.ui.components.TvIconAction
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvTextEditDialog
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors
import tv.roam.client.util.Utils

@Composable
fun LogcatScreen(
    viewModel: LogcatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val logs by viewModel.logEntries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    var showSearch by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        searchQuery = ""
        viewModel.filter("")
        viewModel.loadLogcat()
    }
    LaunchedEffect(logs.firstOrNull()?.key) {
        if (logs.isNotEmpty()) firstFocus.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.title_logcat),
                onBack = onBack,
                actions = {
                    TvIconAction(
                        iconRes = R.drawable.ic_search_24dp,
                        contentDescription = stringResource(R.string.acc_search),
                        onClick = { showSearch = true },
                    )
                    TvIconAction(
                        iconRes = R.drawable.ic_copy,
                        contentDescription = stringResource(R.string.acc_copy_log),
                        onClick = { viewModel.copyLogcat() },
                    )
                    TvIconAction(
                        iconRes = R.drawable.ic_restore_24dp,
                        contentDescription = stringResource(R.string.acc_refresh),
                        onClick = { viewModel.loadLogcat() },
                    )
                    TvIconAction(
                        iconRes = R.drawable.ic_delete_24dp,
                        contentDescription = stringResource(R.string.acc_clear_log),
                        onClick = { showClear = true },
                    )
                },
            )
            if (searchQuery.isNotEmpty()) {
                Text(
                    text = searchQuery,
                    style = MaterialTheme.typography.labelLarge,
                    color = TvColors.Amber,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            if (isLoading) {
                Text(
                    text = stringResource(R.string.tv_editor_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.Amber,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (!isLoading && logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.tv_logcat_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextMuted,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(logs, key = { _, entry -> entry.key }) { index, entry ->
                        LogcatRow(
                            entry = entry,
                            modifier = if (index == 0) {
                                Modifier.focusRequester(firstFocus)
                            } else {
                                Modifier
                            },
                            onClick = {
                                Utils.setClipboard(context, entry.text)
                                context.toast(R.string.toast_success)
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

    if (showSearch) {
        TvTextEditDialog(
            title = stringResource(R.string.menu_item_search),
            initialValue = searchQuery,
            onSave = { value ->
                searchQuery = value.trim()
                viewModel.filter(searchQuery)
                showSearch = false
            },
            onDismiss = { showSearch = false },
        )
    }

    if (showClear) {
        TvConfirmDialog(
            title = stringResource(R.string.acc_clear_log),
            message = stringResource(R.string.tv_logcat_clear_message),
            confirmText = stringResource(R.string.logcat_clear),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showClear = false
                viewModel.clearLogcat()
            },
            onDismiss = { showClear = false },
            danger = true,
        )
    }
}

@Composable
private fun LogcatRow(
    entry: LogcatEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
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
        Text(
            text = entry.text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
            color = TvColors.TextMuted,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
