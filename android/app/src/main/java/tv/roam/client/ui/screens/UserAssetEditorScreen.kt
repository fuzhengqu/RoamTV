package tv.roam.client.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.feature.pairing.PairingViewModel
import tv.roam.client.feature.userasset.AssetEditorField
import tv.roam.client.feature.userasset.UserAssetEditorAction
import tv.roam.client.feature.userasset.UserAssetEditorState
import tv.roam.client.feature.userasset.UserAssetEditorViewModel
import tv.roam.client.ui.components.PairingDialog
import tv.roam.client.ui.components.TvConfirmDialog
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvPrimaryButton
import tv.roam.client.ui.components.TvSecondaryButton
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTextEditDialog
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.navigation.TvRoute
import tv.roam.client.ui.theme.TvColors

@Composable
fun UserAssetEditorScreen(
    route: TvRoute.AssetEditor,
    viewModel: UserAssetEditorViewModel,
    pairingViewModel: PairingViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pairingState by pairingViewModel.uiState.collectAsStateWithLifecycle()
    val fieldFocus = remember {
        AssetEditorField.entries.associateWith { FocusRequester() }.toMutableMap()
    }
    val deleteFocus = remember { FocusRequester() }
    var editingField by remember { mutableStateOf<AssetEditorField?>(null) }
    var editSeed by remember { mutableStateOf("") }
    var pairingField by remember { mutableStateOf<AssetEditorField?>(null) }
    var showPairing by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var lastField by remember { mutableStateOf<AssetEditorField?>(null) }

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

    val startPairing: (AssetEditorField) -> Unit = { field ->
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

    LaunchedEffect(route.key) {
        viewModel.onAction(UserAssetEditorAction.Load(route.guid))
    }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onAction(UserAssetEditorAction.ConsumeSaved)
            onSaved()
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.onAction(UserAssetEditorAction.ConsumeDeleted)
            onDeleted()
        }
    }
    LaunchedEffect(state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        if (state.isFileAsset) {
            deleteFocus.requestFocus()
        } else {
            fieldFocus[AssetEditorField.Remarks]?.requestFocus()
        }
    }
    LaunchedEffect(state.errors) {
        state.errors.keys.firstOrNull()?.let { field -> fieldFocus[field]?.requestFocus() }
    }
    LaunchedEffect(editingField, showPairing) {
        if (editingField == null && !showPairing) {
            lastField?.let { fieldFocus[it]?.requestFocus() }
        }
    }
    LaunchedEffect(Unit) {
        pairingViewModel.received.collect { text ->
            val field = pairingField
            showPairing = false
            pairingField = null
            if (field != null) {
                editSeed = text
                editingField = field
                lastField = field
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { pairingViewModel.stop() }
    }
    state.generalError?.let { errorRes ->
        LaunchedEffect(errorRes) {
            context.toast(errorRes)
            viewModel.onAction(UserAssetEditorAction.ConsumeError)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(
                    when {
                        state.isFileAsset -> R.string.tv_asset_local_file
                        state.isNew -> R.string.title_user_asset_add_url
                        else -> R.string.tv_asset_edit_title
                    }
                ),
                onBack = onBack,
            )
            if (state.isLoading) {
                Text(
                    text = stringResource(R.string.tv_editor_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (state.isFileAsset) {
                        AssetInfoBlock(
                            label = stringResource(R.string.sub_setting_remarks),
                            value = state.remarks,
                        )
                        AssetInfoBlock(
                            label = stringResource(R.string.tv_asset_local_file),
                            value = stringResource(R.string.tv_asset_local_file_hint),
                        )
                    } else {
                        EditorFieldRow(
                            field = AssetEditorField.Remarks,
                            state = state,
                            focusRequester = fieldFocus.getValue(AssetEditorField.Remarks),
                            onEdit = { field ->
                                lastField = field
                                editSeed = state.remarks
                                editingField = field
                            },
                        )
                        EditorFieldRow(
                            field = AssetEditorField.Url,
                            state = state,
                            focusRequester = fieldFocus.getValue(AssetEditorField.Url),
                            onEdit = { field ->
                                lastField = field
                                editSeed = state.url
                                editingField = field
                            },
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!state.isFileAsset) {
                            TvPrimaryButton(
                                text = stringResource(R.string.tv_editor_save),
                                onClick = { viewModel.onAction(UserAssetEditorAction.Save) },
                            )
                        }
                        if (!state.isNew) {
                            TvSecondaryButton(
                                text = stringResource(R.string.action_delete),
                                onClick = { showDelete = true },
                                modifier = Modifier.focusRequester(deleteFocus),
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

    editingField?.let { field ->
        TvTextEditDialog(
            title = stringResource(
                if (field == AssetEditorField.Remarks) R.string.sub_setting_remarks else R.string.title_url
            ),
            initialValue = editSeed,
            multiline = field == AssetEditorField.Url,
            onSave = { value ->
                viewModel.onAction(UserAssetEditorAction.SetValue(field, value))
                editingField = null
            },
            onFromPhone = if (field == AssetEditorField.Url) {
                { startPairing(field) }
            } else {
                null
            },
            onDismiss = { editingField = null },
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

    if (showDelete) {
        TvConfirmDialog(
            title = stringResource(R.string.action_delete),
            message = stringResource(R.string.confirm_delete_asset_file),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showDelete = false
                viewModel.onAction(UserAssetEditorAction.Delete)
            },
            onDismiss = { showDelete = false },
            danger = true,
        )
    }
}

@Composable
private fun EditorFieldRow(
    field: AssetEditorField,
    state: UserAssetEditorState,
    focusRequester: FocusRequester,
    onEdit: (AssetEditorField) -> Unit,
) {
    val value = if (field == AssetEditorField.Remarks) state.remarks else state.url
    Column {
        TvSettingsRow(
            label = stringResource(
                if (field == AssetEditorField.Remarks) R.string.sub_setting_remarks else R.string.title_url
            ),
            value = value.ifEmpty { stringResource(R.string.tv_editor_not_set) },
            iconRes = if (field == AssetEditorField.Remarks) {
                R.drawable.ic_edit_24dp
            } else {
                R.drawable.ic_description_24dp
            },
            onClick = { onEdit(field) },
            modifier = Modifier.focusRequester(focusRequester),
        )
        state.errors[field]?.let { errorRes ->
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.Error,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
            )
        }
    }
}

@Composable
private fun AssetInfoBlock(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TvColors.TextMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TvColors.Text,
        )
    }
}
