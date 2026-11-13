package tv.roam.client.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.feature.main.MainAction
import tv.roam.client.feature.main.MainViewModel
import tv.roam.client.feature.pairing.PairingViewModel
import tv.roam.client.ui.components.PairingDialog
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvPrimaryButton
import tv.roam.client.ui.components.TvSecondaryButton
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors
import tv.roam.client.util.Utils

/**
 * Import hub. The phone pairing path is the primary television input method;
 * clipboard, document picker and a paste dialog remain available as fallbacks.
 */
@Composable
fun ImportScreen(
    mainViewModel: MainViewModel,
    pairingViewModel: PairingViewModel,
    onBack: () -> Unit,
    onImported: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pairingState by pairingViewModel.uiState.collectAsStateWithLifecycle()
    var showPairing by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    val initialFocus = remember { FocusRequester() }

    val importText: (String) -> Unit = { text ->
        if (text.isBlank()) {
            context.toast(R.string.tv_import_empty)
        } else {
            mainViewModel.onAction(MainAction.ImportBatchConfig(text))
            onImported()
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                    }.getOrNull()
                }
                if (text.isNullOrBlank() || text.length > MAX_IMPORT_TEXT_LENGTH) {
                    context.toast(R.string.tv_import_read_error)
                } else {
                    importText(text)
                }
            }
        }
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

    val startPairing: () -> Unit = {
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
        pairingViewModel.received.collect { text ->
            showPairing = false
            importText(text)
        }
    }

    DisposableEffect(Unit) {
        onDispose { pairingViewModel.stop() }
    }

    LaunchedEffect(Unit) { initialFocus.requestFocus() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.tv_action_import),
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.tv_import_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TvSettingsRow(
                    label = stringResource(R.string.tv_import_from_phone),
                    value = stringResource(R.string.tv_import_recommended),
                    iconRes = R.drawable.ic_scan_24dp,
                    onClick = startPairing,
                    modifier = Modifier.focusRequester(initialFocus),
                )
                TvSettingsRow(
                    label = stringResource(R.string.menu_item_import_config_clipboard),
                    value = "",
                    iconRes = R.drawable.ic_copy,
                    onClick = {
                        scope.launch {
                            val text = withContext(Dispatchers.IO) {
                                Utils.getClipboard(context)
                            }
                            importText(text)
                        }
                    },
                )
                TvSettingsRow(
                    label = stringResource(R.string.menu_item_import_config_local),
                    value = "",
                    iconRes = R.drawable.ic_file_24dp,
                    onClick = {
                        fileLauncher.launch(
                            arrayOf("text/*", "application/json", "application/octet-stream")
                        )
                    },
                )
                TvSettingsRow(
                    label = stringResource(R.string.tv_import_text_title),
                    value = "",
                    iconRes = R.drawable.ic_add_24dp,
                    onClick = { showTextDialog = true },
                )
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

    if (showPairing) {
        PairingDialog(
            state = pairingState,
            onCancel = {
                pairingViewModel.stop()
                showPairing = false
            },
        )
    }

    if (showTextDialog) {
        ImportTextDialog(
            onImport = { text ->
                showTextDialog = false
                importText(text)
            },
            onDismiss = { showTextDialog = false },
        )
    }
}

private const val MAX_IMPORT_TEXT_LENGTH = 1_000_000

@Composable
private fun ImportTextDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.tv_import_text_title),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(
                    color = TvColors.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                ),
                cursorBrush = SolidColor(TvColors.Teal),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onImport(text) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .focusRequester(focus)
                    .background(TvColors.SurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tv_import_text_hint),
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
                    text = stringResource(R.string.tv_action_import),
                    onClick = { onImport(text) },
                )
            }
        }
    }
}
