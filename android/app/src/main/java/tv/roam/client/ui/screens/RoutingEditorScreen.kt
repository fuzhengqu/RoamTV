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
import tv.roam.client.feature.routing.RoutingEditorAction
import tv.roam.client.feature.routing.RoutingEditorState
import tv.roam.client.feature.routing.RoutingEditorViewModel
import tv.roam.client.feature.routing.RoutingField
import tv.roam.client.feature.routing.RoutingFieldKind
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
fun RoutingEditorScreen(
    route: TvRoute.RoutingEditor,
    viewModel: RoutingEditorViewModel,
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
        RoutingField.entries.associateWith { FocusRequester() }.toMutableMap()
    }
    var editingField by remember { mutableStateOf<RoutingField?>(null) }
    var editSeed by remember { mutableStateOf("") }
    var dropdownField by remember { mutableStateOf<RoutingField?>(null) }
    var pairingField by remember { mutableStateOf<RoutingField?>(null) }
    var showPairing by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var lastField by remember { mutableStateOf<RoutingField?>(null) }

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

    val startPairing: (RoutingField) -> Unit = { field ->
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
        viewModel.onAction(RoutingEditorAction.Load(route.ruleId))
    }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onAction(RoutingEditorAction.ConsumeSaved)
            onSaved()
        }
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.onAction(RoutingEditorAction.ConsumeDeleted)
            onDeleted()
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            RoutingField.entries.firstOrNull()?.let { fieldFocus[it]?.requestFocus() }
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
            viewModel.onAction(RoutingEditorAction.ConsumeGeneralError)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.routing_settings_rule_title),
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
                RoutingEditorForm(
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
                            RoutingEditorAction.SetSwitch(field, !state.isOn(field))
                        )
                    },
                    onSave = { viewModel.onAction(RoutingEditorAction.Save) },
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
            multiline = field == RoutingField.Domain ||
                field == RoutingField.Ip ||
                field == RoutingField.Process ||
                field == RoutingField.Protocol,
            onSave = { value ->
                viewModel.onAction(RoutingEditorAction.SetValue(field, value))
                editingField = null
            },
            onFromPhone = if (field == RoutingField.Domain ||
                field == RoutingField.Ip ||
                field == RoutingField.Process ||
                field == RoutingField.Protocol
            ) {
                { startPairing(field) }
            } else {
                null
            },
            onDismiss = { editingField = null },
        )
    }

    dropdownField?.let { field ->
        val options = when (field) {
            RoutingField.Network -> listOf("tcp", "udp", "tcp,udp")
            RoutingField.Outbound -> {
                val current = state.value(field)
                if (current.isNotBlank() && current !in state.outboundOptions) {
                    state.outboundOptions + current
                } else {
                    state.outboundOptions
                }
            }

            else -> emptyList()
        }
        if (options.isNotEmpty()) {
            TvOptionsDialog(
                title = stringResource(field.labelRes),
                options = options,
                selectedIndex = options.indexOf(state.value(field)),
                onSelectIndex = { index ->
                    viewModel.onAction(RoutingEditorAction.SetValue(field, options[index]))
                    dropdownField = null
                },
                onDismiss = { dropdownField = null },
            )
        }
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
            message = stringResource(R.string.confirm_delete_routing_rule),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showDelete = false
                viewModel.onAction(RoutingEditorAction.Delete)
            },
            onDismiss = { showDelete = false },
            danger = true,
        )
    }
}

@Composable
private fun RoutingEditorForm(
    state: RoutingEditorState,
    fieldFocus: MutableMap<RoutingField, FocusRequester>,
    modifier: Modifier = Modifier,
    onEdit: (RoutingField) -> Unit,
    onDropdown: (RoutingField) -> Unit,
    onToggle: (RoutingField) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val fields = listOf(
        RoutingField.Remarks,
        RoutingField.Enabled,
        RoutingField.Locked,
        RoutingField.Domain,
        RoutingField.Ip,
        RoutingField.Process,
        RoutingField.Port,
        RoutingField.Protocol,
        RoutingField.Network,
        RoutingField.Outbound,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        fields.forEach { field ->
            val requester = fieldFocus.getValue(field)
            val value = state.value(field)
            val display = when {
                field == RoutingField.Enabled && state.isOn(field) ->
                    stringResource(R.string.tv_editor_on)

                field == RoutingField.Enabled ->
                    stringResource(R.string.tv_editor_off)

                field == RoutingField.Locked && state.isOn(field) ->
                    stringResource(R.string.tv_editor_on)

                field == RoutingField.Locked ->
                    stringResource(R.string.tv_editor_off)

                value.isEmpty() -> stringResource(R.string.tv_editor_not_set)
                else -> value
            }
            Column {
                TvSettingsRow(
                    label = stringResource(
                        if (field == RoutingField.Enabled) R.string.tv_routing_enabled
                        else field.labelRes
                    ),
                    value = display,
                    iconRes = when (field.kind) {
                        RoutingFieldKind.Switch -> R.drawable.ic_qu_switch_24dp
                        RoutingFieldKind.Dropdown -> R.drawable.ic_expand_more_24dp
                        else -> R.drawable.ic_edit_24dp
                    },
                    onClick = {
                        when (field.kind) {
                            RoutingFieldKind.Switch -> onToggle(field)
                            RoutingFieldKind.Dropdown -> onDropdown(field)
                            else -> onEdit(field)
                        }
                    },
                    modifier = Modifier.focusRequester(requester),
                )
                if (field == RoutingField.Locked) {
                    Text(
                        text = stringResource(R.string.routing_settings_tips),
                        style = MaterialTheme.typography.bodySmall,
                        color = TvColors.TextMuted,
                        modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                    )
                }
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
