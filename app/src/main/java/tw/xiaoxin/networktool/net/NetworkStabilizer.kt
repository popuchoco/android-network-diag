package tw.xiaoxin.networktool.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import kotlinx.coroutines.delay

/** Waits briefly for Android's active-network view to stop changing before comparable probes run. */
class NetworkStabilizer(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    suspend fun awaitStable(
        intervalMs: Long = 400,
        requiredMatches: Int = 3,
        timeoutMs: Long = 4_000,
    ): Long {
        val started = SystemClock.elapsedRealtime()
        var previous: String? = null
        var matches = 0
        while (SystemClock.elapsedRealtime() - started < timeoutMs) {
            val signature = signature()
            matches = if (signature == previous && signature != null) matches + 1 else 1
            previous = signature
            if (matches >= requiredMatches) break
            delay(intervalMs)
        }
        return SystemClock.elapsedRealtime() - started
    }

    private fun signature(): String? {
        val network = connectivity.activeNetwork ?: return null
        val caps = connectivity.getNetworkCapabilities(network) ?: return null
        val transports = listOf(
            NetworkCapabilities.TRANSPORT_WIFI,
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_VPN,
            NetworkCapabilities.TRANSPORT_ETHERNET,
            NetworkCapabilities.TRANSPORT_BLUETOOTH,
        ).filter(caps::hasTransport)
        return buildString {
            append(network)
            append('|')
            append(transports.joinToString(","))
            append('|')
            append(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            append('|')
            append(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL))
        }
    }
}
