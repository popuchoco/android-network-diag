package tw.xiaoxin.networktool

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tw.xiaoxin.networktool.model.NetworkReport
import tw.xiaoxin.networktool.model.ProbeResult
import tw.xiaoxin.networktool.log.DiagnosticLog
import tw.xiaoxin.networktool.net.FastlyDiagnostics
import tw.xiaoxin.networktool.net.NetworkDiagnostics
import tw.xiaoxin.networktool.net.NetworkStabilizer
import tw.xiaoxin.networktool.model.ServiceState
import tw.xiaoxin.networktool.snapshot.DiagnosticSnapshot
import tw.xiaoxin.networktool.snapshot.SnapshotCodec
import tw.xiaoxin.networktool.snapshot.SnapshotComparator
import tw.xiaoxin.networktool.snapshot.SnapshotComparison
import tw.xiaoxin.networktool.snapshot.SnapshotDatabase
import tw.xiaoxin.networktool.snapshot.SnapshotFactory

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val diagnostics = NetworkDiagnostics(application)
    private val stabilizer = NetworkStabilizer(application)
    private val _report = MutableStateFlow(NetworkReport())
    val report = _report.asStateFlow()
    private val _checking = MutableStateFlow(false)
    val checking = _checking.asStateFlow()
    private val _fastly = MutableStateFlow<List<ProbeResult>>(emptyList())
    val fastly = _fastly.asStateFlow()
    private val _fastlyChecking = MutableStateFlow(false)
    val fastlyChecking = _fastlyChecking.asStateFlow()
    private val snapshotDao = SnapshotDatabase.get(application).snapshots()
    private val _baseline = MutableStateFlow<DiagnosticSnapshot?>(null)
    val baseline = _baseline.asStateFlow()
    private val _comparison = MutableStateFlow<SnapshotComparison?>(null)
    val comparison = _comparison.asStateFlow()
    private val _changeLabRunning = MutableStateFlow(false)
    val changeLabRunning = _changeLabRunning.asStateFlow()

    init {
        viewModelScope.launch {
            _baseline.value = snapshotDao.latestBaseline()?.let(SnapshotCodec::fromEntity)
        }
    }

    fun checkNetwork() = viewModelScope.launch {
        DiagnosticLog.info(app, "Health", "開始外網分層檢查")
        _checking.value = true
        try {
            val result = diagnostics.run()
            _report.value = result
            val passed = result.probes.count { it.state == tw.xiaoxin.networktool.model.CheckState.Success }
            DiagnosticLog.info(app, "Health", "檢查完成：$passed/${result.probes.size} 項通過，${result.transport}，IP ${result.publicIp}")
        } finally {
            _checking.value = false
        }
    }

    fun checkFastly() = viewModelScope.launch {
        DiagnosticLog.info(app, "Fastly", "開始服務路線測試")
        _fastlyChecking.value = true
        try {
            val result = FastlyDiagnostics.run()
            _fastly.value = result
            val passed = result.count { it.state == tw.xiaoxin.networktool.model.CheckState.Success }
            DiagnosticLog.info(app, "Fastly", "路線測試完成：$passed/${result.size} 項通過")
        } finally {
            _fastlyChecking.value = false
        }
    }

    fun captureBaseline(annotation: String = "") = viewModelScope.launch {
        _changeLabRunning.value = true
        DiagnosticLog.info(app, "ChangeLab", "開始建立變更前基準")
        try {
            val snapshot = collectSnapshot("變更前", annotation)
            _baseline.value = save(snapshot)
            _comparison.value = null
            DiagnosticLog.info(app, "ChangeLab", "已建立基準：${snapshot.transport}，${snapshot.probes.count { it.state == "Success" }}/${snapshot.probes.size} 項通過")
        } finally {
            _changeLabRunning.value = false
        }
    }

    fun compareWithBaseline(annotation: String = "") = viewModelScope.launch {
        val before = _baseline.value ?: return@launch
        _changeLabRunning.value = true
        DiagnosticLog.info(app, "ChangeLab", "開始重測並比較變更")
        try {
            val after = save(collectSnapshot("變更後", annotation))
            _comparison.value = SnapshotComparator.compare(before, after)
            DiagnosticLog.info(app, "ChangeLab", _comparison.value!!.summary)
        } finally {
            _changeLabRunning.value = false
        }
    }

    fun evidenceJson(includeAddresses: Boolean): String? =
        _comparison.value?.let { SnapshotCodec.evidenceJson(it, includeAddresses) }

    fun evidenceText(includeAddresses: Boolean): String? =
        _comparison.value?.let { SnapshotCodec.evidenceText(it, includeAddresses) }

    private suspend fun collectSnapshot(label: String, annotation: String): DiagnosticSnapshot {
        DiagnosticLog.info(app, "ChangeLab", "等待網路狀態穩定")
        val stabilizationMs = stabilizer.awaitStable()
        val result = diagnostics.run()
        val fastlyResult = FastlyDiagnostics.run()
        _report.value = result
        _fastly.value = fastlyResult
        return SnapshotFactory.create(
            label = label,
            report = result,
            fastly = fastlyResult,
            dnsUpstream = ServiceState.dnsUpstream.value,
            annotation = annotation,
            stabilizationMs = stabilizationMs,
        )
    }

    private suspend fun save(snapshot: DiagnosticSnapshot): DiagnosticSnapshot {
        val id = snapshotDao.insert(SnapshotCodec.toEntity(snapshot))
        snapshotDao.trim()
        return snapshot.copy(id = id)
    }
}
