package tv.roam.client.core

import android.content.Context
import tv.roam.client.AppConfig
import tv.roam.client.dto.CoreConfigContext
import tv.roam.client.dto.entities.ProfileEntry
import tv.roam.client.enums.BalancerStrategyType
import tv.roam.client.enums.CoreResolvedType
import tv.roam.client.enums.EConfigType
import tv.roam.client.extension.isComplexType
import tv.roam.client.extension.isNotNullEmpty
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.SettingsManager
import tv.roam.client.util.LogUtil
import tv.roam.client.util.Utils

/**
 * Build runtime context from the selected profile.
 *
 * All outbound type analysis is completed here for both the selected profile
 * and routing targets. Full custom configs are returned immediately; a custom
 * outbound keeps its GUID so the raw outbound JSON can be resolved later.
 */
object CoreConfigContextBuilder {

    /**
     * Load one profile and produce a fully analyzed context.
     *
     * Null is returned only when the selected profile cannot be loaded.
     */
    fun build(context: Context, guid: String): CoreConfigContext? {
        val config = MmkvManager.decodeServerConfig(guid) ?: return null

        // CUSTOM: return immediately — CoreConfigManager handles the full config path.
        if (config.configType == EConfigType.CUSTOM) {
            return CoreConfigContext(context = context, guid = guid, isCustom = true)
        }

        // Step 1: Resolve the main outbound (always tag = TAG_PROXY).
        val primaryResolvedOutbound = resolveOutbound(
            AppConfig.TAG_PROXY,
            ProfileEntry(guid, config),
        ) ?: run {
            LogUtil.e(AppConfig.TAG, "Failed to resolve main outbound for '${config.remarks}'")
            return null
        }

        // Step 2: Resolve all non-builtin routing outbound tags.
        val routingResolvedOutbounds = resolveRoutingOutbounds()
        val resolvedOutbounds = listOf(primaryResolvedOutbound) + routingResolvedOutbounds
        val fallbackResolvedOutbounds = resolveFallbackOutbounds(resolvedOutbounds)
        val routingDomainRules = collectRoutingDomainRulesForDns()

        return CoreConfigContext(
            context = context,
            guid = guid,
            resolvedOutbounds = resolvedOutbounds + fallbackResolvedOutbounds,
            routingDomainRules = routingDomainRules,
        )
    }

    /**
     * Resolve one outbound target into a normalized outbound entry.
     *
     * Full custom configs are ignored at this stage; a custom outbound entry
     * remains addressable through its GUID.
     */
    private fun resolveOutbound(
        tag: String,
        entry: ProfileEntry,
    ): CoreConfigContext.ResolvedOutbound? {
        if (entry.profile.configType == EConfigType.CUSTOM) {
            return null
        }

        val (resolvedEntries, resolvedType) = when (entry.profile.configType) {
            EConfigType.POLICYGROUP -> Pair(
                resolvePolicyGroupEntries(entry),
                CoreResolvedType.POLICYGROUP,
            )

            EConfigType.PROXYCHAIN -> {
                val chainEntries = resolveProxyChainEntries(entry)
                val type = if (chainEntries.size <= 1) CoreResolvedType.NORMAL else CoreResolvedType.PROXYCHAIN
                Pair(chainEntries, type)
            }

            else -> {
                val chainEntries = resolveProxyChainEntriesFromGroup(entry)
                val type = if (chainEntries.size <= 1) CoreResolvedType.NORMAL else CoreResolvedType.PROXYCHAIN
                Pair(chainEntries, type)
            }
        }

        return CoreConfigContext.ResolvedOutbound(
            tag = tag,
            entry = entry,
            resolvedEntries = resolvedEntries,
            resolvedType = resolvedType,
        )
    }

    /**
     * Collect and resolve non-builtin routing targets from enabled rules.
     *
     * Invalid or empty targets are skipped and handled by fallback logic later.
     */
    private fun resolveRoutingOutbounds(): List<CoreConfigContext.ResolvedOutbound> {
        val rulesetItems = MmkvManager.decodeRoutingRulesets() ?: return emptyList()
        val resolvedOutbounds = mutableListOf<CoreConfigContext.ResolvedOutbound>()
        val processedTags = mutableSetOf<String>()

        try {
            rulesetItems
                .filter { it.enabled }
                .mapNotNull { it.outboundTag.takeIf { tag -> tag.isNotBlank() } }
                .filter { tag -> tag !in AppConfig.BUILTIN_OUTBOUND_TAGS }
                .distinct()
                .forEach { tag ->
                    if (tag in processedTags) {
                        return@forEach
                    }
                    processedTags.add(tag)

                    try {
                        val entry = SettingsManager.getProfileEntryViaRemarks(tag) ?: run {
                            LogUtil.w(AppConfig.TAG, "Routing tag '$tag' has no matching profile — will fall back to proxy at routing time")
                            return@forEach
                        }
                        val resolvedOutbound = resolveOutbound(tag, entry) ?: run {
                            LogUtil.w(AppConfig.TAG, "Cannot use CUSTOM profile as routing outbound for tag '$tag', skipping")
                            return@forEach
                        }
                        if (resolvedOutbound.resolvedEntries.isEmpty()) {
                            LogUtil.w(AppConfig.TAG, "Routing outbound '$tag' resolved to empty list, skipping")
                            return@forEach
                        }
                        resolvedOutbounds.add(resolvedOutbound)
                        LogUtil.d(AppConfig.TAG, "Resolved routing outbound: tag='$tag', type='${resolvedOutbound.resolvedType}', profiles=${resolvedOutbound.resolvedEntries.size}")
                    } catch (e: Exception) {
                        LogUtil.e(AppConfig.TAG, "Failed to resolve routing outbound for tag '$tag', skipping", e)
                    }
                }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve routing outbounds from rulesets", e)
        }

        return resolvedOutbounds
    }

