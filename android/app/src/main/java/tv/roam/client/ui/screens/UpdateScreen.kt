package tv.roam.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import tv.roam.client.BuildConfig
import tv.roam.client.R
import tv.roam.client.extension.toast
import tv.roam.client.feature.checkupdate.CheckUpdateViewModel
import tv.roam.client.feature.checkupdate.UpdateAction
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvPrimaryButton
import tv.roam.client.ui.components.TvScrollableText
import tv.roam.client.ui.components.TvSecondaryButton
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors
import tv.roam.client.util.Utils

@Composable
fun UpdateScreen(
    viewModel: CheckUpdateViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        firstFocus.requestFocus()
        viewModel.onAction(UpdateAction.Check)
    }
    LaunchedEffect(state.messageRes) {
        val message = state.messageRes ?: return@LaunchedEffect
        context.toast(message)
        viewModel.onAction(UpdateAction.ConsumeMessage)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.update_check_for_update),
                onBack = onBack,
            )
            if (state.isLoading) {
                Text(
                    text = stringResource(R.string.tv_editor_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.Amber,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            TvSettingsRow(
                label = stringResource(R.string.update_check_pre_release),
                value = stringResource(
                    if (state.checkPreRelease) R.string.tv_editor_on else R.string.tv_editor_off
                ),
                iconRes = R.drawable.ic_source_code_24dp,
                onClick = {
                    viewModel.onAction(UpdateAction.SetPreRelease(!state.checkPreRelease))
                },
                modifier = Modifier.focusRequester(firstFocus),
            )
            TvSettingsRow(
                label = stringResource(R.string.update_check_for_update),
                value = "",
                iconRes = R.drawable.ic_check_update_24dp,
                onClick = { viewModel.onAction(UpdateAction.Check) },
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                color = TvColors.Text,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = BuildConfig.APPLICATION_ID,
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
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

    val result = state.result
    if (result != null) {
        val actionFocus = remember(result) { FocusRequester() }
        LaunchedEffect(result) { actionFocus.requestFocus() }
        TvDialog(
            onDismissRequest = {
                viewModel.onAction(UpdateAction.ConsumeResult)
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(
                        R.string.update_new_version_found,
                        result.latestVersion.orEmpty(),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TvColors.Text,
                )
                TvScrollableText(
                    lines = result.releaseNotes.orEmpty().lines()
                        .filter { it.isNotBlank() }
                        .ifEmpty { listOf(stringResource(R.string.tv_editor_not_set)) },
                    autoFocus = false,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TvSecondaryButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = {
                            viewModel.onAction(UpdateAction.ConsumeResult)
                        },
                    )
                    TvPrimaryButton(
                        text = stringResource(R.string.update_now),
                        onClick = {
                            result.downloadUrl?.let { Utils.openUri(context, it) }
                            viewModel.onAction(UpdateAction.ConsumeResult)
                        },
                        modifier = Modifier.focusRequester(actionFocus),
                    )
                }
            }
        }
    }
}
