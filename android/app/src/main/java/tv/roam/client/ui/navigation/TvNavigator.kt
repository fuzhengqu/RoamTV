package tv.roam.client.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import tv.roam.client.R

/**
 * Routes are deliberately small and carry only stable domain identifiers.
 * List positions are never part of a route, so a subscription refresh or a
 * server deletion cannot silently change which editor is open.
 */
sealed interface TvRoute {
    val key: String

    data object Home : TvRoute {
        override val key: String = "home"
    }

    data object Subscriptions : TvRoute {
        override val key: String = "subscriptions"
    }

    data object Routing : TvRoute {
        override val key: String = "routing"
    }

    data object PerApp : TvRoute {
        override val key: String = "per_app"
    }

    data object Assets : TvRoute {
        override val key: String = "assets"
    }

    data object Settings : TvRoute {
        override val key: String = "settings"
    }

    data object Logs : TvRoute {
        override val key: String = "logs"
    }

    data object Backup : TvRoute {
        override val key: String = "backup"
    }

    data object About : TvRoute {
        override val key: String = "about"
    }

    data object Update : TvRoute {
        override val key: String = "update"
    }

    data object Import : TvRoute {
        override val key: String = "import"
    }

    /**
     * Server editor route. The GUID, protocol type and restart flag are part of
     * the key so process recreation reopens the same profile, not a list row.
     */
    data class ServerEditor(
        val guid: String = "",
        val configType: Int = 1,
        val restartOnSave: Boolean = false,
    ) : TvRoute {
        override val key: String =
            "server_editor|$guid|$configType|$restartOnSave"
    }

    data class PolicyGroupEditor(val guid: String = "") : TvRoute {
        override val key: String = "policy_group|$guid"
    }

    data class ProxyChainEditor(val guid: String = "") : TvRoute {
        override val key: String = "proxy_chain|$guid"
    }

    data class SubscriptionEditor(val guid: String = "") : TvRoute {
        override val key: String = "subscription_editor|$guid"
    }

    data class RoutingEditor(val ruleId: String = "") : TvRoute {
        override val key: String = "routing_editor|$ruleId"
    }
}

/** True for destinations that own the left rail; secondary flows use a back top bar. */
val TvRoute.isTopLevel: Boolean
    get() = when (this) {
        TvRoute.Home,
        TvRoute.Subscriptions,
        TvRoute.Routing,
        TvRoute.PerApp,
        TvRoute.Assets,
        TvRoute.Settings -> true

        else -> false
    }

/** Rail destinations in display order. */
enum class TvDestination(
    val route: TvRoute,
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
) {
    Home(TvRoute.Home, R.string.tv_nav_home, R.string.tv_nav_home_desc),
    Subscriptions(TvRoute.Subscriptions, R.string.title_sub_setting, R.string.tv_nav_subscriptions_desc),
    Routing(TvRoute.Routing, R.string.routing_settings_title, R.string.tv_nav_routing_desc),
    PerApp(TvRoute.PerApp, R.string.per_app_proxy_settings, R.string.tv_nav_per_app_desc),
    Assets(TvRoute.Assets, R.string.title_user_asset_setting, R.string.tv_nav_assets_desc),
    Settings(TvRoute.Settings, R.string.title_settings, R.string.tv_nav_settings_desc),
}

/** Secondary destinations, opened from the rail's More dialog. */
enum class TvSecondaryDestination(
    val route: TvRoute,
    @StringRes val labelRes: Int,
) {
    Logs(TvRoute.Logs, R.string.title_logcat),
    Backup(TvRoute.Backup, R.string.title_configuration_backup_restore),
    Update(TvRoute.Update, R.string.update_check_for_update),
    About(TvRoute.About, R.string.title_about),
}

/**
 * App-owned back stack.
 *
 * Compose for TV focus restoration needs a deterministic owner for the visible
 * screen. Keeping the stack in one small class makes back handling, tests and
 * focus restoration explicit instead of spreading them across a navigation
 * graph. Routes are saved as stable keys so a process recreation returns to the
 * last screen rather than always restarting at Home.
 */
class TvNavigator(initial: List<TvRoute> = listOf(TvRoute.Home)) {
    private val stack: SnapshotStateList<TvRoute> = mutableStateListOf<TvRoute>().apply {
        addAll(initial.ifEmpty { listOf(TvRoute.Home) })
    }

    val current: TvRoute
        get() = stack.last()

    val canGoBack: Boolean
        get() = stack.size > 1

    val depth: Int
        get() = stack.size

    fun push(route: TvRoute) {
        require(route.key.isNotEmpty())
        if (stack.last().key == route.key) return
        stack.add(route)
    }

    fun replace(route: TvRoute) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack.add(route)
    }

    /** Selects a rail destination and resets the detail stack. */
    fun openRoot(route: TvRoute) {
        stack.clear()
        stack.add(route)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popToRoot(): Boolean {
        if (stack.size <= 1) return false
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        return true
    }

    fun snapshot(): List<String> = stack.map { it.key }

    companion object {
        private val routeByKey: Map<String, TvRoute> = listOf(
            TvRoute.Home,
            TvRoute.Subscriptions,
            TvRoute.Routing,
            TvRoute.PerApp,
            TvRoute.Assets,
            TvRoute.Settings,
            TvRoute.Logs,
            TvRoute.Backup,
            TvRoute.About,
            TvRoute.Update,
            TvRoute.Import,
        ).associateBy { it.key }

        private fun fromKey(key: String): TvRoute {
            if (key.startsWith("policy_group|")) {
                return TvRoute.PolicyGroupEditor(key.substringAfter('|'))
            }
            if (key.startsWith("proxy_chain|")) {
                return TvRoute.ProxyChainEditor(key.substringAfter('|'))
            }
            if (key.startsWith("subscription_editor|")) {
                return TvRoute.SubscriptionEditor(key.substringAfter('|'))
            }
            if (key.startsWith("routing_editor|")) {
                return TvRoute.RoutingEditor(key.substringAfter('|'))
            }
            if (key.startsWith("server_editor|")) {
                val parts = key.split('|')
                if (parts.size == 4) {
                    return TvRoute.ServerEditor(
                        guid = parts[1],
                        configType = parts[2].toIntOrNull() ?: 1,
                        restartOnSave = parts[3].toBoolean(),
                    )
                }
            }
            return routeByKey[key] ?: TvRoute.Home
        }

        val Saver: Saver<TvNavigator, List<String>> = Saver(
            save = { it.snapshot() },
            restore = { keys -> TvNavigator(keys.map(::fromKey)) },
        )
    }
}

@Composable
fun rememberTvNavigator(initial: TvRoute = TvRoute.Home): TvNavigator =
    rememberSaveable(saver = TvNavigator.Saver) {
        TvNavigator(listOf(initial))
    }
