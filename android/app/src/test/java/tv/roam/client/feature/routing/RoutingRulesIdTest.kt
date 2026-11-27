package tv.roam.client.feature.routing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tv.roam.client.dto.entities.RulesetItem

class RoutingRulesIdTest {

    @Test
    fun `blank and duplicate ids are repaired without reordering`() {
        val rules = mutableListOf(
            RulesetItem(id = "first", remarks = "first"),
            RulesetItem(id = "", remarks = "blank"),
            RulesetItem(id = "first", remarks = "duplicate"),
            RulesetItem(id = "last", remarks = "last"),
        )

        assertTrue(RoutingRules.ensureIds(rules))

        assertEquals(listOf("first", "blank", "duplicate", "last"), rules.map { it.remarks })
        assertEquals(4, rules.map { it.id }.toSet().size)
        assertEquals("first", rules[0].id)
        assertEquals("last", rules[3].id)
    }
}
