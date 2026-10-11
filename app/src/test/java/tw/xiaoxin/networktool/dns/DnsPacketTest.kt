package tw.xiaoxin.networktool.dns

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class DnsPacketTest {
    @Test fun parsesValidIpv4UdpDnsPacket() {
        val packet = queryPacket(byteArrayOf(1, 2, 3, 4))
        val parsed = DnsPacket.parse(packet, packet.size)
        assertNotNull(parsed)
        val result = parsed!!
        assertEquals(12_345, result.sourcePort)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), result.payload)
    }

    @Test fun rejectsTruncatedPacket() {
        val packet = queryPacket(byteArrayOf(1, 2, 3, 4))
        assertNull(DnsPacket.parse(packet, packet.size - 1))
    }

    @Test fun rejectsFragmentedPacket() {
        val packet = queryPacket(byteArrayOf(1))
        put16(packet, 6, 0x2000)
        assertNull(DnsPacket.parse(packet, packet.size))
    }

    @Test fun rejectsUdpLengthBeyondIpPacket() {
        val packet = queryPacket(byteArrayOf(1))
        put16(packet, 24, 100)
        assertNull(DnsPacket.parse(packet, packet.size))
    }

    @Test fun replySwapsAddressesAndTargetsOriginalSourcePort() {
        val request = DnsPacket.parse(queryPacket(byteArrayOf(1)), 29)!!
        val response = byteArrayOf(9, 8, 7)
        val reply = DnsPacket.reply(request, response)

        assertEquals(31, reply.size)
        assertArrayEquals(byteArrayOf(10, 77, 0, 2), reply.copyOfRange(12, 16))
        assertArrayEquals(byteArrayOf(192.toByte(), 168.toByte(), 1, 2), reply.copyOfRange(16, 20))
        assertEquals(53, u16(reply, 20))
        assertEquals(12_345, u16(reply, 22))
        assertArrayEquals(response, reply.copyOfRange(28, reply.size))
    }

    private fun queryPacket(payload: ByteArray): ByteArray {
        val packet = ByteArray(28 + payload.size)
        packet[0] = 0x45
        put16(packet, 2, packet.size)
        packet[8] = 64
        packet[9] = 17
        byteArrayOf(192.toByte(), 168.toByte(), 1, 2).copyInto(packet, 12)
        byteArrayOf(10, 77, 0, 2).copyInto(packet, 16)
        put16(packet, 20, 12_345)
        put16(packet, 22, 53)
        put16(packet, 24, 8 + payload.size)
        payload.copyInto(packet, 28)
        return packet
    }

    private fun put16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value ushr 8).toByte()
        data[offset + 1] = value.toByte()
    }

    private fun u16(data: ByteArray, offset: Int) =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
}
