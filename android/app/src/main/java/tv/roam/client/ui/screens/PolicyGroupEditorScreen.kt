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
import tv.roam.client.feature.server.PolicyField
import tv.roam.client.feature.server.PolicyGroupEditorAction
import tv.roam.client.feature.server.PolicyGroupEditorState
import tv.roam.client.feature.server.PolicyGroupEditorViewModel
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
fun PolicyGroupEditorScreen(
    route: TvRoute.PolicyGroupEditor,
    viewModel: PolicyGroupEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    var editingField by remember { mutableStateOf<PolicyField?>(null) }
    var dropdownField by remember { mutableStateOf<PolicyField?>(null) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(route.key) {
        viewModel.onAction(PolicyGroupEditorAction.Load(route.guid))
    }
    LaunchedEffect(state.savedGuid) {
        val guid = state.savedGuid ?: return@LaunchedEffect
        viewModel.onAction(PolicyGroupEditorAction.ConsumeSaved)
        if (guid.isNotEmpty()) onSaved()
    }
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.onAction(PolicyGroupEditorAction.ConsumeDeleted)
            onDeleted()
        }
    }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) focus.requestFocus()
    }
    LaunchedEffect(editingField, dropdownField) {
        if (editingField == null && dropdownField == null) focus.requestFocus()
    }
    state.generalError?.let { errorRes ->
        LaunchedEffect(errorRes) {
            context.toast(errorRes)
            viewModel.onAction(PolicyGroupEditorAction.ConsumeGeneralError)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.menu_item_import_config_policy_group),
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
                    FieldRow(
                        label = stringResource(PolicyField.Remarks.labelRes),
                        value = state.remarks,
                        error = PolicyField.Remarks in state.errors,
                        modifier = Modifier.focusRequester(focus),
                        onClick = {
                            editingField = PolicyField.Remarks
                        },
                    )
                    FieldRow(
                        label = stringResource(PolicyField.Filter.labelRes),
                        value = state.filter,
                        onClick = {
                            editingField = PolicyField.Filter
                        },
                    )
                    FieldRow(
                        label = stringResource(PolicyField.Type.labelRes),
                        value = state.typeLabels.getOrNull(state.typeIndex).orEmpty(),
                        onClick = {
                            dropdownField = PolicyField.Type
                        },
                    )
                    FieldRow(
                        label = stringResource(PolicyField.Subscription.labelRes),
                        value = state.subscriptions.getOrNull(state.subscriptionIndex)?.label.orEmpty(),
                        onClick = {
                            dropdownField = PolicyField.Subscription
                        },
                    )
                    FieldRow(
                        label = stringResource(PolicyField.TestOutbounds.labelRes),
                        value = stringResource(
                            if (state.testOutbounds) R.string.tv_editor_on else R.string.tv_editor_off
                        ),
                        onClick = {
                            viewModel.onAction(
                                PolicyGroupEditorAction.SetTestOutbounds(!state.testOutbounds)
                            )
                        },
                    )
                    FieldRow(
                        label = stringResource(PolicyField.Fallback.labelRes),
                        value = state.fallbacks.getOrNull(state.fallbackIndex)
                            ?.ifEmpty { stringResource(R.string.tv_editor_none) }
                            .orEmpty(),
                        onClick = {
                            dropdownField = PolicyField.Fallback
                        },
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TvPrimaryButton(
                            text = stringResource(R.string.tv_editor_save),
                            onClick = { viewModel.onAction(PolicyGroupEditorAction.Save) },
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

    editingField?.let { field ->
        val initial = when (field) {
            PolicyField.Remarks -> state.remarks
            PolicyField.Filter -> state.filter
            else -> ""
        }
        TvTextEditDialog(
            title = stringResource(field.labelRes),
            initialValue = initial,
            onSave = { value ->
                when (field) {
                    PolicyField.Remarks -> viewModel.onAction(
                        PolicyGroupEditorAction.SetRemarks(value)
                    )

                    PolicyField.Filter -> viewModel.onAction(
                        PolicyGroupEditorAction.SetFilter(value)
                    )

                    else -> Unit
                }
                editingField = null
            },
            onDismiss = { editingField = null },
        )
    }

    dropdownField?.let { field ->
        val title = stringResource(field.labelRes)
        when (field) {
            PolicyField.Type -> TvOptionsDialog(
                title = title,
                options = state.typeLabels,
                selectedIndex = state.typeIndex,
                onSelectIndex = { index ->
                    viewModel.onAction(PolicyGroupEditorAction.SetType(index))
                    dropdownField = null
                },
                onDismiss = { dropdownField = null },
            )

            PolicyField.Subscription -> TvOptionsDialog(
                title = title,
                options = state.subscriptions.map { it.label },
                selectedIndex = state.subscriptionIndex,
                onSelectIndex = { index ->
                    viewModel.onAction(PolicyGroupEditorAction.SetSubscription(index))
                    dropdownField = null
                },
                onDismiss = { dropdownField = null },
            )

            PolicyField.Fallback -> TvOptionsDialog(
                title = title,
                options = state.fallbacks,
                selectedIndex = state.fallbackIndex,
                onSelectIndex = { index ->
                    viewModel.onAction(PolicyGroupEditorAction.SetFallback(index))
                    dropdownField = null
                },
                onDismiss = { dropdownField = null },
            )

            else -> Unit
        }
    }

    if (showDelete) {
        TvConfirmDialog(
            title = stringResource(R.string.action_delete),
            message = stringResource(R.string.confirm_delete_policy_group),
            confirmText = stringResource(R.string.action_delete),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                showDelete = false
                viewModel.onAction(PolicyGroupEditorAction.Delete)
            },
            onDismiss = { showDelete = false },
            danger = true,
        )
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: Boolean = false,
) {
    val display = if (value.isEmpty()) {
        stringResource(R.string.tv_editor_not_set)
    } else {
        value
    }
    Column {
        TvSettingsRow(
            label = label,
            value = display,
            iconRes = R.drawable.ic_edit_24dp,
            onClick = onClick,
            modifier = modifier,
        )
        if (error) {
            Text(
                text = stringResource(R.string.tv_editor_error_required),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.Error,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
            )
        }
    }
}
