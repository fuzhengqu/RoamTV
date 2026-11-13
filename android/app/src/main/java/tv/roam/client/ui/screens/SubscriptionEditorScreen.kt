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
import tv.roam.client.feature.subscription.SubscriptionEditorAction
import tv.roam.client.feature.subscription.SubscriptionEditorState
import tv.roam.client.feature.subscription.SubscriptionEditorViewModel
import tv.roam.client.feature.subscription.SubscriptionField
import tv.roam.client.feature.subscription.SubscriptionFieldKind
import tv.roam.client.ui.components.PairingDialog
import tv.roam.client.ui.components.TvConfirmDialog
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvOptionsDialog
import tv.roam.client.ui.components.TvPrimaryButton
import tv.roam.client.ui.components.TvSecondaryButton
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTextEditDialog
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.navigation.TvRoute
import tv.roam.client.ui.theme.TvColors

@Composable
fun SubscriptionEditorScreen(
    route: TvRoute.SubscriptionEditor,
    viewModel: SubscriptionEditorViewModel,
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
        SubscriptionField.entries.associateWith { FocusRequester() }.toMutableMap()
    }
    var editingField by remember { mutableStateOf<SubscriptionField?>(null) }
    var editSeed by remember { mutableStateOf("") }
    var dropdownField by remember { mutableStateOf<SubscriptionField?>(null) }
    var pairingField by remember { mutableStateOf<SubscriptionField?>(null) }
    var showPairing by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var lastField by remember { mutableStateOf<SubscriptionField?>(null) }

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

    val startPairing: (SubscriptionField) -> Unit = { field ->
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
        viewModel.onAction(SubscriptionEditorAction.Load(route.guid))
    }
    LaunchedEffect(state.savedGuid) {
        val guid = state.savedGuid ?: return@LaunchedEffect
        viewModel.onAction(SubscriptionEditorAction.ConsumeSaved)
        if (guid.isNotEmpty()) onSaved()
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.onAction(SubscriptionEditorAction.ConsumeDeleted)
            onDeleted()
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            SubscriptionField.entries.firstOrNull()?.let { fieldFocus[it]?.requestFocus() }
        }
    }
    LaunchedEffect(state.validationAttempt) {
        if (state.validationAttempt > 0) {
            state.errors.keys.firstOrNull()?.let { field ->
                fieldFocus[field]?.requestFocus()
            }
        }
    }
    LaunchedEffect(editingField, dropdownField, showPairing) {
        if (editingField == null && dropdownField == null && !showPairing) {
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
            viewModel.onAction(SubscriptionEditorAction.ConsumeGeneralError)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.title_sub_setting),
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
                SubscriptionForm(
                    state = state,
                    fieldFocus = fieldFocus,
                    modifier = Modifier.weight(1f),
                    onEdit = { field ->
                        lastField = field
                        editSeed = state.value(field)
                        editingField = field
                    },
                    onDropdown = { field ->
                        lastField = field
                        dropdownField = field
                    },
                    onToggle = { field ->
                        lastField = field
                        viewModel.onAction(
                            SubscriptionEditorAction.SetSwitch(field, !state.isOn(field))
                        )
                    },
                    onSave = { viewModel.onAction(SubscriptionEditorAction.Save) },
                    onDelete = { showDelete = true },
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

    editingField?.let { field ->
        TvTextEditDialog(
            title = stringResource(field.labelRes),
            initialValue = editSeed,
            multiline = field.kind == SubscriptionFieldKind.Multiline,
            numberOnly = field.kind == SubscriptionFieldKind.Number,
            onSave = { value ->
                viewModel.onAction(SubscriptionEditorAction.SetValue(field, value))
                editingField = null
            },
            onFromPhone = if (field == SubscriptionField.Url ||
                field == SubscriptionField.Headers
            ) {
                { startPairing(field) }
            } else {
                null
            },
            onDismiss = { editingField = null },
        )
    }

    dropdownField?.let { field ->
        val options = listOf("") + state.profiles
        TvOptionsDialog(
            title = stringResource(field.labelRes),
            options = options,
            selectedIndex = options.indexOf(state.value(field)).coerceAtLeast(0),
            onSelectIndex = { index ->
                viewModel.onAction(SubscriptionEditorAction.SetValue(field, options[index]))
                dropdownField = null
            },
            onDismiss = { dropdownField = null },
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
            message = stringResource(R.string.confirm_delete_profile),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showDelete = false
                viewModel.onAction(SubscriptionEditorAction.Delete)
            },
            onDismiss = { showDelete = false },
            danger = true,
        )
    }
}

@Composable
private fun SubscriptionForm(
    state: SubscriptionEditorState,
    fieldFocus: MutableMap<SubscriptionField, FocusRequester>,
    modifier: Modifier = Modifier,
    onEdit: (SubscriptionField) -> Unit,
    onDropdown: (SubscriptionField) -> Unit,
    onToggle: (SubscriptionField) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SubscriptionField.entries.forEach { field ->
            val requester = fieldFocus.getValue(field)
            val value = state.value(field)
            val display = when {
                field.kind == SubscriptionFieldKind.Switch ->
                    stringResource(
                        if (state.isOn(field)) R.string.tv_editor_on else R.string.tv_editor_off
                    )

                field == SubscriptionField.PrevProfile || field == SubscriptionField.NextProfile ->
                    value.ifEmpty { stringResource(R.string.tv_editor_none) }

                value.isEmpty() -> stringResource(R.string.tv_editor_not_set)
                else -> value
            }
            Column {
                TvSettingsRow(
                    label = stringResource(field.labelRes),
                    value = display,
                    iconRes = when (field.kind) {
                        SubscriptionFieldKind.Switch -> R.drawable.ic_qu_switch_24dp
                        SubscriptionFieldKind.Dropdown -> R.drawable.ic_expand_more_24dp
                        SubscriptionFieldKind.Multiline -> R.drawable.ic_description_24dp
                        else -> R.drawable.ic_edit_24dp
                    },
                    onClick = {
                        when (field.kind) {
                            SubscriptionFieldKind.Switch -> onToggle(field)
                            SubscriptionFieldKind.Dropdown -> onDropdown(field)
                            else -> onEdit(field)
                        }
                    },
                    modifier = Modifier.focusRequester(requester),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvPrimaryButton(
                text = stringResource(R.string.tv_editor_save),
                onClick = onSave,
                enabled = !state.isSaving,
            )
            if (!state.isNew) {
                TvSecondaryButton(
                    text = stringResource(R.string.action_delete),
                    onClick = onDelete,
                )
            }
        }
    }
}
