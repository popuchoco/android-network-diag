package tw.xiaoxin.networktool

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import tw.xiaoxin.networktool.dns.DnsVpnService
import tw.xiaoxin.networktool.monitor.NetworkMonitorService
import tw.xiaoxin.networktool.model.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        setContent { NetworkToolApp() }
    }
}

private val Ink = Color(0xFFE8EFEA)
private val Moss = Color(0xFF62E6AA)
private val Mint = Color(0xFF19372B)
private val Canvas = Color(0xFF0E1210)
private val Panel = Color(0xFF171D1A)
private val Line = Color(0xFF2C3631)

@Composable
fun NetworkToolApp(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Moss, onPrimary = Color(0xFF082116), background = Canvas, surface = Panel, onSurface = Ink, outline = Line)) {
        Scaffold(
            containerColor = Canvas,
            bottomBar = {
                NavigationBar(containerColor = Panel, tonalElevation = 0.dp) {
                    listOf("健檢" to Icons.Outlined.MonitorHeart, "中華電信" to Icons.Outlined.CellTower, "DNS" to Icons.Outlined.Dns).forEachIndexed { index, item ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(item.second, null) }, label = { Text(item.first) })
                    }
                }
            }
        ) { padding ->
            when (tab) {
                0 -> HealthScreen(vm, Modifier.padding(padding))
                1 -> HinetScreen(vm, Modifier.padding(padding))
                else -> DnsScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun Page(title: String, subtitle: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Ink.copy(alpha = .62f), lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun HealthScreen(vm: MainViewModel, modifier: Modifier) {
    val report by vm.report.collectAsStateWithLifecycle()
    val checking by vm.checking.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var monitoring by rememberSaveable { mutableStateOf(false) }
    Page("外網健檢", "不是只看 Wi‑Fi 圖示，而是實際確認裝置能否連到外部網路。", modifier) {
        Card(colors = CardDefaults.cardColors(containerColor = if (report.validated) Mint else Panel), shape = RoundedCornerShape(12.dp), border = CardDefaults.outlinedCardBorder()) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (report.validated) Icons.Outlined.CheckCircle else Icons.Outlined.WifiFind, null, tint = if (report.validated) Moss else Ink.copy(alpha = .7f), modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(16.dp))
                    Column { Text(if (report.validated) "網路可正常對外" else "等待檢查", fontSize = 20.sp, fontWeight = FontWeight.SemiBold); Text(report.transport, color = Ink.copy(alpha = .6f)) }
                }
                Spacer(Modifier.height(24.dp))
                Text("公開 IP", color = Ink.copy(alpha = .55f), fontSize = 13.sp)
                Text(report.publicIp, fontSize = 22.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(16.dp))
        report.probes.forEach { ProbeRow(it); Spacer(Modifier.height(8.dp)) }
        if (report.checkedAt != null) Text("最後檢查：${formatTime(report.checkedAt!!)}", color = Ink.copy(alpha = .5f), modifier = Modifier.padding(vertical = 8.dp))
        Spacer(Modifier.height(12.dp))
        Button(onClick = vm::checkNetwork, enabled = !checking, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(16.dp)) {
            if (checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Canvas) else Icon(Icons.Outlined.PlayArrow, null)
            Spacer(Modifier.width(10.dp)); Text(if (checking) "正在分層檢查…" else "立即檢查", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            if (monitoring) stopMonitor(context) else startMonitor(context)
            monitoring = !monitoring
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(14.dp)) {
            Icon(if (monitoring) Icons.Outlined.Pause else Icons.Outlined.NotificationsActive, null)
            Spacer(Modifier.width(8.dp)); Text(if (monitoring) "停止每分鐘監控" else "開始每分鐘監控")
        }
        Spacer(Modifier.height(8.dp))
        Text("持續監控會保留通知；首次失敗於 5 秒後複查，連續失敗才記為斷線。原始紀錄儲存在 App 私有 CSV。", color = Ink.copy(alpha = .72f), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun HinetScreen(vm: MainViewModel, modifier: Modifier) {
    val results by vm.fastly.collectAsStateWithLifecycle()
    val checking by vm.fastlyChecking.collectAsStateWithLifecycle()
    Page("中華電信專區", "檢查 HiNet 到 Fastly 服務的實際路由表現，找出 GitHub、X 與 Twitch 的卡點。", modifier) {
        InfoCard(Icons.Outlined.Info, "Android 限制", "一般 App 無法修改系統 hosts。本區提供可驗證的路由診斷；若裝置有 root，才適合另加 hosts 套用模式。")
        Spacer(Modifier.height(16.dp))
        results.forEach { ProbeRow(it); Spacer(Modifier.height(8.dp)) }
        if (results.isEmpty()) EmptyPanel("尚未測試 Fastly 路線", "測試只讀取少量資料，不會執行完整大型測速。")
        Spacer(Modifier.height(16.dp))
        Button(onClick = vm::checkFastly, enabled = !checking, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(16.dp)) {
            if (checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Canvas) else Icon(Icons.Outlined.Route, null)
            Spacer(Modifier.width(10.dp)); Text(if (checking) "正在測試路線…" else "測試 Fastly 服務")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { openUrl(LocalContext.current, "https://speed.hinet.net/") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(14.dp)) {
            Icon(Icons.Outlined.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("開啟 HiNet 官方測速")
        }
    }
}

@Composable
private fun DnsScreen(modifier: Modifier) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(dnsProviders.first()) }
    var active by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<DnsProvider?>(null) }
    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pending?.let { startDns(context, it.primary); active = true }; pending = null
    }
    Page("DNS 切換", "用本機 DNS-only VPN 切換解析服務；流量不會送往第三方 VPN 業者。", modifier) {
        dnsProviders.forEach { provider ->
            DnsChoice(provider, selected == provider, active && selected == provider) { selected = provider }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = {
            if (active) { stopDns(context); active = false }
            else {
                val prepare = VpnService.prepare(context)
                if (prepare != null) { pending = selected; vpnLauncher.launch(prepare) } else { startDns(context, selected.primary); active = true }
            }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) Ink else Moss)) {
            Icon(if (active) Icons.Outlined.Stop else Icons.Outlined.Shield, null); Spacer(Modifier.width(8.dp)); Text(if (active) "停止 DNS 切換" else "套用 ${selected.name}")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            copy(context, selected.privateDnsHost)
            context.startActivity(Intent(Settings.ACTION_PRIVATE_DNS_SETTINGS))
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(14.dp)) {
            Icon(Icons.Outlined.Settings, null); Spacer(Modifier.width(8.dp)); Text("改用系統 Private DNS")
        }
        Spacer(Modifier.height(12.dp))
        Text("提示：Private DNS 使用主機名稱（已複製 ${selected.privateDnsHost}）；DNS-only VPN 會占用 Android 唯一的 VPN 插槽。", color = Ink.copy(alpha = .72f), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun DnsChoice(provider: DnsProvider, selected: Boolean, active: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = if (selected) Mint else Panel), shape = RoundedCornerShape(8.dp), border = CardDefaults.outlinedCardBorder()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) { Text(provider.name, fontWeight = FontWeight.SemiBold); Text("${provider.primary}  ·  ${provider.secondary}", color = Ink.copy(alpha = .72f), fontSize = 14.sp, fontFamily = FontFamily.Monospace) }
            if (active) AssistChip(onClick = {}, label = { Text("使用中") }, leadingIcon = { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) })
        }
    }
}

