package tw.xiaoxin.networktool.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceState {
    private val _monitoring = MutableStateFlow(false)
    val monitoring = _monitoring.asStateFlow()

    private val _dnsUpstream = MutableStateFlow<String?>(null)
    val dnsUpstream = _dnsUpstream.asStateFlow()

    fun setMonitoring(active: Boolean) {
        _monitoring.value = active
    }

    fun setDnsUpstream(upstream: String?) {
        _dnsUpstream.value = upstream
    }
}
