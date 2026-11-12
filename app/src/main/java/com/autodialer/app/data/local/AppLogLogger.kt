package com.autodialer.app.data.local

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ================================================================================================
 * WHAT IS IT:
 * In-memory diagnostic ring-buffer and log event emitter designed for live troubleshooting, developer
 * inspection, and background service diagnostics.
 *
 * WHAT IT DOES:
 * 1. Formats log entries with exact millisecond timestamps and component tags (`[HH:mm:ss.SSS] [TAG]`).
 * 2. Mirrors all log events to Android standard Logcat (`Log.d`).
 * 3. Maintains a bounded in-memory list (max 200 items, newest entries at top).
 * 4. Exposes an observable [StateFlow] (`logsState`) for real-time live console rendering in the
 *    Settings / Developer diagnostics UI.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Direct Callers:
 *   * [com.autodialer.app.service.AutoDialerService]: Calling loop lifecycle events and states.
 *   * [com.autodialer.app.service.AppInCallService]: Telecom InCall state transitions and audio routing.
 *   * [com.autodialer.app.service.CallSchedulerManager]: AlarmManager scheduling calculations.
 *   * [com.autodialer.app.service.ScheduleReceiver]: Alarm firing and broadcast notifications.
 *   * [com.autodialer.app.service.WakeAndCallActivity]: Screen wakeup and lockscreen bypass logs.
 * - Observers:
 *   * [com.autodialer.app.ui.settings.SettingsScreen]: Live diagnostic log viewer composable.
 * ================================================================================================
 */
object AppLogLogger {

    /**
     * Factory function creating localized time formatter per call to ensure thread safety
     * and dynamic locale compliance.
     */
    private fun getDateFormat(): SimpleDateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    /**
     * Internal in-memory ring-buffer containing formatted diagnostic entries.
     */
    private val logList = mutableListOf<String>()

    /**
     * Mutable backing flow for reactive UI consumption.
     */
    private val _logsState = MutableStateFlow<List<String>>(emptyList())

    /**
     * Public read-only StateFlow of current diagnostic logs.
     */
    val logsState: StateFlow<List<String>> = _logsState.asStateFlow()

    /**
     * Appends a diagnostic entry to the in-memory ring-buffer and emits an update to [logsState].
     *
     * @param tag Calling class or subsystem identifier.
     * @param message Diagnostic description.
     */
    @Synchronized
    fun log(tag: String, message: String) {
        val timestamp = getDateFormat().format(Date())
        val formattedEntry = "[$timestamp] [$tag] $message"
        
        Log.d(tag, message)

        logList.add(0, formattedEntry) // Newest at top
        if (logList.size > 200) {
            logList.removeAt(logList.lastIndex)
        }
        _logsState.value = logList.toList()
    }

    /**
     * Clears all in-memory diagnostic logs and resets [logsState] to an empty list.
     */
    @Synchronized
    fun clearLogs() {
        logList.clear()
        _logsState.value = emptyList()
    }
}
