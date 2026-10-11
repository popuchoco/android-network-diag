package tw.xiaoxin.networktool.log

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long,
    val level: String,
    val source: String,
    val message: String,
)

object DiagnosticLog {
    private const val MAX_ENTRIES = 500
    private const val FILE_NAME = "diagnostic.log"
    private val lock = Any()
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    fun initialize(context: Context) = synchronized(lock) {
        if (_entries.value.isNotEmpty()) return
        val file = File(context.filesDir, FILE_NAME)
        _entries.value = runCatching {
            file.takeIf(File::exists)?.readLines()?.takeLast(MAX_ENTRIES)?.mapNotNull(::parseLine)
        }.getOrNull().orEmpty()
    }

    fun info(context: Context, source: String, message: String) = append(context, "INFO", source, message)
    fun warning(context: Context, source: String, message: String) = append(context, "WARN", source, message)
    fun error(context: Context, source: String, message: String) = append(context, "ERROR", source, message)

    fun clear(context: Context) = synchronized(lock) {
        _entries.value = emptyList()
        File(context.filesDir, FILE_NAME).delete()
    }

    fun exportText(): String = buildString {
        appendLine("Android Network Diag — diagnostic log")
        appendLine("Exported: ${formatTimestamp(System.currentTimeMillis())}")
        appendLine()
        _entries.value.forEach { appendLine(format(it)) }
    }

    private fun append(context: Context, level: String, source: String, message: String) = synchronized(lock) {
        val sanitized = message.replace('\n', ' ').replace('\r', ' ')
        val entry = LogEntry(System.currentTimeMillis(), level, source, sanitized)
        _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
        runCatching {
            File(context.filesDir, FILE_NAME).writeText(_entries.value.joinToString("\n", postfix = "\n", transform = ::serialize))
        }
    }

    private fun serialize(entry: LogEntry) = listOf(entry.timestamp, entry.level, entry.source, entry.message)
        .joinToString("\t") { it.toString().replace("\t", " ") }

    private fun parseLine(line: String): LogEntry? {
        val parts = line.split('\t', limit = 4)
        if (parts.size != 4) return null
        return LogEntry(parts[0].toLongOrNull() ?: return null, parts[1], parts[2], parts[3])
    }

    fun format(entry: LogEntry): String = "${formatTimestamp(entry.timestamp)}  ${entry.level.padEnd(5)}  ${entry.source} — ${entry.message}"

    private fun formatTimestamp(timestamp: Long) =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}
