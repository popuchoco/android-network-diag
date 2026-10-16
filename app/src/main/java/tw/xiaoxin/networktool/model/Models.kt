package tw.xiaoxin.networktool.model

enum class CheckState { Idle, Running, Success, Warning, Failed }
data class ProbeResult(val name: String, val state: CheckState, val detail: String, val latencyMs: Long? = null)
data class ConnectionExplanation(
    val logicalTransport: String = "尚未檢查",
    val underlyingTransport: String? = null,
    val isOwnDnsVpn: Boolean = false,
    val vpnInterface: String? = null,
    val vpnAppCandidates: List<String> = emptyList(),
    val hasActiveNetwork: Boolean = false,
    val localAddresses: List<String> = emptyList(),
    val gateway: String? = null,
    val hasPrivateAddress: Boolean = false,
    val hasInternet: Boolean = false,
    val isValidated: Boolean = false,
    val isCaptivePortal: Boolean = false,
    val cellularSummary: String? = null,
    val explanation: String = "尚未檢查連線來源",
    val airplaneMode: Boolean = false,
)

object ConnectionPolicy {
    fun shouldSkipExternalChecks(airplaneMode: Boolean, hasActiveNetwork: Boolean) =
        airplaneMode && !hasActiveNetwork
}
data class NetworkReport(
    val transport: String = "尚未檢查",
    val validated: Boolean = false,
    val publicIp: String = "—",
    val probes: List<ProbeResult> = emptyList(),
    val checkedAt: Long? = null,
    val connection: ConnectionExplanation? = null,
)
data class DnsProvider(val id: String, val name: String, val primary: String, val secondary: String, val privateDnsHost: String)
val dnsProviders = listOf(
    DnsProvider("cloudflare", "Cloudflare", "1.1.1.1", "1.0.0.1", "one.one.one.one"),
    DnsProvider("google", "Google", "8.8.8.8", "8.8.4.4", "dns.google"),
    DnsProvider("quad9", "Quad9", "9.9.9.9", "149.112.112.112", "dns.quad9.net"),
)

fun dnsProviderById(id: String): DnsProvider = dnsProviders.firstOrNull { it.id == id } ?: dnsProviders.first()
