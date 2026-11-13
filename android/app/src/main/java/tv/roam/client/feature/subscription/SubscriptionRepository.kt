package tv.roam.client.feature.subscription

import tv.roam.client.dto.SubscriptionUpdateResult
import tv.roam.client.dto.entities.SubscriptionCache
import tv.roam.client.dto.entities.SubscriptionItem
import tv.roam.client.enums.EConfigType
import tv.roam.client.handler.AngConfigManager
import tv.roam.client.handler.MmkvManager
import tv.roam.client.handler.SettingsChangeManager
import tv.roam.client.handler.SettingsManager
import tv.roam.client.handler.SubscriptionUpdater
import tv.roam.client.util.Utils

interface SubscriptionDataSource {
    fun load(): List<SubscriptionCache>
    fun loadOne(guid: String): SubscriptionItem?
    fun save(guid: String, item: SubscriptionItem): String
    fun delete(guid: String)
    fun updateAll(): SubscriptionUpdateResult
    fun syncOne(guid: String)
    fun suggestions(): List<String>
}

class SubscriptionRepository : SubscriptionDataSource {

    override fun load(): List<SubscriptionCache> = MmkvManager.decodeSubscriptions()

    override fun loadOne(guid: String): SubscriptionItem? =
        if (guid.isBlank()) null else MmkvManager.decodeSubscription(guid)

    override fun save(guid: String, item: SubscriptionItem): String {
        val key = guid.ifBlank { Utils.getUuid() }
        MmkvManager.encodeSubscription(key, item)
        SettingsChangeManager.makeSetupGroupTab()
        return key
    }

    override fun delete(guid: String) {
        SettingsManager.removeSubscriptionWithDefault(guid)
        SettingsChangeManager.makeSetupGroupTab()
    }

    override fun updateAll(): SubscriptionUpdateResult =
        AngConfigManager.updateConfigViaSubAll()

    override fun syncOne(guid: String) {
        SubscriptionUpdater.syncOne(subId = guid)
    }

    override fun suggestions(): List<String> =
        SettingsManager.getProfileRemarks(
            excludeConfigTypes = setOf(
                EConfigType.CUSTOM,
                EConfigType.POLICYGROUP,
                EConfigType.PROXYCHAIN,
            ),
        )
}
