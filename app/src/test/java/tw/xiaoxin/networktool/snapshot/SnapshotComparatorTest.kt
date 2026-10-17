package tw.xiaoxin.networktool.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test fun readableEvidenceKeepsAnnotationAndRedactsAddressesByDefault() {
        val before = snapshot(listOf(SnapshotProbe("DNS", "Success", "resolved 192.168.1.53", 20)))
            .copy(label = "變更前", annotation = "尚未開啟 VPN")
        val after = before.copy(label = "變更後", annotation = "開啟 Tailscale", publicIp = "198.51.100.2")

        val text = SnapshotCodec.evidenceText(SnapshotComparator.compare(before, after), includeAddresses = false)

        assertTrue(text.contains("開啟 Tailscale"))
        assertTrue(text.contains("[已隱藏]"))
        assertFalse(text.contains("198.51.100.2"))
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
