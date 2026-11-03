package com.autodialer.app.data.local

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogLogger {

    private fun getDateFormat(): SimpleDateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val logList = mutableListOf<String>()
    private val _logsState = MutableStateFlow<List<String>>(emptyList())
    val logsState: StateFlow<List<String>> = _logsState.asStateFlow()

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

    @Synchronized
    fun clearLogs() {
        logList.clear()
        _logsState.value = emptyList()
    }
}
