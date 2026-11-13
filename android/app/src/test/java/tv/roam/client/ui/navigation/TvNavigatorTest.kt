package tv.roam.client.ui.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TvNavigatorTest {

    @Test
    fun `initial stack is home and cannot pop`() {
        val navigator = TvNavigator()

        assertEquals(TvRoute.Home, navigator.current)
        assertEquals(1, navigator.depth)
        assertFalse(navigator.canGoBack)
        assertFalse(navigator.pop())
    }

    @Test
    fun `push adds a detail and pop returns to the previous route`() {
        val navigator = TvNavigator()

        navigator.push(TvRoute.Import)
        assertEquals(TvRoute.Import, navigator.current)
        assertEquals(2, navigator.depth)
        assertTrue(navigator.canGoBack)

        assertTrue(navigator.pop())
        assertEquals(TvRoute.Home, navigator.current)
        assertEquals(1, navigator.depth)
    }

    @Test
    fun `pushing the current route is a no-op`() {
        val navigator = TvNavigator()

        navigator.push(TvRoute.Home)

        assertEquals(1, navigator.depth)
    }

    @Test
    fun `openRoot resets the detail stack`() {
        val navigator = TvNavigator()
        navigator.push(TvRoute.Import)
        navigator.push(TvRoute.About)

        navigator.openRoot(TvRoute.Settings)

        assertEquals(TvRoute.Settings, navigator.current)
        assertEquals(1, navigator.depth)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun `replace swaps only the top route`() {
        val navigator = TvNavigator()
        navigator.push(TvRoute.Import)

        navigator.replace(TvRoute.About)

        assertEquals(TvRoute.About, navigator.current)
        assertEquals(2, navigator.depth)
        assertTrue(navigator.pop())
        assertEquals(TvRoute.Home, navigator.current)
    }

    @Test
    fun `popToRoot keeps the root route`() {
        val navigator = TvNavigator()
        navigator.push(TvRoute.Import)
        navigator.push(TvRoute.About)

        assertTrue(navigator.popToRoot())
        assertEquals(TvRoute.Home, navigator.current)
        assertEquals(1, navigator.depth)
        assertFalse(navigator.popToRoot())
    }

    @Test
    fun `server editor route restores its parameters`() {
        val route = TvRoute.ServerEditor(
            guid = "guid-1",
            configType = 5,
            restartOnSave = true,
        )

        val restored = TvNavigator.Saver.restore(listOf(route.key))

        assertEquals(route, restored?.current)
    }

    @Test
    fun `policy group and proxy chain routes restore their guids`() {
        val group = TvRoute.PolicyGroupEditor("group-1")
        val chain = TvRoute.ProxyChainEditor("chain-1")

        assertEquals(group, TvNavigator.Saver.restore(listOf(group.key))?.current)
        assertEquals(chain, TvNavigator.Saver.restore(listOf(chain.key))?.current)
    }

    @Test
    fun `subscription editor route restores its guid`() {
        val route = TvRoute.SubscriptionEditor("sub-1")

        assertEquals(route, TvNavigator.Saver.restore(listOf(route.key))?.current)
    }

    @Test
    fun `saved keys restore the stack`() {        val navigator = TvNavigator()
        navigator.push(TvRoute.Subscriptions)
        navigator.push(TvRoute.Settings)

        val saved = navigator.snapshot()
        val restored = TvNavigator(saved.map { key ->
            listOf(
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
            ).first { it.key == key }
        })

        assertEquals(saved, restored.snapshot())
        assertEquals(TvRoute.Settings, restored.current)
    }
}
