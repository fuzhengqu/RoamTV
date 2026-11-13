package tv.roam.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.theme.TvColors

/**
 * Temporary destination used while the TV screens are built milestone by
 * milestone. It is removed as soon as the corresponding feature screen lands.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { initialFocus.requestFocus() }

    Column(modifier = modifier.fillMaxSize()) {
        TvTopBar(
            title = title,
            onBack = onBack,
            backFocusRequester = if (onBack != null) initialFocus else null,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = TvColors.Surface,
                    contentColor = TvColors.Text,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.tv_placeholder_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TvColors.Text,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.tv_placeholder_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TvColors.TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        TvKeyHintBar(
            hints = listOf(
                stringResource(R.string.tv_key_back) to stringResource(R.string.tv_key_back_action),
            )
        )
    }
}
