package tv.roam.client.feature.pairing

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import tv.roam.client.BuildConfig
import java.net.Inet4Address

/**
 * Resolves a private IPv4 address on the active Wi-Fi or Ethernet interface.
 * VPN, loopback and mobile interfaces are deliberately excluded.
 */
object LanAddressResolver {
    /**
     * allNetworks is deprecated for general connectivity observation, but it
     * remains the only synchronous way to enumerate Wi-Fi/Ethernet link
     * addresses for a short-lived local server. Replace this when a synchronous
     * replacement is available in the supported API range.
     */
    @Suppress("DEPRECATION")
    fun find(context: Context): Inet4Address? {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        return manager.allNetworks
            .asSequence()
            .mapNotNull { network ->
                val capabilities = manager.getNetworkCapabilities(network) ?: return@mapNotNull null
                val lanTransport =
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                // The emulator exposes its NAT network as cellular/eth0. Keep a
                // debug-only fallback so pairing can be exercised there; release
                // builds require a real Wi-Fi/Ethernet LAN.
                val debugEmulatorFallback =
                    BuildConfig.DEBUG && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                if (!lanTransport && !debugEmulatorFallback) {
                    return@mapNotNull null
                }
                manager.getLinkProperties(network)
                    ?.linkAddresses
                    ?.asSequence()
                    ?.map { it.address }
                    ?.filterIsInstance<Inet4Address>()
                    ?.firstOrNull { address ->
                        !address.isLoopbackAddress &&
                            !address.isLinkLocalAddress &&
                            address.isSiteLocalAddress
                    }
            }
            .firstOrNull()
    }
}
