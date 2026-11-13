package tv.roam.client.feature.routing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tv.roam.client.R
import tv.roam.client.dto.entities.RulesetItem

class RoutingRulesTest {

    @Test
    fun `comma lists are trimmed and blank entries are dropped`() {
        assertEquals(listOf("a", "b"), RoutingRules.splitList(" a, b ,, "))
        assertNull(RoutingRules.splitList("  ,  "))
    }

    @Test
    fun `missing ids are generated once`() {
        val rules = mutableListOf(
            RulesetItem(id = "", remarks = "one"),
            RulesetItem(id = "stable", remarks = "two"),
        )

        assertTrue(RoutingRules.ensureIds(rules))
        assertTrue(rules[0].id.isNotBlank())
        assertEquals("stable", rules[1].id)
        assertFalse(RoutingRules.ensureIds(rules))
    }

    @Test
    fun `new state has usable routing defaults`() {
        val state = RoutingRules.newState()

        assertEquals("tcp,udp", state.value(RoutingField.Network))
        assertEquals("proxy", state.value(RoutingField.Outbound))
        assertTrue(state.isOn(RoutingField.Enabled))
        assertFalse(state.isOn(RoutingField.Locked))
    }

    @Test
    fun `ruleset fields round trip`() {
        val rule = RulesetItem(
            id = "rule-1",
            remarks = "Work",
            domain = listOf("example.com", "example.org"),
            ip = listOf("10.0.0.0/8"),
            process = listOf("com.example.app"),
            port = "443",
            network = "tcp",
            protocol = listOf("tls"),
            outboundTag = "direct",
            enabled = false,
            locked = true,
        )

        val state = RoutingRules.fromRuleset(rule)
        val restored = RoutingRules.toRuleset(state, rule)

        assertEquals("rule-1", restored.id)
        assertEquals("Work", restored.remarks)
        assertEquals(listOf("example.com", "example.org"), restored.domain)
        assertEquals(listOf("10.0.0.0/8"), restored.ip)
        assertEquals(listOf("com.example.app"), restored.process)
        assertEquals("443", restored.port)
        assertEquals("tcp", restored.network)
        assertEquals(listOf("tls"), restored.protocol)
        assertEquals("direct", restored.outboundTag)
        assertFalse(restored.enabled)
        assertTrue(restored.locked == true)
    }

    @Test
    fun `a new rule gets a generated id and default outbound`() {
        val state = RoutingRules.newState().copy(
            values = mapOf(
                RoutingField.Remarks to "  New rule  ",
                RoutingField.Outbound to "",
            ),
        )

        val rule = RoutingRules.toRuleset(state)

        assertTrue(rule.id.isNotBlank())
        assertEquals("New rule", rule.remarks)
        assertEquals("proxy", rule.outboundTag)
        assertNull(rule.network)
        assertTrue(rule.enabled)
    }

    @Test
    fun `remarks are required`() {
        val errors = RoutingRules.validate(RoutingRules.newState())

        assertEquals(R.string.tv_editor_error_required, errors[RoutingField.Remarks])
    }

    @Test
    fun `a valid rule has no errors`() {
        val state = RoutingRules.newState().copy(
            values = mapOf(
                RoutingField.Remarks to "Rule",
                RoutingField.Network to "tcp,udp",
                RoutingField.Outbound to "proxy",
            ),
        )

        assertTrue(RoutingRules.validate(state).isEmpty())
    }

    @Test
    fun `existing id is preserved when fields are edited`() {
        val existing = RulesetItem(id = "rule-1", remarks = "Old")
        val state = RoutingRules.fromRuleset(existing).copy(
            values = RoutingRules.fromRuleset(existing).values +
                (RoutingField.Remarks to "Updated"),
        )

        val restored = RoutingRules.toRuleset(state, existing)

        assertEquals("rule-1", restored.id)
        assertEquals("Updated", restored.remarks)
        assertNotNull(restored.outboundTag)
    }
}