    private fun resolvePolicyGroupEntries(config: ProfileEntry): List<ProfileEntry> {
        try {
            return MmkvManager.decodeAllServerList()
                .asSequence()
                .mapNotNull { id ->
                    MmkvManager.decodeServerConfig(id)?.let { ProfileEntry(id, it) }
                }
                .filter { entry ->
                    val subscriptionId = config.profile.policyGroupSubscriptionId
                    if (subscriptionId.isNullOrBlank()) {
                        true
                    } else {
                        entry.profile.subscriptionId == subscriptionId
                    }
                }
                .filter { entry ->
                    val filter = config.profile.policyGroupFilter
                    if (filter.isNullOrBlank()) {
                        true
                    } else {
                        try {
                            Regex(filter).containsMatchIn(entry.profile.remarks)
                        } catch (_: Exception) {
                            entry.profile.remarks.contains(filter)
                        }
                    }
                }
                .filter { it.profile.server.isNotNullEmpty() }
                .filter {
                    Utils.isPureIpAddress(it.profile.server!!) ||
                        Utils.isValidUrl(it.profile.server!!)
                }
                .filter { !it.profile.configType.isComplexType() }
                .toList()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve policy group profiles for '${config.profile.remarks}'", e)
            return listOf(config)
        }
    }

    private fun resolveProxyChainEntries(config: ProfileEntry): List<ProfileEntry> {
        if (config.profile.proxyChainProfiles.isNullOrBlank()) {
            return listOf(config)
        }

        try {
            return config.profile.proxyChainProfiles.orEmpty().split(",")
                .asSequence()
                .mapNotNull { remark -> SettingsManager.getProfileEntryViaRemarks(remark) }
                .filter { it.profile.server.isNotNullEmpty() }
                .filter {
                    Utils.isPureIpAddress(it.profile.server!!) ||
                        Utils.isValidUrl(it.profile.server!!)
                }
                .filter { !it.profile.configType.isComplexType() }
                .toList()
                .reversed()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve proxy chain profiles for '${config.profile.remarks}'", e)
            return listOf(config)
        }
    }

    /**
     * Resolve chain nodes from subscription neighbors in order: next, current, prev.
     *
     * When no chain is available, return a single-node result.
     */
    private fun resolveProxyChainEntriesFromGroup(config: ProfileEntry): List<ProfileEntry> {
        if (config.profile.subscriptionId.isEmpty()) {
            return listOf(config)
        }

        try {
            val subItem = MmkvManager.decodeSubscription(config.profile.subscriptionId)
                ?: return listOf(config)
            val resolved = mutableListOf<ProfileEntry>()
            SettingsManager.getProfileEntryViaRemarks(subItem.nextProfile)?.let { resolved.add(it) }
            resolved.add(config)
            SettingsManager.getProfileEntryViaRemarks(subItem.prevProfile)?.let { resolved.add(it) }
            return resolved
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Failed to resolve proxy chain from group for '${config.profile.remarks}'", e)
            return listOf(config)
        }
    }

    /**
     * Collect enabled routing domain rules in original order for DNS segmentation.
     *
     * outbounds are normalized into three tags only: proxy / direct / block.
     */
    private fun collectRoutingDomainRulesForDns(): List<CoreConfigContext.RoutingDomainRule> {
        val rulesetItems = MmkvManager.decodeRoutingRulesets() ?: return emptyList()
        val result = mutableListOf<CoreConfigContext.RoutingDomainRule>()

        rulesetItems
            .asSequence()
            .filter { it.enabled }
            .filter { !it.domain.isNullOrEmpty() }
            .forEach { rule ->
                val normalizedOutboundTag = when (rule.outboundTag) {
                    AppConfig.TAG_DIRECT -> AppConfig.TAG_DIRECT
                    AppConfig.TAG_BLOCKED -> AppConfig.TAG_BLOCKED
                    else -> AppConfig.TAG_PROXY
                }
                result.add(
                    CoreConfigContext.RoutingDomainRule(
                        domain = rule.domain.orEmpty(),
                        outboundTag = normalizedOutboundTag
                    )
                )
            }

        return result
    }

    /**
     * Resolve and collect fallback outbounds from all POLICYGROUP nodes.
     *
     * Fallback targets must not overlap with already resolved tags or builtin tags.
     */
    private fun resolveFallbackOutbounds(
        resolvedOutbounds: List<CoreConfigContext.ResolvedOutbound>,
    ): List<CoreConfigContext.ResolvedOutbound> {
        return resolvedOutbounds
            .asSequence()
            .filter { it.resolvedType == CoreResolvedType.POLICYGROUP }
            .filter {
                BalancerStrategyType.from(it.entry.profile.policyGroupType).supportsObservatory &&
                    it.entry.profile.policyGroupTestOutbounds != false
            }
            .mapNotNull { it.entry.profile.policyGroupFallbackTag }
            .filter { it !in AppConfig.BUILTIN_OUTBOUND_TAGS && resolvedOutbounds.none { outbound -> outbound.tag == it } }
            .distinct()
            .mapNotNull { tag ->
                SettingsManager.getProfileEntryViaRemarks(tag)
                    ?.takeUnless {
                        it.profile.configType == EConfigType.CUSTOM ||
                            it.profile.configType == EConfigType.POLICYGROUP
                    }
                    ?.let { resolveOutbound(tag, it) }
            }
            .toList()
    }
}
