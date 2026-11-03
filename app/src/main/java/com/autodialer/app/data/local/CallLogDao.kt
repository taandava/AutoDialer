package com.autodialer.app.data.local

import androidx.room.*
import com.autodialer.app.data.model.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_logs ORDER BY timestampEpochMs DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity): Long

    @Update
    suspend fun updateCallLog(callLog: CallLogEntity)

    @Query("UPDATE call_logs SET status = :status, durationSeconds = :durationSeconds WHERE id = :id")
    suspend fun updateCallStatus(id: Long, status: String, durationSeconds: Int)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllLogs()
}
