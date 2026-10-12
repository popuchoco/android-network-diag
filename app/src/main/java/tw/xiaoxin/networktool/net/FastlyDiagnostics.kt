package tw.xiaoxin.networktool.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tw.xiaoxin.networktool.model.CheckState
import tw.xiaoxin.networktool.model.ProbeResult
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object FastlyDiagnostics {
    private val services = listOf(
        "GitHub Raw" to "https://raw.githubusercontent.com/github/gitignore/main/README.md",
        "X 圖片" to "https://pbs.twimg.com/robots.txt",
        "Twitch 靜態資源" to "https://static-cdn.jtvnw.net/robots.txt",
    )

    suspend fun run(): List<ProbeResult> = withContext(Dispatchers.IO) {
        services.map { (name, url) ->
            val start = System.nanoTime()
            runCatching {
                val host = URL(url).host
                val ip = InetAddress.getAllByName(host).first().hostAddress
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 6_000; c.readTimeout = 6_000; c.instanceFollowRedirects = false
                c.setRequestProperty("Range", "bytes=0-4095")
                val code = c.responseCode
                runCatching { c.inputStream.use { it.read(ByteArray(4096)) } }
                c.disconnect()
                val ms = (System.nanoTime() - start) / 1_000_000
                ProbeResult(name, if (code in 200..399) CheckState.Success else CheckState.Warning, "$ip · HTTP $code", ms)
            }.getOrElse { ProbeResult(name, CheckState.Failed, it.localizedMessage ?: "測試失敗") }
        }
    }
}