@Composable
private fun ProbeRow(item: ProbeResult) {
    val color = when (item.state) { CheckState.Success -> Moss; CheckState.Warning -> Color(0xFFB06B00); CheckState.Failed -> Color(0xFFB3261E); else -> Ink.copy(alpha = .4f) }
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(6.dp), border = CardDefaults.outlinedCardBorder()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(when (item.state) { CheckState.Success -> Icons.Outlined.CheckCircle; CheckState.Warning -> Icons.Outlined.WarningAmber; CheckState.Failed -> Icons.Outlined.ErrorOutline; else -> Icons.Outlined.HourglassEmpty }, item.state.name, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(item.name, fontWeight = FontWeight.Medium); Text(item.detail, color = Ink.copy(alpha = .72f), fontSize = 14.sp) }
            item.latencyMs?.let { Text("$it ms", color = color, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace) }
        }
    }
}

@Composable private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) = Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF342B18)), shape = RoundedCornerShape(8.dp)) { Row(Modifier.padding(18.dp)) { Icon(icon, null, tint = Color(0xFFFFC96B)); Spacer(Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Text(body, color = Ink.copy(alpha = .78f), fontSize = 14.sp, lineHeight = 20.sp) } } }
@Composable private fun EmptyPanel(title: String, body: String) = Box(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(8.dp)).padding(24.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Route, null, tint = Ink.copy(alpha = .6f), modifier = Modifier.size(34.dp)); Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.SemiBold); Text(body, color = Ink.copy(alpha = .72f), fontSize = 14.sp) } }

private fun startDns(context: Context, dns: String) = ContextCompat.startForegroundService(context, Intent(context, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_START).putExtra(DnsVpnService.EXTRA_DNS, dns))
private fun stopDns(context: Context) = context.startService(Intent(context, DnsVpnService::class.java).setAction(DnsVpnService.ACTION_STOP))
private fun startMonitor(context: Context) = ContextCompat.startForegroundService(context, Intent(context, NetworkMonitorService::class.java).setAction(NetworkMonitorService.ACTION_START))
private fun stopMonitor(context: Context) = context.startService(Intent(context, NetworkMonitorService::class.java).setAction(NetworkMonitorService.ACTION_STOP))
private fun openUrl(context: Context, url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
private fun copy(context: Context, text: String) = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Private DNS", text))
private fun formatTime(time: Long) = SimpleDateFormat("MM/dd HH:mm:ss", Locale.TAIWAN).format(Date(time))
