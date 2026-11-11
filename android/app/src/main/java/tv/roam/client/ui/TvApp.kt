package tv.roam.client.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.roam.client.R
import tv.roam.client.feature.main.MainViewModel
import tv.roam.client.ui.components.TvDialog
import tv.roam.client.ui.components.TvOverscan
import tv.roam.client.ui.components.TvRailItem
import tv.roam.client.ui.components.TvRailLayout
import tv.roam.client.ui.navigation.TvDestination
import tv.roam.client.ui.navigation.TvNavRail
import tv.roam.client.ui.navigation.TvRoute
import tv.roam.client.ui.navigation.TvSecondaryDestination
import tv.roam.client.ui.navigation.TvNavigator
import tv.roam.client.ui.navigation.isTopLevel
import tv.roam.client.ui.navigation.rememberTvNavigator
import tv.roam.client.ui.screens.HomeScreen
import tv.roam.client.ui.screens.PlaceholderScreen
import tv.roam.client.ui.theme.TvColors

@Composable
fun TvApp(
    mainViewModel: MainViewModel,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRestartService: () -> Unit,
    navigator: TvNavigator = rememberTvNavigator(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var showMore by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (!navigator.pop()) {
            activity?.moveTaskToBack(true)
        }
    }

    val current = navigator.current
    val screen: @Composable () -> Unit = {
        when (val route = current) {
            TvRoute.Home -> HomeScreen(
                mainViewModel = mainViewModel,
                onStartService = onStartService,
                onStopService = onStopService,
                onRestartService = onRestartService,
                onImport = { navigator.push(TvRoute.Import) },
            )

            else -> PlaceholderScreen(
                title = routeTitle(route),
                onBack = if (navigator.canGoBack) {
                    { navigator.pop() }
                } else {
                    null
                },
            )
        }
    }

    if (current.isTopLevel) {
        TvRailLayout(
            rail = {
                TvNavRail(
                    current = current,
                    onSelect = { route -> navigator.openRoot(route) },
                    onMore = { showMore = true },
                )
            },
            content = screen,
        )
    } else {
        // Secondary flows replace the rail with a back top bar, keeping the
        // whole width for the import/editor content.
        TvOverscan {
            screen()
        }
    }

    if (showMore) {
        MoreDialog(
            onDismiss = { showMore = false },
            onSelect = { route ->
                showMore = false
                navigator.push(route)
            },
        )
    }
}

@Composable
private fun MoreDialog(
    onDismiss: () -> Unit,
    onSelect: (TvRoute) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstFocus.requestFocus() }

    TvDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(min = 320.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.tv_nav_more),
                style = MaterialTheme.typography.headlineSmall,
                color = TvColors.Text,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            TvSecondaryDestination.entries.forEachIndexed { index, destination ->
                TvRailItem(
                    label = stringResource(destination.labelRes),
                    iconRes = secondaryIcon(destination),
                    selected = false,
                    onClick = { onSelect(destination.route) },
                    modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                )
            }
        }
    }
}

private fun secondaryIcon(destination: TvSecondaryDestination): Int = when (destination) {
    TvSecondaryDestination.Logs -> R.drawable.ic_logcat_24dp
    TvSecondaryDestination.Backup -> R.drawable.ic_restore_24dp
    TvSecondaryDestination.Update -> R.drawable.ic_check_update_24dp
    TvSecondaryDestination.About -> R.drawable.ic_about_24dp
}

@Composable
private fun routeTitle(route: TvRoute): String = when (route) {
    TvRoute.Home -> stringResource(R.string.tv_nav_home)
    TvRoute.Subscriptions -> stringResource(R.string.title_sub_setting)
    TvRoute.Routing -> stringResource(R.string.routing_settings_title)
    TvRoute.PerApp -> stringResource(R.string.per_app_proxy_settings)
    TvRoute.Assets -> stringResource(R.string.title_user_asset_setting)
    TvRoute.Settings -> stringResource(R.string.title_settings)
    TvRoute.Logs -> stringResource(R.string.title_logcat)
    TvRoute.Backup -> stringResource(R.string.title_configuration_backup_restore)
    TvRoute.About -> stringResource(R.string.title_about)
    TvRoute.Update -> stringResource(R.string.update_check_for_update)
    TvRoute.Import -> stringResource(R.string.tv_action_import)
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Kept for future rail-selection helpers that need the same mapping.
internal fun TvDestination.matches(route: TvRoute): Boolean = this.route.key == route.key
