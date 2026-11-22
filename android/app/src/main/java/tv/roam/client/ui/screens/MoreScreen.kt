package tv.roam.client.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.roam.client.R
import tv.roam.client.ui.components.TvKeyHintBar
import tv.roam.client.ui.components.TvSettingsRow
import tv.roam.client.ui.components.TvTopBar
import tv.roam.client.ui.navigation.TvRoute
import tv.roam.client.ui.navigation.TvSecondaryDestination

/** Right-hand content for the rail's More destination. */
@Composable
fun MoreScreen(
    onSelect: (TvRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val target = TvSecondaryDestination.entries
            .firstOrNull { it.route.key == lastFocusedKey }
            ?.route
            ?.key
            ?: TvSecondaryDestination.entries.first().route.key
        requesters.getOrPut(target) { FocusRequester() }.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
        ) {
            TvTopBar(title = stringResource(R.string.tv_nav_more))
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 560.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(TvSecondaryDestination.entries, key = { it.route.key }) { destination ->
                    val requester = remember(destination.route.key) {
                        requesters.getOrPut(destination.route.key) { FocusRequester() }
                    }
                    TvSettingsRow(
                        label = stringResource(destination.labelRes),
                        value = "",
                        iconRes = secondaryIcon(destination),
                        onClick = { onSelect(destination.route) },
                        modifier = Modifier
                            .focusRequester(requester)
                            .onFocusChanged {
                                if (it.isFocused) lastFocusedKey = destination.route.key
                            },
                    )
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
}

private fun secondaryIcon(destination: TvSecondaryDestination): Int = when (destination) {
    TvSecondaryDestination.Logs -> R.drawable.ic_logcat_24dp
    TvSecondaryDestination.Backup -> R.drawable.ic_restore_24dp
    TvSecondaryDestination.Update -> R.drawable.ic_check_update_24dp
    TvSecondaryDestination.About -> R.drawable.ic_about_24dp
}
