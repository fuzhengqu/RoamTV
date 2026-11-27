package tv.roam.client.handler

import tv.roam.client.dto.entities.ProfileItem

/** Reads and writes proxy-chain member remarks without comma ambiguity. */
object ProxyChainStorage {
    fun decode(profile: ProfileItem): List<String> {
        val list = profile.proxyChainProfileList
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
        if (!list.isNullOrEmpty()) return list
        return splitLegacy(profile.proxyChainProfiles)
    }

    fun encode(profile: ProfileItem, members: List<String>) {
        profile.proxyChainProfileList = members
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        profile.proxyChainProfiles = null
    }

    /** Best-effort migration for the pre-list comma-separated field. */
    fun splitLegacy(value: String?): List<String> = value
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
}
