package tw.xiaoxin.networktool.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotComparatorTest {
    @Test fun reportsImprovementWhenMoreChecksPass() {
        val before = snapshot(listOf(SnapshotProbe("DNS", "Failed", "timeout", null)))
        val after = snapshot(listOf(SnapshotProbe("DNS", "Success", "ok", 20)))

        val result = SnapshotComparator.compare(before, after)

        assertTrue(result.summary.contains("改善"))
        assertEquals(true, result.changes.first { it.title == "通過測項" }.improved)
    }

    @Test fun ignoresEquivalentSnapshots() {
        val value = snapshot(listOf(SnapshotProbe("DNS", "Success", "ok", 20)))
        assertTrue(SnapshotComparator.compare(value, value).changes.isEmpty())
    }

    private fun snapshot(probes: List<SnapshotProbe>) = DiagnosticSnapshot(
        capturedAt = 1,
        label = "test",
        transport = "Wi-Fi",
        underlyingTransport = null,
        publicIp = "203.0.113.1",
        validated = true,
        airplaneMode = false,
        externalTunnel = false,
        localAddresses = listOf("192.168.1.2"),
        gateway = "192.168.1.1",
        dnsUpstream = null,
        probes = probes,
        fastly = emptyList(),
    )
}
