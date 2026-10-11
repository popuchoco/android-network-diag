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

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val diagnostics = NetworkDiagnostics(application)
    private val _report = MutableStateFlow(NetworkReport())
    val report = _report.asStateFlow()
    private val _checking = MutableStateFlow(false)
    val checking = _checking.asStateFlow()
    private val _fastly = MutableStateFlow<List<ProbeResult>>(emptyList())
    val fastly = _fastly.asStateFlow()
    private val _fastlyChecking = MutableStateFlow(false)
    val fastlyChecking = _fastlyChecking.asStateFlow()

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
}
