package tw.xiaoxin.networktool.net

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import tw.xiaoxin.networktool.model.ConnectionExplanation
import tw.xiaoxin.networktool.model.ServiceState
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

class ConnectionExplainer(private val context: Context) {
    fun explain(): ConnectionExplanation {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val active = cm.activeNetwork
        val caps = active?.let(cm::getNetworkCapabilities)
        val airplaneMode = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
        if (airplaneMode) {
            return ConnectionExplanation(
                logicalTransport = "飛航模式",
                explanation = "目前為飛航模式，已暫停外部網路檢查",
                airplaneMode = true,
            )
        }

        val logical = transportName(caps)
        val physical = if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
            selectUnderlyingNetwork(cm, active)
        } else active
        val physicalCaps = physical?.let(cm::getNetworkCapabilities)
        val physicalLink = physical?.let(cm::getLinkProperties)
        val addresses = physicalLink?.linkAddresses.orEmpty().mapNotNull { it.address.hostAddress }
        val gateway = physicalLink?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
        val cellular = cellularSummary(cm)
        val underlying = if (physical != active) transportName(physicalCaps).takeUnless { it == "未連線" || it == "其他網路" } else null
        val ownDns = logical == "VPN" && ServiceState.dnsUpstream.value != null
        val path = when {
            ownDns && underlying != null -> "DNS-only VPN（底層：$underlying）"
            logical == "VPN" && underlying != null -> "外部 VPN（底層：$underlying）"
            else -> logical
        }
        val explanation = when {
            caps == null -> "目前沒有可用網路"
            logical == "Wi‑Fi" && cellular != "偵測到可用行動網路" -> "目前透過 Wi‑Fi 連線；$cellular"
            else -> "目前透過 $path 連線"
        }
        return ConnectionExplanation(
            logicalTransport = logical,
            underlyingTransport = underlying,
            isOwnDnsVpn = ownDns,
            localAddresses = addresses,
            gateway = gateway,
            hasPrivateAddress = addresses.mapNotNull(::parseAddress).any(::isPrivateAddress),
            hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            isCaptivePortal = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true,
            cellularSummary = cellular,
            explanation = explanation,
        )
    }

    private fun selectUnderlyingNetwork(cm: ConnectivityManager, active: Network?): Network? =
        cm.allNetworks.asSequence()
            .filter { it != active }
            .mapNotNull { network -> cm.getNetworkCapabilities(network)?.let { network to it } }
            .filterNot { (_, caps) -> caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }
            .sortedByDescending { (_, caps) ->
                when {
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> 2
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> 1
                    else -> 0
                }
            }.firstOrNull()?.first

    private fun cellularSummary(cm: ConnectivityManager): String {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return "此裝置無行動網路硬體"
        }
        val cellularCaps = cm.allNetworks.mapNotNull(cm::getNetworkCapabilities)
            .firstOrNull { it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) }
            ?: return "未偵測到可用行動網路"
        return when {
            cellularCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> "偵測到可用行動網路"
            cellularCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "行動網路尚未通過網際網路驗證"
            else -> "行動網路目前無網際網路能力"
        }
    }

    private fun transportName(caps: NetworkCapabilities?): String = when {
        caps == null -> "未連線"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "行動網路"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "乙太網路"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "藍牙網路"
        else -> "其他網路"
    }

    private fun parseAddress(value: String): InetAddress? = runCatching {
        InetAddress.getByName(value.substringBefore('%'))
    }.getOrNull()

    private fun isPrivateAddress(address: InetAddress): Boolean = when (address) {
        is Inet4Address -> address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress
        is Inet6Address -> address.isSiteLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            (address.address.first().toInt() and 0xFE) == 0xFC
        else -> false
    }
}
