package tv.roam.client.feature.routing

import android.app.Application
import tv.roam.client.AppConfig
import tv.roam.client.dto.entities.RulesetItem
import tv.roam.client.enums.RoutingType
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.SettingsManager
import tv.roam.client.util.JsonUtil
import tv.roam.client.util.Utils

interface RoutingDataSource {
    fun loadRules(): List<RulesetItem>
    fun loadRule(id: String): RulesetItem?
    fun saveRule(rule: RulesetItem)
    fun deleteRule(id: String)
    fun strategies(): List<String>
    fun domainStrategy(): String
    fun setDomainStrategy(value: String)
    fun importPreset(type: RoutingType)
    fun importFromClipboard(): Boolean
    fun exportToClipboard(): Boolean
    fun outboundSuggestions(): List<String>
}

class RoutingRepository(private val app: Application) : RoutingDataSource {

    override fun loadRules(): List<RulesetItem> {
        val rules = MmkvManager.decodeRoutingRulesets()?.toMutableList() ?: mutableListOf()
        if (RoutingRules.ensureIds(rules)) {
            MmkvManager.encodeRoutingRulesets(rules)
        }
        return rules
    }

    override fun loadRule(id: String): RulesetItem? =
        if (id.isBlank()) null else loadRules().firstOrNull { it.id == id }

    override fun saveRule(rule: RulesetItem) {
        val rules = loadRules().toMutableList()
        val index = rules.indexOfFirst { it.id == rule.id }
        SettingsManager.saveRoutingRuleset(index, rule)
    }

    override fun deleteRule(id: String) {
        val rules = loadRules()
        val index = rules.indexOfFirst { it.id == id }
        if (index >= 0) {
            SettingsManager.removeRoutingRuleset(index)
        }
    }

    override fun strategies(): List<String> =
        app.resources.getStringArray(tv.roam.client.R.array.routing_domain_strategy).toList()

    override fun domainStrategy(): String =
        MmkvManager.decodeSettingsString(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY)
            ?: strategies().first()

    override fun setDomainStrategy(value: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_ROUTING_DOMAIN_STRATEGY, value)
    }

    override fun importPreset(type: RoutingType) {
        SettingsManager.resetRoutingRulesetsFromPresets(app, type)
    }

    override fun importFromClipboard(): Boolean {
        val content = Utils.getClipboard(app)
        if (content.isBlank()) return false
        return SettingsManager.resetRoutingRulesets(content)
    }

    override fun exportToClipboard(): Boolean {
        val rules = MmkvManager.decodeRoutingRulesets()
        if (rules.isNullOrEmpty()) return false
        Utils.setClipboard(app, JsonUtil.toJson(rules))
        return true
    }

    override fun outboundSuggestions(): List<String> =
        (AppConfig.BUILTIN_OUTBOUND_TAGS.toList() +
            SettingsManager.getProfileRemarks()).distinct()
}
