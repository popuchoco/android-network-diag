package tw.xiaoxin.networktool.dns

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import tw.xiaoxin.networktool.MainActivity
import tw.xiaoxin.networktool.log.DiagnosticLog
import tw.xiaoxin.networktool.model.ServiceState
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class DnsVpnService : VpnService() {
    private val vpnLock = Any()
    private var tun: ParcelFileDescriptor? = null
    private var executor = Executors.newSingleThreadExecutor()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            DiagnosticLog.info(this, "DNS", "DNS-only VPN 已停止")
            stopVpn()
            return Service.START_NOT_STICKY
        }
        return try {
            val upstream = intent?.getStringExtra(EXTRA_DNS) ?: "1.1.1.1"
            startForeground(NOTIFICATION_ID, notification(upstream))
            if (startVpn(upstream)) {
                ServiceState.setDnsUpstream(upstream)
                ServiceState.setDnsVerification("驗證中…")
                DiagnosticLog.info(this, "DNS", "DNS-only VPN 已啟動，上游 $upstream")
                verifyDnsPath(upstream)
                Service.START_STICKY
            } else {
                DiagnosticLog.error(this, "DNS", "系統未建立 VPN 介面")
                stopVpn()
                Service.START_NOT_STICKY
            }
        } catch (error: Exception) {
            val message = "DNS 啟動失敗：${error.javaClass.simpleName}: ${error.message ?: "無詳細訊息"}"
            DiagnosticLog.error(this, "DNS", message)
            ServiceState.reportDnsError(message)
            cleanupVpn()
            stopSelf()
            Service.START_NOT_STICKY
        }
    }

    private fun startVpn(upstream: String): Boolean {
        val previous = synchronized(vpnLock) { tun.also { tun = null } }
        runCatching { previous?.close() }
        val builder = Builder().setSession("網路工具箱 DNS").setMtu(1500)
            .addAddress(VPN_ADDRESS, 24).addDnsServer(VPN_DNS).addRoute(VPN_DNS, 32)
        if (Build.VERSION.SDK_INT >= 29) builder.setBlocking(true)
        val descriptor = runCatching { builder.establish() }.getOrElse {
            DiagnosticLog.error(this, "DNS", "建立 VPN 介面失敗：${it.javaClass.simpleName}: ${it.message ?: "無詳細訊息"}")
            null
        }
        if (descriptor == null) {
            ServiceState.reportDnsError("無法建立 DNS VPN；請確認 VPN 權限，並關閉其他正在使用的 VPN")
            return false
        }
        synchronized(vpnLock) { tun = descriptor }
        if (executor.isShutdown) executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val failure = runCatching { forwardDns(descriptor, upstream) }.exceptionOrNull()
            val wasCurrent = synchronized(vpnLock) {
                if (tun === descriptor) {
                    tun = null
                    true
                } else false
            }
            runCatching { descriptor.close() }
            if (wasCurrent) {
                val message = failure?.let {
                    "DNS 轉送失敗：${it.javaClass.simpleName}: ${it.message ?: "無詳細訊息"}"
                } ?: "DNS VPN 介面已意外結束"
                DiagnosticLog.error(this, "DNS", message)
                ServiceState.reportDnsError(message)
                ServiceState.setDnsUpstream(null)
                runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                stopSelf()
            }
        }
        return true
    }

    private fun forwardDns(descriptor: ParcelFileDescriptor, upstream: String) {
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val packet = ByteArray(32767)
        while (!Thread.interrupted()) {
            val length = input.read(packet)
            if (length < 0) break
            if (length < 28) continue
            val request = DnsPacket.parse(packet, length) ?: continue
            val response = runCatching {
                DatagramSocket().use { socket ->
                    protect(socket); socket.soTimeout = 5_000
                    socket.send(DatagramPacket(request.payload, request.payload.size, InetAddress.getByName(upstream), 53))
                    val data = ByteArray(4096)
                    val reply = DatagramPacket(data, data.size)
                    socket.receive(reply)
                    data.copyOf(reply.length)
                }
            }.getOrNull() ?: continue
            runCatching { output.write(DnsPacket.reply(request, response)) }
        }
    }

    private fun verifyDnsPath(upstream: String) {
        thread(name = "dns-path-verification", isDaemon = true) {
            runCatching {
                Thread.sleep(300)
                InetAddress.getAllByName("hinet.net").firstOrNull()?.hostAddress
                    ?: error("解析未回傳位址")
            }.onSuccess { address ->
                if (ServiceState.dnsUpstream.value == upstream) {
                    val status = "已驗證解析可用 · $address"
                    ServiceState.setDnsVerification(status)
                    DiagnosticLog.info(this, "DNS", "$upstream $status")
                }
            }.onFailure { error ->
                if (ServiceState.dnsUpstream.value == upstream) {
                    val status = "解析驗證失敗：${error.message ?: error.javaClass.simpleName}"
                    ServiceState.setDnsVerification(status)
                    DiagnosticLog.warning(this, "DNS", "$upstream $status")
                }
            }
        }
    }

    private fun notification(upstream: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, "DNS 服務", NotificationManager.IMPORTANCE_LOW))
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("DNS 已切換")
            .setContentText("目前使用 $upstream").setContentIntent(pending).setOngoing(true).build()
    }

    private fun cleanupVpn() {
        ServiceState.setDnsUpstream(null)
        ServiceState.setDnsVerification(null)
        val descriptor = synchronized(vpnLock) { tun.also { tun = null } }
        runCatching { descriptor?.close() }
        executor.shutdownNow()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
    }

    private fun stopVpn() {
        cleanupVpn()
        stopSelf()
    }

    override fun onDestroy() { cleanupVpn(); super.onDestroy() }

    companion object {
        const val ACTION_START = "tw.xiaoxin.networktool.START_DNS"
        const val ACTION_STOP = "tw.xiaoxin.networktool.STOP_DNS"
        const val EXTRA_DNS = "dns"
        private const val VPN_ADDRESS = "10.77.0.1"
        private const val VPN_DNS = "10.77.0.2"
        private const val CHANNEL = "dns_vpn"
        private const val NOTIFICATION_ID = 77
    }
}

