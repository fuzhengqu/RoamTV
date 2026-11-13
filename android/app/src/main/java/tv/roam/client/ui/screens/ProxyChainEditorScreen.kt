package tv.roam.client.ui.screens

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.feature.server.ProxyChainEditorAction
import tv.roam.client.feature.server.ProxyChainEditorState
import tv.roam.client.feature.server.ProxyChainEditorViewModel
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
fun ProxyChainEditorScreen(
    route: TvRoute.ProxyChainEditor,
    viewModel: ProxyChainEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    var editRemarks by remember { mutableStateOf(false) }
    var editMemberIndex by remember { mutableStateOf<Int?>(null) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(route.key) {
        viewModel.onAction(ProxyChainEditorAction.Load(route.guid))
    }
    LaunchedEffect(state.savedGuid) {
        val guid = state.savedGuid ?: return@LaunchedEffect
        viewModel.onAction(ProxyChainEditorAction.ConsumeSaved)
        if (guid.isNotEmpty()) onSaved()
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.onAction(ProxyChainEditorAction.ConsumeDeleted)
            onDeleted()
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) focus.requestFocus()
    }
    state.generalError?.let { errorRes ->
        LaunchedEffect(errorRes) {
            context.toast(errorRes)
            viewModel.onAction(ProxyChainEditorAction.ConsumeGeneralError)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.menu_item_import_config_proxy_chain),
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
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val remarksDisplay = if (state.remarks.isEmpty()) {
                        stringResource(R.string.tv_editor_not_set)
                    } else {
                        state.remarks
                    }
                    TvSettingsRow(
                        label = stringResource(R.string.server_lab_remarks),
                        value = remarksDisplay,
                        iconRes = R.drawable.ic_edit_24dp,
                        onClick = { editRemarks = true },
                        modifier = Modifier.focusRequester(focus),
                    )
                    if (state.error) {
                        Text(
                            text = stringResource(R.string.tv_editor_error_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = TvColors.Error,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.server_proxy_chain_members),
                        style = MaterialTheme.typography.titleMedium,
                        color = TvColors.Teal,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                    state.members.forEachIndexed { index, member ->
                        val memberDisplay = if (member.isEmpty()) {
                            stringResource(R.string.tv_editor_not_set)
                        } else {
                            member
                        }
                        TvSettingsRow(
                            label = stringResource(R.string.tv_editor_member_number, index + 1),
                            value = memberDisplay,
                            iconRes = R.drawable.ic_expand_more_24dp,
                            onClick = { editMemberIndex = index },
                        )
                    }
                    TvSettingsRow(
                        label = stringResource(R.string.acc_add_member),
                        value = "",
                        iconRes = R.drawable.ic_add_24dp,
                        onClick = { viewModel.onAction(ProxyChainEditorAction.AddMember) },
                    )
                    if (state.members.size > 2) {
                        TvSettingsRow(
                            label = stringResource(R.string.tv_editor_remove_last),
                            value = "",
                            iconRes = R.drawable.ic_delete_24dp,
                            onClick = {
                                viewModel.onAction(
                                    ProxyChainEditorAction.RemoveMember(state.members.lastIndex)
                                )
                            },
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TvPrimaryButton(
                            text = stringResource(R.string.tv_editor_save),
                            onClick = { viewModel.onAction(ProxyChainEditorAction.Save) },
                            enabled = !state.isSaving,
                        )
                        if (!state.isNew) {
                            TvSecondaryButton(
                                text = stringResource(R.string.action_delete),
                                onClick = { showDelete = true },
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

    if (editRemarks) {
        TvTextEditDialog(
            title = stringResource(R.string.server_lab_remarks),
            initialValue = state.remarks,
            onSave = { value ->
                viewModel.onAction(ProxyChainEditorAction.SetRemarks(value))
                editRemarks = false
            },
            onDismiss = { editRemarks = false },
        )
    }

    editMemberIndex?.let { index ->
        TvOptionsDialog(
            title = stringResource(R.string.server_proxy_chain_members),
            options = state.options,
            selectedIndex = state.options.indexOf(state.members.getOrNull(index).orEmpty()),
            onSelectIndex = { optionIndex ->
                viewModel.onAction(
                    ProxyChainEditorAction.SetMember(index, state.options[optionIndex])
                )
                editMemberIndex = null
            },
            onDismiss = { editMemberIndex = null },
        )
    }

    if (showDelete) {
        TvConfirmDialog(
            title = stringResource(R.string.action_delete),
            message = stringResource(R.string.confirm_delete_proxy_chain_member),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showDelete = false
                viewModel.onAction(ProxyChainEditorAction.Delete)
            },
            onDismiss = { showDelete = false },
            danger = true,
        )
    }
}
