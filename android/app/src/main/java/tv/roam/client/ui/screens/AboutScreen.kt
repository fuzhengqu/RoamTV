package tv.roam.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.BuildConfig
import tv.roam.client.R
import tv.roam.client.core.CoreNativeManager
import tv.roam.client.feature.about.AboutViewModel
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvScrollableText
import tv.roam.client.ui.components.TvSecondaryButton
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors

@Composable
fun AboutScreen(
    viewModel: AboutViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val firstFocus = remember { FocusRequester() }
    var showLicenses by remember { mutableStateOf(false) }
    var showTranslators by remember { mutableStateOf(false) }
    val libVersion = remember { CoreNativeManager.getLibVersion() }

    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(
                title = stringResource(R.string.title_about),
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TvSettingsRow(
                    label = stringResource(R.string.title_oss_license),
                    value = "",
                    iconRes = R.drawable.license_24px,
                    onClick = { showLicenses = true },
                    modifier = Modifier.focusRequester(firstFocus),
                )
                TvSettingsRow(
                    label = stringResource(R.string.title_translators),
                    value = "",
                    iconRes = R.drawable.ic_translate_24dp,
                    onClick = { showTranslators = true },
                )
                Text(
                    text = stringResource(R.string.about_upstream_attribution),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Text(
                    text = "${stringResource(R.string.tv_about_version)} v${BuildConfig.VERSION_NAME} ($libVersion)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.Text,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Text(
                    text = "${stringResource(R.string.tv_about_app_id)} ${BuildConfig.APPLICATION_ID}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TvColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(24.dp))
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

    if (showLicenses) {
        TextDialog(
            title = stringResource(R.string.title_oss_license),
            lines = if (state.isLoading) {
                listOf(stringResource(R.string.tv_editor_loading))
            } else {
                state.licenseLines
            },
            onDismiss = { showLicenses = false },
        )
    }

    if (showTranslators) {
        TextDialog(
            title = stringResource(R.string.title_translators),
            lines = if (state.isLoading) {
                listOf(stringResource(R.string.tv_editor_loading))
            } else {
                state.translatorLines
            },
            onDismiss = { showTranslators = false },
        )
    }
}

@Composable
private fun TextDialog(
    title: String,
    lines: List<String>,
    onDismiss: () -> Unit,
) {
    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            TvScrollableText(lines = lines)
            TvSecondaryButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
            )
        }
    }
}
