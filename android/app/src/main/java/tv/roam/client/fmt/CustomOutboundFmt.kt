package tv.roam.client.fmt

import tv.roam.client.AppConfig
import tv.roam.client.dto.V2rayConfig
import tv.roam.client.dto.entities.ProfileItem
import tv.roam.client.enums.EConfigType
import tv.roam.client.util.JsonUtil

/**
 * Imports a single Xray outbound JSON object (the shape used under
 * `outbounds`), not a full client configuration. The raw outbound is stored and
 * injected by the core config builder.
 */
object CustomOutboundFmt : FmtBase() {
    fun parse(str: String): Pair<ProfileItem, String> {
        val config = ProfileItem.create(EConfigType.CUSTOM_OUTBOUND)

        val outbound = JsonUtil.fromJson(str, V2rayConfig.OutboundBean::class.java)
        config.remarks = outbound?.tag
            ?.takeIf { it.isNotBlank() && it != AppConfig.TAG_PROXY }
            ?: System.currentTimeMillis().toString()
        config.server = outbound?.getServerAddress()
        config.serverPort = outbound?.getServerPort()?.toString()

        outbound?.tag = AppConfig.TAG_PROXY
        val rawOutbound = outbound?.let { JsonUtil.toJsonPretty(it) } ?: ""
        return Pair(config, rawOutbound)
    }
}
