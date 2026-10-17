package tw.xiaoxin.networktool.snapshot

import org.json.JSONArray
import org.json.JSONObject
import tw.xiaoxin.networktool.model.CheckState
import tw.xiaoxin.networktool.model.NetworkReport
import tw.xiaoxin.networktool.model.ProbeResult

data class SnapshotProbe(
    val name: String,
    val state: String,
    val detail: String,
    val latencyMs: Long?,
)

data class DiagnosticSnapshot(
    val id: Long = 0,
    val schemaVersion: Int = 2,
    val capturedAt: Long,
    val label: String,
    val transport: String,
    val underlyingTransport: String?,
    val publicIp: String,
    val validated: Boolean,
    val airplaneMode: Boolean,
    val externalTunnel: Boolean,
    val localAddresses: List<String>,
    val gateway: String?,
    val dnsUpstream: String?,
    val annotation: String? = null,
    val stabilizationMs: Long = 0,
    val probes: List<SnapshotProbe>,
    val fastly: List<SnapshotProbe>,
)

data class SnapshotChange(val title: String, val before: String, val after: String, val improved: Boolean? = null)

data class SnapshotComparison(
    val before: DiagnosticSnapshot,
    val after: DiagnosticSnapshot,
    val summary: String,
    val changes: List<SnapshotChange>,
)

object SnapshotFactory {
    fun create(
        label: String,
        report: NetworkReport,
        fastly: List<ProbeResult>,
        dnsUpstream: String?,
        annotation: String? = null,
        stabilizationMs: Long = 0,
    ): DiagnosticSnapshot {
        val connection = report.connection
        return DiagnosticSnapshot(
            capturedAt = report.checkedAt ?: System.currentTimeMillis(),
            label = label,
            transport = report.transport,
            underlyingTransport = connection?.underlyingTransport,
            publicIp = report.publicIp,
            validated = report.validated,
            airplaneMode = connection?.airplaneMode == true,
            externalTunnel = connection?.logicalTransport == "VPN" && connection.isOwnDnsVpn.not(),
            localAddresses = connection?.localAddresses.orEmpty(),
            gateway = connection?.gateway,
            dnsUpstream = dnsUpstream,
            annotation = annotation?.trim()?.takeIf(String::isNotEmpty),
            stabilizationMs = stabilizationMs,
            probes = report.probes.map(::probe),
            fastly = fastly.map(::probe),
        )
    }

    private fun probe(value: ProbeResult) = SnapshotProbe(value.name, value.state.name, value.detail, value.latencyMs)
}

object SnapshotComparator {
    fun compare(before: DiagnosticSnapshot, after: DiagnosticSnapshot): SnapshotComparison {
        val changes = buildList {
            change("連線路徑", path(before), path(after))
            change("公開 IP", before.publicIp, after.publicIp)
            change("DNS 上游", before.dnsUpstream ?: "系統預設", after.dnsUpstream ?: "系統預設")
            change("網路驗證", yesNo(before.validated), yesNo(after.validated), after.validated.compareTo(before.validated))
            change("通過測項", passed(before).toString(), passed(after).toString(), passed(after).compareTo(passed(before)))
            change("Fastly 平均延遲", latency(before.fastly), latency(after.fastly), latencyScore(before.fastly, after.fastly))
        }
        val positive = changes.count { it.improved == true }
        val negative = changes.count { it.improved == false }
        val summary = when {
            changes.isEmpty() -> "本次重測沒有觀察到有意義的差異"
            positive > negative -> "變更後整體有改善；請查看下列證據"
            negative > positive -> "變更後狀況較差；可考慮復原剛才的調整"
            else -> "觀察到路徑或狀態改變，但尚不足以判定改善"
        }
        return SnapshotComparison(before, after, summary, changes)
    }

    private fun MutableList<SnapshotChange>.change(title: String, before: String, after: String, score: Int = 0) {
        if (before != after) add(SnapshotChange(title, before, after, score.takeIf { it != 0 }?.let { it > 0 }))
    }

    private fun path(value: DiagnosticSnapshot) = listOfNotNull(value.transport, value.underlyingTransport).joinToString(" / ")
    private fun yesNo(value: Boolean) = if (value) "已驗證" else "未驗證"
    private fun passed(value: DiagnosticSnapshot) = value.probes.count { it.state == CheckState.Success.name }
    private fun latency(values: List<SnapshotProbe>): String = values.mapNotNull { it.latencyMs }.average()
        .takeIf { !it.isNaN() }?.let { "%.0f ms".format(it) } ?: "無資料"
    private fun latencyScore(before: List<SnapshotProbe>, after: List<SnapshotProbe>): Int {
        val old = before.mapNotNull { it.latencyMs }.average()
        val new = after.mapNotNull { it.latencyMs }.average()
        if (old.isNaN() || new.isNaN() || kotlin.math.abs(old - new) < 20) return 0
        return if (new < old) 1 else -1
    }
}

object SnapshotCodec {
    fun toEntity(value: DiagnosticSnapshot) = SnapshotEntity(
        id = value.id,
        schemaVersion = value.schemaVersion,
        capturedAt = value.capturedAt,
        label = value.label,
        transport = value.transport,
        underlyingTransport = value.underlyingTransport,
        publicIp = value.publicIp,
        validated = value.validated,
        airplaneMode = value.airplaneMode,
        externalTunnel = value.externalTunnel,
        localAddresses = JSONArray(value.localAddresses).toString(),
        gateway = value.gateway,
        dnsUpstream = value.dnsUpstream,
        annotation = value.annotation,
        stabilizationMs = value.stabilizationMs,
        probesJson = probesToJson(value.probes).toString(),
        fastlyJson = probesToJson(value.fastly).toString(),
    )

