package tw.xiaoxin.networktool.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tw.xiaoxin.networktool.model.CheckState
import tw.xiaoxin.networktool.model.NetworkReport
import tw.xiaoxin.networktool.model.ProbeResult
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

class NetworkDiagnostics(private val context: Context) {
    suspend fun run(): NetworkReport = coroutineScope {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let(cm::getNetworkCapabilities)
        val transport = when {
            caps == null -> "未連線"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "行動網路"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "乙太網路"
            else -> "其他網路"
        }
        val probes = listOf(
            async { dnsProbe() },
            async { httpProbe("Google 連線驗證", "https://connectivitycheck.gstatic.com/generate_204", setOf(204)) },
            async { httpProbe("Cloudflare 連線驗證", "https://cp.cloudflare.com/generate_204", setOf(204)) },
            async { httpProbe("Microsoft 連線驗證", "https://www.msftconnecttest.com/connecttest.txt", 200..399) },
        ).awaitAll()
        val publicIp = withContext(Dispatchers.IO) {
            runCatching { URL("https://api64.ipify.org").readText().trim() }.getOrDefault("無法取得")
        }
        NetworkReport(transport, caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true, publicIp, probes, System.currentTimeMillis())
    }

    private suspend fun dnsProbe() = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        runCatching { InetAddress.getAllByName("www.hinet.net") }.fold(
            { ProbeResult("DNS 解析", CheckState.Success, "www.hinet.net → ${it.first().hostAddress}", elapsed(start)) },
            { ProbeResult("DNS 解析", CheckState.Failed, it.localizedMessage ?: "解析失敗") },
        )
    }

    private suspend fun httpProbe(name: String, url: String, accepted: Iterable<Int>) = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 5_000; c.readTimeout = 5_000; c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "NetworkTool/0.1")
            val code = c.responseCode
            c.disconnect()
            ProbeResult(name, if (code in accepted) CheckState.Success else CheckState.Warning, "HTTP $code", elapsed(start))
        }.getOrElse { ProbeResult(name, CheckState.Failed, it.localizedMessage ?: "連線失敗") }
    }

    private fun elapsed(start: Long) = (System.nanoTime() - start) / 1_000_000
}

private operator fun Iterable<Int>.contains(value: Int) = any { it == value }
