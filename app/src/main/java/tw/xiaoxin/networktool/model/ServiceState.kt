package tw.xiaoxin.networktool.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceState {
    private val _monitoring = MutableStateFlow(false)
    val monitoring = _monitoring.asStateFlow()

    private val _dnsUpstream = MutableStateFlow<String?>(null)
    val dnsUpstream = _dnsUpstream.asStateFlow()

    private val _dnsError = MutableStateFlow<String?>(null)
    val dnsError = _dnsError.asStateFlow()

    fun setMonitoring(active: Boolean) {
        _monitoring.value = active
    }

    fun setDnsUpstream(upstream: String?) {
        _dnsUpstream.value = upstream
    }

    fun reportDnsError(message: String) {
        _dnsError.value = message
    }

    fun consumeDnsError(message: String) {
        if (_dnsError.value == message) _dnsError.value = null
    }
}
