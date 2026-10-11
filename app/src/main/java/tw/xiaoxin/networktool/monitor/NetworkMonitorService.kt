package tw.xiaoxin.networktool.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import tw.xiaoxin.networktool.MainActivity
import tw.xiaoxin.networktool.model.CheckState
import tw.xiaoxin.networktool.model.ServiceState
import tw.xiaoxin.networktool.net.NetworkDiagnostics
import tw.xiaoxin.networktool.log.DiagnosticLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NetworkMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var firstFailure = false
    private var monitorJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { DiagnosticLog.info(this, "Monitor", "持續監控已停止"); ServiceState.setMonitoring(false); stopSelf(); return START_NOT_STICKY }
        createChannel()
        startForeground(NOTIFICATION_ID, notification("準備第一次檢查", false))
        if (monitorJob?.isActive != true) monitorJob = scope.launch { monitorLoop() }
        ServiceState.setMonitoring(true)
        DiagnosticLog.info(this, "Monitor", "持續監控已啟動，間隔 60 秒")
        return START_STICKY
    }

    private suspend fun monitorLoop() {
        val diagnostics = NetworkDiagnostics(this)
        while (currentCoroutineContext().isActive) {
            val report = diagnostics.run()
            val online = report.probes.any { it.state == CheckState.Success }
            if (online) {
                firstFailure = false
                save("ONLINE", report.probes.mapNotNull { it.latencyMs }.average().takeIf { !it.isNaN() })
                notify("連線正常 · ${report.transport}", false)
                delay(INTERVAL_MS)
            } else if (!firstFailure) {
                firstFailure = true
                DiagnosticLog.warning(this, "Monitor", "首次偵測不到外網，5 秒後複查")
                notify("連線異常，5 秒後複查", true)
                delay(RETRY_MS)
            } else {
                firstFailure = false
                save("OFFLINE", null)
                DiagnosticLog.error(this, "Monitor", "連續兩次檢查失敗，確認斷線")
                notify("已確認無法對外連線", true)
                delay(INTERVAL_MS)
            }
        }
    }

    private fun save(status: String, latency: Double?) {
        val file = File(filesDir, "network_history.csv")
        if (!file.exists()) file.writeText("timestamp,status,latency_ms\n")
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
        file.appendText("$now,$status,${latency?.let { "%.1f".format(Locale.US, it) } ?: ""}\n")
    }

    private fun createChannel() = getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(CHANNEL, "外網監控", NotificationManager.IMPORTANCE_LOW)
    )

    private fun notify(text: String, alert: Boolean) = getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text, alert))
    private fun notification(text: String, alert: Boolean): Notification {
        val pending = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL).setSmallIcon(if (alert) android.R.drawable.stat_notify_error else android.R.drawable.stat_sys_download_done)
            .setContentTitle("網路工具箱").setContentText(text).setContentIntent(pending).setOngoing(true).build()
    }

    override fun onDestroy() { ServiceState.setMonitoring(false); scope.cancel(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "tw.xiaoxin.networktool.MONITOR_START"
        const val ACTION_STOP = "tw.xiaoxin.networktool.MONITOR_STOP"
        private const val CHANNEL = "network_monitor"
        private const val NOTIFICATION_ID = 41
        private const val INTERVAL_MS = 60_000L
        private const val RETRY_MS = 5_000L
    }
}
