package tv.roam.client.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.ui.components.TvRailItem
import tv.roam.client.ui.theme.TvColors

@Composable
fun TvNavRail(
    current: TvRoute,
    onSelect: (TvRoute) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified,
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = TvColors.Text,
            )
        }

        Spacer(Modifier.height(12.dp))

        // A television box can report a shorter content area than a 1080p
        // panel. Scrolling the primary items keeps Settings reachable instead
        // of letting the rail overflow below the overscan safe area.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TvDestination.entries.forEach { destination ->
                TvRailItem(
                    label = stringResource(destination.labelRes),
                    iconRes = destinationIcon(destination),
                    selected = current.key == destination.route.key,
                    onClick = { onSelect(destination.route) },
                )
            }
        }

        TvRailItem(
            label = stringResource(R.string.tv_nav_more),
            iconRes = R.drawable.ic_more_vert_24dp,
            selected = current.key in secondaryKeys,
            onClick = onMore,
        )
        Spacer(Modifier.height(8.dp))
    }
}

private val secondaryKeys: Set<String> = TvSecondaryDestination.entries
    .map { it.route.key }
    .toSet()

private fun destinationIcon(destination: TvDestination): Int = when (destination) {
    TvDestination.Home -> R.drawable.ic_play_24dp
    TvDestination.Subscriptions -> R.drawable.ic_subscriptions_24dp
    TvDestination.Routing -> R.drawable.ic_routing_24dp
    TvDestination.PerApp -> R.drawable.ic_per_apps_24dp
    TvDestination.Assets -> R.drawable.ic_file_24dp
    TvDestination.Settings -> R.drawable.ic_settings_24dp
}
