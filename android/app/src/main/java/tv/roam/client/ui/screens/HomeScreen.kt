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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvSecondaryButton
import tv.roam.client.ui.components.TvStatusDot
import tv.roam.client.ui.theme.TvColors

/**
 * M0 shell. The live server list, connection state and group tabs arrive in
 * M1; this screen exists to validate the television frame, focus flow and the
 * overscan-safe layout on a real device before the feature work starts.
 */
@Composable
fun HomeScreen(
    onImport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { initialFocus.requestFocus() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tv_nav_home),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TvColors.Text,
                )
                Spacer(Modifier.width(24.dp))
                TvStatusDot(color = TvColors.TextMuted)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.tv_status_disconnected),
                    style = MaterialTheme.typography.titleSmall,
                    color = TvColors.TextMuted,
                )
                Spacer(Modifier.weight(1f))
                TvSecondaryButton(
                    text = stringResource(R.string.tv_action_import),
                    onClick = onImport,
                    modifier = Modifier.focusRequester(initialFocus),
                )
                Spacer(Modifier.width(12.dp))
                TvSecondaryButton(
                    text = stringResource(R.string.tv_action_settings),
                    onClick = onOpenSettings,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = SurfaceDefaults.colors(
                        containerColor = TvColors.Surface,
                        contentColor = TvColors.Text,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tv_home_welcome_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TvColors.Text,
                        )
                        Text(
                            text = stringResource(R.string.tv_home_welcome_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvColors.TextMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        HomeStep(1, stringResource(R.string.tv_home_step_remote))
                        HomeStep(2, stringResource(R.string.tv_home_step_import))
                        HomeStep(3, stringResource(R.string.tv_home_step_connect))
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
                stringResource(R.string.tv_key_menu) to stringResource(R.string.tv_key_menu_action),
                stringResource(R.string.tv_key_back) to stringResource(R.string.tv_key_back_action),
            ),
        )
    }
}

@Composable
private fun HomeStep(index: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier
                .width(36.dp)
                .height(36.dp),
            shape = RoundedCornerShape(50),
            colors = SurfaceDefaults.colors(
                containerColor = TvColors.Teal.copy(alpha = 0.16f),
                contentColor = TvColors.Teal,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TvColors.Teal,
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TvColors.Text,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
