package tw.xiaoxin.networktool.model

enum class CheckState { Idle, Running, Success, Warning, Failed }
data class ProbeResult(val name: String, val state: CheckState, val detail: String, val latencyMs: Long? = null)
data class NetworkReport(
    val transport: String = "尚未檢查",
    val validated: Boolean = false,
    val publicIp: String = "—",
    val probes: List<ProbeResult> = emptyList(),
    val checkedAt: Long? = null,
)
data class DnsProvider(val id: String, val name: String, val primary: String, val secondary: String, val privateDnsHost: String)
val dnsProviders = listOf(
    DnsProvider("cloudflare", "Cloudflare", "1.1.1.1", "1.0.0.1", "one.one.one.one"),
    DnsProvider("google", "Google", "8.8.8.8", "8.8.4.4", "dns.google"),
    DnsProvider("quad9", "Quad9", "9.9.9.9", "149.112.112.112", "dns.quad9.net"),
)
