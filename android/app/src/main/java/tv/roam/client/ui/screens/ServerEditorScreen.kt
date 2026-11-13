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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.feature.pairing.PairingViewModel
import tv.roam.client.feature.server.ServerEditorAction
import tv.roam.client.feature.server.ServerEditorDefinition
import tv.roam.client.feature.server.ServerEditorMapper
import tv.roam.client.feature.server.ServerEditorState
import tv.roam.client.feature.server.ServerEditorViewModel
import tv.roam.client.feature.server.ServerField
import tv.roam.client.feature.server.ServerFieldKind
import tv.roam.client.enums.EConfigType
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

/**
 * Remote-first server editor. Every value is a focusable row; OK opens a large
 * edit dialog, a dropdown, or a toggle. Long values can be sent from a phone
 * through the same encrypted pairing session used by Import.
 */
@Composable
fun ServerEditorScreen(
    route: TvRoute.ServerEditor,
    viewModel: ServerEditorViewModel,
    pairingViewModel: PairingViewModel,
    onBack: () -> Unit,
    onSaved: (guid: String, restart: Boolean) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pairingState by pairingViewModel.uiState.collectAsStateWithLifecycle()
    val fieldFocus = remember {
        ServerField.entries.associateWith { FocusRequester() }.toMutableMap()
    }
    var editingField by remember { mutableStateOf<ServerField?>(null) }
    var editSeed by remember { mutableStateOf("") }
    var dropdownField by remember { mutableStateOf<ServerField?>(null) }
    var pairingField by remember { mutableStateOf<ServerField?>(null) }
    var showPairing by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var lastField by remember { mutableStateOf<ServerField?>(null) }

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

    val startPairing: (ServerField) -> Unit = { field ->
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
        viewModel.onAction(
            ServerEditorAction.Load(
                guid = route.guid,
                configType = EConfigType.fromInt(route.configType) ?: EConfigType.VMESS,
                restartOnSave = route.restartOnSave,
            )
        )
    }

    LaunchedEffect(state.savedGuid) {
        val savedGuid = state.savedGuid ?: return@LaunchedEffect
        viewModel.onAction(ServerEditorAction.ConsumeSaved)
        onSaved(savedGuid, state.restartRequired)
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.onAction(ServerEditorAction.ConsumeDeleted)
            onDeleted()
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

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            ServerEditorDefinition.visibleFields(state)
                .firstOrNull()
                ?.let { fieldFocus[it]?.requestFocus() }
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
                    if (state.isNew) R.string.menu_item_add_config
                    else R.string.menu_item_edit_config
                ) + " · " + protocolName(state.configType),
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
                EditorForm(
                    state = state,
                    fieldFocus = fieldFocus,
                    modifier = Modifier.weight(1f),
                    onEditText = { field ->
                        lastField = field
                        editSeed = state.value(field)
                        editingField = field
                    },
                    onOpenDropdown = { field ->
                        lastField = field
                        dropdownField = field
                    },
                    onToggle = { field ->
                        lastField = field
                        viewModel.onAction(
                            ServerEditorAction.SetSwitch(field, !state.isOn(field))
                        )
                    },
                    onSave = { viewModel.onAction(ServerEditorAction.Save) },
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
            title = stringResource(
                ServerEditorMapper.labelRes(field, state.configType, state.values)
            ),
            initialValue = editSeed,
            multiline = field.kind == ServerFieldKind.Multiline,
            numberOnly = field.kind == ServerFieldKind.Number,
            onSave = { value ->
                viewModel.onAction(ServerEditorAction.SetValue(field, value))
                editingField = null
            },
            onFromPhone = { startPairing(field) },
            onDismiss = { editingField = null },
        )
    }

    dropdownField?.let { field ->
        val optionsRes = ServerEditorMapper.optionsRes(field, state.configType, state.values)
        if (optionsRes != null) {
            val options = stringArrayResource(optionsRes).toList()
            TvOptionsDialog(
                title = stringResource(
                    ServerEditorMapper.labelRes(field, state.configType, state.values)
                ),
                options = options,
                selectedIndex = options.indexOf(state.value(field)),
                onSelectIndex = { index ->
                    viewModel.onAction(ServerEditorAction.SetValue(field, options[index]))
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
            message = stringResource(R.string.confirm_delete_profile),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showDelete = false
                viewModel.onAction(ServerEditorAction.Delete)
            },
            onDismiss = { showDelete = false },
            danger = true,
        )
    }

    state.generalError?.let { errorRes ->
        LaunchedEffect(errorRes) {
            context.toast(errorRes)
            viewModel.onAction(ServerEditorAction.ConsumeGeneralError)
        }
    }
}

@Composable
private fun EditorForm(
    state: ServerEditorState,
    fieldFocus: MutableMap<ServerField, FocusRequester>,
    modifier: Modifier = Modifier,
    onEditText: (ServerField) -> Unit,
    onOpenDropdown: (ServerField) -> Unit,
    onToggle: (ServerField) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val fields = ServerEditorDefinition.visibleFields(state)
    val sections = fields.groupBy(ServerEditorDefinition::sectionOf)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sections.forEach { (section, sectionFields) ->
            Text(
                text = stringResource(section.titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.Teal,
                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
            )
            sectionFields.forEach { field ->
                val requester = fieldFocus.getValue(field)
                val value = state.value(field)
                val errorRes = state.errors[field]
                val display = when {
                    field.kind == ServerFieldKind.Switch ->
                        stringResource(
                            if (state.isOn(field)) R.string.tv_editor_on else R.string.tv_editor_off
                        )

                    field.secret && value.isNotEmpty() -> "••••••"
                    value.isEmpty() -> stringResource(R.string.tv_editor_not_set)
                    else -> value
                }
                Column {
                    TvSettingsRow(
                        label = stringResource(
                            ServerEditorMapper.labelRes(field, state.configType, state.values)
                        ),
                        value = display,
                        iconRes = when (field.kind) {
                            ServerFieldKind.Switch -> R.drawable.ic_qu_switch_24dp
                            ServerFieldKind.Dropdown -> R.drawable.ic_expand_more_24dp
                            ServerFieldKind.Multiline -> R.drawable.ic_description_24dp
                            else -> R.drawable.ic_edit_24dp
                        },
                        onClick = {
                            when (field.kind) {
                                ServerFieldKind.Switch -> onToggle(field)
                                ServerFieldKind.Dropdown -> onOpenDropdown(field)
                                else -> onEditText(field)
                            }
                        },
                        modifier = Modifier
                            .focusRequester(requester),
                    )
                    if (errorRes != null) {
                        Text(
                            text = stringResource(errorRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.Error,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                        )
                    }
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

@Composable
private fun protocolName(type: EConfigType): String =
    if (type == EConfigType.CUSTOM) {
        stringResource(R.string.tv_editor_protocol_custom)
    } else {
        type.name
    }
