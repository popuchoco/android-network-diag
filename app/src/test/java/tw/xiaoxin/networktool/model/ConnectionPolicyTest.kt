package tw.xiaoxin.networktool.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionPolicyTest {
    @Test fun airplaneModeWithWifiOrVpnStillRunsChecks() {
        assertFalse(ConnectionPolicy.shouldSkipExternalChecks(airplaneMode = true, hasActiveNetwork = true))
    }

    @Test fun airplaneModeWithoutAnyNetworkSkipsChecks() {
        assertTrue(ConnectionPolicy.shouldSkipExternalChecks(airplaneMode = true, hasActiveNetwork = false))
    }

    @Test fun ordinaryOfflineStateStillRunsDiagnostics() {
        assertFalse(ConnectionPolicy.shouldSkipExternalChecks(airplaneMode = false, hasActiveNetwork = false))
    }
}
