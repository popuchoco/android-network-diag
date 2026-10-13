package tw.xiaoxin.networktool.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DnsProviderTest {
    @Test
    fun resolvesProviderFromSaveableId() {
        assertEquals("8.8.8.8", dnsProviderById("google").primary)
    }

    @Test
    fun fallsBackWhenRestoredIdNoLongerExists() {
        assertEquals(dnsProviders.first(), dnsProviderById("removed-provider"))
    }
}
