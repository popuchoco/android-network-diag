package tw.xiaoxin.networktool.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceStateTest {
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
