package me.kyrozyn.carside.core

import android.util.Log
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

data class LogEntry(
    val id: Long,
    val time: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null,
)

/**
 * In-App Logger untuk Carside.
 * Menyimpan log sistem, Google Maps, GPS, dan error jaringan ke dalam memori
 * sehingga pengguna dapat melihat dan mendiagnosis masalah langsung di layar Head Unit.
 */
object AppLogger {
    private const val MAX_LOGS = 600
    private val nextId = AtomicLong(1L)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val deque = ConcurrentLinkedDeque<LogEntry>()
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        append(LogLevel.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        append(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String, tr: Throwable? = null) {
        if (tr != null) Log.w(tag, message, tr) else Log.w(tag, message)
        append(LogLevel.WARN, tag, message, tr)
    }

    fun e(tag: String, message: String, tr: Throwable? = null) {
        if (tr != null) Log.e(tag, message, tr) else Log.e(tag, message)
        append(LogLevel.ERROR, tag, message, tr)
    }

    private fun append(level: LogLevel, tag: String, message: String, tr: Throwable? = null) {
        val timeStr = try {
            LocalTime.now().format(timeFormatter)
        } catch (_: Exception) {
            System.currentTimeMillis().toString()
        }
        val stackTrace = tr?.stackTraceToString()
        val entry = LogEntry(
            id = nextId.getAndIncrement(),
            time = timeStr,
            level = level,
            tag = tag,
            message = message,
            throwable = stackTrace,
        )
        deque.addLast(entry)
        while (deque.size > MAX_LOGS) {
            deque.pollFirst()
        }
        _logs.value = deque.toList()
    }

    fun clear() {
        deque.clear()
        _logs.value = emptyList()
    }

    fun getAllAsText(): String {
        val list = deque.toList()
        if (list.isEmpty()) return "Log kosong."
        return buildString {
            list.forEach { entry ->
                append("[${entry.time}] [${entry.level.name}] [${entry.tag}] ${entry.message}\n")
                if (!entry.throwable.isNullOrBlank()) {
                    append("${entry.throwable}\n")
                }
            }
        }
    }
}
