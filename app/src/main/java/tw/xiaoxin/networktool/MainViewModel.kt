package tw.xiaoxin.networktool

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tw.xiaoxin.networktool.model.NetworkReport
import tw.xiaoxin.networktool.model.ProbeResult
import tw.xiaoxin.networktool.net.FastlyDiagnostics
import tw.xiaoxin.networktool.net.NetworkDiagnostics

class MainViewModel(application: Application) : AndroidViewModel(application) {
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
        _checking.value = true
        _report.value = diagnostics.run()
        _checking.value = false
    }

    fun checkFastly() = viewModelScope.launch {
        _fastlyChecking.value = true
        _fastly.value = FastlyDiagnostics.run()
        _fastlyChecking.value = false
    }
}