internal data class DnsPacket(val source: ByteArray, val destination: ByteArray, val sourcePort: Int, val payload: ByteArray) {
    companion object {
        fun parse(packet: ByteArray, length: Int): DnsPacket? {
            if (length !in 28..packet.size) return null
            if (((packet[0].toInt() ushr 4) and 0xF) != 4) return null
            val ihl = (packet[0].toInt() and 0xF) * 4
            if (ihl < 20 || length < ihl + 8 || u16(packet, 2) !in (ihl + 8)..length) return null
            if ((u16(packet, 6) and 0x3FFF) != 0) return null
            if ((packet[9].toInt() and 0xFF) != 17 || u16(packet, ihl + 2) != 53) return null
            val udpLength = u16(packet, ihl + 4)
            if (udpLength < 8 || ihl + udpLength > u16(packet, 2)) return null
            val payloadEnd = ihl + udpLength
            return DnsPacket(packet.copyOfRange(12, 16), packet.copyOfRange(16, 20), u16(packet, ihl), packet.copyOfRange(ihl + 8, payloadEnd))
        }

        fun reply(request: DnsPacket, payload: ByteArray): ByteArray {
            val total = 28 + payload.size
            val out = ByteArray(total)
            out[0] = 0x45; put16(out, 2, total); out[8] = 64; out[9] = 17
            request.destination.copyInto(out, 12); request.source.copyInto(out, 16)
            put16(out, 10, checksum(out, 0, 20))
            put16(out, 20, 53); put16(out, 22, request.sourcePort); put16(out, 24, 8 + payload.size)
            payload.copyInto(out, 28)
            val pseudo = ByteArray(12 + 8 + payload.size)
            request.destination.copyInto(pseudo, 0); request.source.copyInto(pseudo, 4)
            pseudo[9] = 17; put16(pseudo, 10, 8 + payload.size); out.copyInto(pseudo, 12, 20, total)
            val udp = checksum(pseudo, 0, pseudo.size).let { if (it == 0) 0xFFFF else it }
            put16(out, 26, udp)
            return out
        }

        private fun u16(data: ByteArray, offset: Int) = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        private fun put16(data: ByteArray, offset: Int, value: Int) { data[offset] = (value ushr 8).toByte(); data[offset + 1] = value.toByte() }
        private fun checksum(data: ByteArray, offset: Int, size: Int): Int {
            var sum = 0L; var i = offset
            while (i + 1 < offset + size) { sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF); i += 2 }
            if (i < offset + size) sum += (data[i].toInt() and 0xFF) shl 8
            while ((sum ushr 16) != 0L) sum = (sum and 0xFFFF) + (sum ushr 16)
            return sum.inv().toInt() and 0xFFFF
        }
    }
}
