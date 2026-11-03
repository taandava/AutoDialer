package com.autodialer.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduleWithContacts
import com.autodialer.app.data.model.ScheduledContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Transaction
    @Query("SELECT * FROM call_schedules ORDER BY nextRunTimeEpochMs ASC")
    fun getAllSchedulesWithContacts(): Flow<List<ScheduleWithContacts>>

    @Transaction
    @Query("SELECT * FROM call_schedules WHERE id = :scheduleId LIMIT 1")
    suspend fun getScheduleWithContactsById(scheduleId: Long): ScheduleWithContacts?

    @Transaction
    @Query("SELECT * FROM call_schedules WHERE isEnabled = 1 AND isCompleted = 0")
    suspend fun getActiveSchedules(): List<ScheduleWithContacts>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: CallScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: CallScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: CallScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ScheduledContactEntity>)

    @Query("DELETE FROM scheduled_contacts WHERE scheduleId = :scheduleId")
    suspend fun deleteContactsForSchedule(scheduleId: Long)

    @Query("UPDATE call_schedules SET isEnabled = :isEnabled WHERE id = :scheduleId")
    suspend fun updateScheduleEnabled(scheduleId: Long, isEnabled: Boolean)

    @Query("UPDATE call_schedules SET nextRunTimeEpochMs = :nextRunTime, isCompleted = :isCompleted WHERE id = :scheduleId")
    suspend fun updateNextRunTime(scheduleId: Long, nextRunTime: Long, isCompleted: Boolean)

    @Transaction
    suspend fun upsertScheduleWithContacts(
        schedule: CallScheduleEntity,
        contacts: List<ScheduledContactEntity>
    ): Long {
        val id = insertSchedule(schedule)
        deleteContactsForSchedule(id)
        val contactsWithScheduleId = contacts.mapIndexed { index, c ->
            c.copy(scheduleId = id, orderIndex = index)
        }
        insertContacts(contactsWithScheduleId)
        return id
    }
}