    fun fromEntity(value: SnapshotEntity) = DiagnosticSnapshot(
        id = value.id,
        schemaVersion = value.schemaVersion,
        capturedAt = value.capturedAt,
        label = value.label,
        transport = value.transport,
        underlyingTransport = value.underlyingTransport,
        publicIp = value.publicIp,
        validated = value.validated,
        airplaneMode = value.airplaneMode,
        externalTunnel = value.externalTunnel,
        localAddresses = jsonStrings(JSONArray(value.localAddresses)),
        gateway = value.gateway,
        dnsUpstream = value.dnsUpstream,
        annotation = value.annotation,
        stabilizationMs = value.stabilizationMs,
        probes = probesFromJson(JSONArray(value.probesJson)),
        fastly = probesFromJson(JSONArray(value.fastlyJson)),
    )

    fun evidenceJson(comparison: SnapshotComparison, includeAddresses: Boolean): String = JSONObject().apply {
        put("schemaVersion", 2)
        put("generatedAt", System.currentTimeMillis())
        put("privacy", JSONObject().put("addressesIncluded", includeAddresses))
        put("summary", comparison.summary)
        put("before", snapshotJson(comparison.before, includeAddresses))
        put("after", snapshotJson(comparison.after, includeAddresses))
        put("changes", JSONArray().apply { comparison.changes.forEach { change -> put(JSONObject().apply {
            put("title", change.title); put("before", redact(change.before, includeAddresses)); put("after", redact(change.after, includeAddresses))
            put("improved", change.improved ?: JSONObject.NULL)
        }) } })
    }.toString(2)

    private fun snapshotJson(value: DiagnosticSnapshot, includeAddresses: Boolean) = JSONObject().apply {
        put("capturedAt", value.capturedAt); put("label", value.label); put("transport", value.transport)
        put("underlyingTransport", value.underlyingTransport); put("publicIp", if (includeAddresses) value.publicIp else "[redacted]")
        put("validated", value.validated); put("airplaneMode", value.airplaneMode); put("externalTunnel", value.externalTunnel)
        put("annotation", value.annotation ?: JSONObject.NULL); put("stabilizationMs", value.stabilizationMs)
        put("localAddresses", if (includeAddresses) JSONArray(value.localAddresses) else JSONArray())
        put("gateway", if (includeAddresses) value.gateway else JSONObject.NULL); put("dnsUpstream", value.dnsUpstream)
        put("probes", probesToJson(value.probes, includeAddresses)); put("fastly", probesToJson(value.fastly, includeAddresses))
    }

    private fun probesToJson(values: List<SnapshotProbe>, includeDetails: Boolean = true) = JSONArray().apply {
        values.forEach { value -> put(JSONObject().apply {
            put("name", value.name); put("state", value.state)
            put("detail", if (includeDetails) value.detail else redact(value.detail, false)); put("latencyMs", value.latencyMs ?: JSONObject.NULL)
        }) }
    }

    private fun probesFromJson(array: JSONArray) = (0 until array.length()).map { index ->
        val value = array.getJSONObject(index)
        SnapshotProbe(value.getString("name"), value.getString("state"), value.getString("detail"), value.optLong("latencyMs").takeIf { !value.isNull("latencyMs") })
    }
    private fun jsonStrings(array: JSONArray) = (0 until array.length()).map(array::getString)
    private fun redact(value: String, includeAddresses: Boolean) = if (includeAddresses) value else
        value.replace(Regex("(?<![A-Fa-f0-9:])(?:\\d{1,3}\\.){3}\\d{1,3}(?![A-Fa-f0-9:])"), "[redacted-ipv4]")
            .replace(Regex("(?i)(?:[0-9a-f]{0,4}:){2,}[0-9a-f]{0,4}"), "[redacted-ipv6]")

    fun evidenceText(comparison: SnapshotComparison, includeAddresses: Boolean): String = buildString {
        appendLine("Android Network Diag - Change Lab")
        appendLine("產生時間：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        appendLine("摘要：${comparison.summary}")
        appendLine()
        appendSnapshot("變更前", comparison.before, includeAddresses)
        appendLine()
        appendSnapshot("變更後", comparison.after, includeAddresses)
        appendLine()
        appendLine("觀察到的差異")
        if (comparison.changes.isEmpty()) appendLine("- 沒有觀察到可辨識的差異")
        comparison.changes.forEach { appendLine("- ${it.title}：${redact(it.before, includeAddresses)} → ${redact(it.after, includeAddresses)}") }
    }

    private fun StringBuilder.appendSnapshot(title: String, value: DiagnosticSnapshot, includeAddresses: Boolean) {
        appendLine(title)
        appendLine("- 時間：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(value.capturedAt))}")
        value.annotation?.let { appendLine("- 註記：$it") }
        appendLine("- 路徑：${value.transport}${value.underlyingTransport?.let { " / $it" }.orEmpty()}")
        appendLine("- 公開 IP：${if (includeAddresses) value.publicIp else "[已隱藏]"}")
        appendLine("- DNS：${value.dnsUpstream ?: "系統設定"}")
        appendLine("- Android 驗證：${if (value.validated) "通過" else "未通過"}")
        appendLine("- 穩定等待：${value.stabilizationMs} ms")
        appendLine("- 外網檢查：${value.probes.count { it.state == CheckState.Success.name }}/${value.probes.size} 通過")
    }
}
