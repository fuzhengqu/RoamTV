package tv.roam.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.feature.settings.SettingsViewModel
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvRailItem
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors

/**
 * TV settings. M1 ships the language section first; the remaining connection,
 * core and TV preference groups are added in M5 on the same screen.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()
    val languageEntries = stringArrayResource(R.array.language_select).toList()
    val languageValues = stringArrayResource(R.array.language_select_value).toList()
    val languageLabel = languageLabel(currentLanguage, languageValues, languageEntries)
    val initialFocus = remember { FocusRequester() }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { initialFocus.requestFocus() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(title = stringResource(R.string.title_settings))

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.title_ui_settings),
                style = MaterialTheme.typography.labelLarge,
                color = TvColors.TextMuted,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            TvSettingsRow(
                label = stringResource(R.string.title_language),
                value = languageLabel,
                iconRes = R.drawable.ic_translate_24dp,
                onClick = { showLanguageDialog = true },
                modifier = Modifier.focusRequester(initialFocus),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.tv_settings_language_summary),
                style = MaterialTheme.typography.bodySmall,
                color = TvColors.TextMuted,
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

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = currentLanguage,
            languageEntries = languageEntries,
            languageValues = languageValues,
            onSelect = { code ->
                showLanguageDialog = false
                viewModel.setLanguage(code)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

@Composable
private fun LanguageDialog(
    currentLanguage: String,
    languageEntries: List<String>,
    languageValues: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedFocus = remember { FocusRequester() }
    val selectedIndex = languageValues.indexOf(currentLanguage).takeIf { it >= 0 } ?: 0
    LaunchedEffect(Unit) { selectedFocus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.title_language),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                languageValues.forEachIndexed { index, code ->
                    val label = if (code == "auto") {
                        stringResource(R.string.language_follow_system)
                    } else {
                        languageEntries.getOrElse(index) { code }
                    }
                    TvRailItem(
                        label = label,
                        iconRes = R.drawable.ic_translate_24dp,
                        selected = code == currentLanguage,
                        onClick = { onSelect(code) },
                        modifier = if (index == selectedIndex) {
                            Modifier.focusRequester(selectedFocus)
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun languageLabel(
    currentLanguage: String,
    languageValues: List<String>,
    languageEntries: List<String>,
): String {
    if (currentLanguage == "auto") return stringResource(R.string.language_follow_system)
    val index = languageValues.indexOf(currentLanguage)
    return languageEntries.getOrElse(index) { currentLanguage }
}
