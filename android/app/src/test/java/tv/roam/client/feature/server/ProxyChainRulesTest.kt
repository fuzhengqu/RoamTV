package tv.roam.client.feature.server

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tv.roam.client.R

class ProxyChainRulesTest {

    private val options = listOf("A", "B", "C")

    @Test
    fun `two selected members are accepted`() {
        assertNull(ProxyChainRules.validate(listOf("A", "B"), options))
    }

    @Test
    fun `blank member is rejected`() {
        assertEquals(
            R.string.server_proxy_chain_members_unselected,
            ProxyChainRules.validate(listOf("A", ""), options),
        )
    }

    @Test
    fun `one member is insufficient`() {
        assertEquals(
            R.string.server_proxy_chain_members_insufficient,
            ProxyChainRules.validate(listOf("A"), options),
        )
    }

    @Test
    fun `unknown member is rejected`() {
        assertEquals(
            R.string.server_proxy_chain_members_invalid,
            ProxyChainRules.validate(listOf("A", "missing"), options),
        )
    }
}
