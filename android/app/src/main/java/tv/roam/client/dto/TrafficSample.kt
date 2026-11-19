package tv.roam.client.dto

import java.io.Serializable

/**
 * One traffic sample taken from the running core. Values are bytes per second
 * for the proxy and direct paths; the UI formats them for the Home header.
 */
data class TrafficSample(
    val proxyUp: Long,
    val proxyDown: Long,
    val directUp: Long,
    val directDown: Long,
) : Serializable
