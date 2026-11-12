package com.autodialer.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ================================================================================================
 * WHAT IS IT:
 * Room entity representing an immutable historical record of a single outbound call attempt stored
 * in the `call_logs` table.
 *
 * WHAT IT DOES:
 * 1. Persists target telephone number, contact name, execution timestamp, and selected SIM slot.
 * 2. Tracks dynamic termination outcomes (e.g. "Completed", "Busy", "Rejected", "Missed", "Failed").
 * 3. Records exact elapsed talk duration in seconds.
 * 4. Associates the call with a parent campaign title or labels it as "Instant".
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Table: `call_logs`
 * - Handled by: [com.autodialer.app.data.local.CallLogDao]
 * - Emitted in: [com.autodialer.app.ui.ledger.CallLedgerViewModel] for ledger presentation and CSV export.
 * ================================================================================================
 */
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val simSlot: Int = 0,
    val status: String = "Dialed",
    val durationSeconds: Int = 0,
    val scheduleTitle: String? = null
)
