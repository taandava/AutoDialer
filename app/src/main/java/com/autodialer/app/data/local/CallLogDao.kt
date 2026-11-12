package com.autodialer.app.data.local

import androidx.room.*
import com.autodialer.app.data.model.CallLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * ================================================================================================
 * WHAT IS IT:
 * Room Data Access Object (DAO) specifying CRUD operations and reactive queries for historical
 * call records stored in the `call_logs` database table.
 *
 * WHAT IT DOES:
 * 1. Provides a reactive [Flow] of all call logs sorted chronologically in descending order.
 * 2. Handles insertion of new call attempts returning the generated auto-increment primary key.
 * 3. Supports asynchronous in-place updates of call status and duration upon disconnect.
 * 4. Enables bulk deletion of all call logs for database maintenance.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Entity: [CallLogEntity]
 * - Direct Callers:
 *   * [com.autodialer.app.service.AutoDialerService]: Inserts initial call log on dial attempt,
 *     and updates final duration/status when call disconnects.
 *   * [com.autodialer.app.ui.ledger.CallLedgerViewModel]: Observes reactive call log list for
 *     dashboard metrics, filtering, and CSV export.
 * ================================================================================================
 */
@Dao
interface CallLogDao {

    /**
     * Observes the complete list of call logs sorted by timestamp descending.
     *
     * @return Hot [Flow] emitting the updated list whenever the `call_logs` table changes.
     */
    @Query("SELECT * FROM call_logs ORDER BY timestampEpochMs DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    /**
     * Inserts a new call log entry when a call is initiated.
     *
     * @param callLog The initial call entity with "Dialing" or "Initiated" state.
     * @return Generated database row ID used to correlate later status updates.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity): Long

    /**
     * Updates an existing call log entity.
     *
     * @param callLog The entity containing modified fields.
     */
    @Update
    suspend fun updateCallLog(callLog: CallLogEntity)

    /**
     * Updates only the termination status and final duration in seconds for a specific call record.
     *
     * @param id The primary key ID returned from [insertCallLog].
     * @param status Disconnect outcome (e.g. "Completed", "Busy", "Rejected", "Missed").
     * @param durationSeconds Total call duration in seconds.
     */
    @Query("UPDATE call_logs SET status = :status, durationSeconds = :durationSeconds WHERE id = :id")
    suspend fun updateCallStatus(id: Long, status: String, durationSeconds: Int)

    /**
     * Wipes all call log records from the database table.
     */
    @Query("DELETE FROM call_logs")
    suspend fun clearAllLogs()
}
