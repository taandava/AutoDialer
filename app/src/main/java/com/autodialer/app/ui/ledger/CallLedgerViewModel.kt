package com.autodialer.app.ui.ledger

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodialer.app.AutoDialerApplication
import com.autodialer.app.data.model.CallLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ================================================================================================
 * WHAT IS IT:
 * Architecture ViewModel providing reactive call history streams and CSV export functionality
 * for the Call Ledger / History dashboard.
 *
 * WHAT IT DOES:
 * 1. Observes `callLogDao.getAllCallLogs()` and exposes an asynchronous [StateFlow] of [CallLogEntity] items.
 * 2. Clears all call history from Room database in [clearAllLogs].
 * 3. Serializes call history records to RFC 4180-compliant CSV text and writes to a SAF destination in [exportLedgerToCsv].
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - UI Screen: [CallLedgerScreen]
 * - Database DAO: [com.autodialer.app.data.local.CallLogDao]
 * - Application Host: [com.autodialer.app.AutoDialerApplication]
 * ================================================================================================
 */
class CallLedgerViewModel : ViewModel() {

    private val db = AutoDialerApplication.instance.database

    val callLogs: StateFlow<List<CallLogEntity>> = db.callLogDao().getAllCallLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            db.callLogDao().clearAllLogs()
        }
    }

    fun exportLedgerToCsv(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val logs = callLogs.value
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write("ID,Phone Number,Contact Name,Timestamp,SIM Slot,Status,Duration (sec),Campaign\n")
                        for (log in logs) {
                            val timeStr = dateFormat.format(Date(log.timestampEpochMs))
                            writer.write("${log.id},\"${log.phoneNumber}\",\"${log.contactName}\",\"$timeStr\",SIM ${log.simSlot + 1},${log.status},${log.durationSeconds},\"${log.scheduleTitle ?: "Instant"}\"\n")
                        }
                    }
                }
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }
}
