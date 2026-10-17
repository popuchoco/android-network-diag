package tw.xiaoxin.networktool.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceStateTest {
    @Test fun dnsVerificationCanBePublishedAndCleared() {
        ServiceState.setDnsVerification("已驗證解析可用")
        assertEquals("已驗證解析可用", ServiceState.dnsVerification.value)
        ServiceState.setDnsVerification(null)
        assertNull(ServiceState.dnsVerification.value)
    }

    @Test
    fun consumingCurrentDnsErrorClearsIt() {
        ServiceState.reportDnsError("failure")

        ServiceState.consumeDnsError("failure")

        assertNull(ServiceState.dnsError.value)
    }

    @Test
    fun consumingOlderDnsErrorDoesNotClearNewerError() {
        ServiceState.reportDnsError("older")
        ServiceState.reportDnsError("newer")

        ServiceState.consumeDnsError("older")

        assertEquals("newer", ServiceState.dnsError.value)
        ServiceState.consumeDnsError("newer")
    }
}
