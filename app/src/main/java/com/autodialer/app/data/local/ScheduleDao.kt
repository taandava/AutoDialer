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

/**
 * ================================================================================================
 * WHAT IS IT:
 * Room Data Access Object (DAO) executing transactional and relational queries for scheduled
 * calling campaigns and their associated contact lists.
 *
 * WHAT IT DOES:
 * 1. Executes multi-table relational joins combining `call_schedules` and `scheduled_contacts`
 *    into [ScheduleWithContacts] domain models.
 * 2. Emits real-time reactive [Flow] streams of all campaigns sorted by next execution time.
 * 3. Provides atomic ACID transaction [upsertScheduleWithContacts] ensuring campaigns and their
 *    child contacts are inserted/updated together without race conditions.
 * 4. Enables granular updates (e.g. `updateScheduleEnabled`, `updateNextRunTime`, `updateSchedule`)
 *    protecting child contacts from accidental cascade wiping.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Entities:
 *   * [CallScheduleEntity]: Parent campaign schedule model.
 *   * [ScheduledContactEntity]: Child contact queue items with cascade delete.
 *   * [ScheduleWithContacts]: 1-to-many relationship container.
 * - Callers:
 *   * [com.autodialer.app.data.repository.ScheduleRepository]: Direct domain repository layer.
 *   * [com.autodialer.app.service.ScheduleReceiver]: Fetches campaign details upon alarm trigger.
 *   * [com.autodialer.app.service.HeartbeatReceiver]: Queries active schedules to keep alarms alive.
 * ================================================================================================
 */
@Dao
interface ScheduleDao {

    /**
     * Observes all campaigns with their child contacts, ordered by next scheduled run time.
     *
     * @return Reactive Flow emitting updated campaign lists.
     */
    @Transaction
    @Query("SELECT * FROM call_schedules ORDER BY nextRunTimeEpochMs ASC")
    fun getAllSchedulesWithContacts(): Flow<List<ScheduleWithContacts>>

    /**
     * Retrieves a single campaign with its child contacts by primary key ID.
     *
     * @param scheduleId Campaign primary key.
     * @return [ScheduleWithContacts] if found, null otherwise.
     */
    @Transaction
    @Query("SELECT * FROM call_schedules WHERE id = :scheduleId LIMIT 1")
    suspend fun getScheduleWithContactsById(scheduleId: Long): ScheduleWithContacts?

    /**
     * Retrieves all active (enabled = 1, completed = 0) campaigns for verification and scheduling.
     */
    @Transaction
    @Query("SELECT * FROM call_schedules WHERE isEnabled = 1 AND isCompleted = 0")
    suspend fun getActiveSchedules(): List<ScheduleWithContacts>

    /**
     * Inserts or replaces a schedule entity.
     *
     * @return Generated primary key ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: CallScheduleEntity): Long

    /**
     * Updates an existing schedule entity without modifying its child contacts.
     */
    @Update
    suspend fun updateSchedule(schedule: CallScheduleEntity)

    /**
     * Deletes a schedule entity. Triggers cascading delete of all child contacts in SQLite.
     */
    @Delete
    suspend fun deleteSchedule(schedule: CallScheduleEntity)

    /**
     * Inserts a list of contact entities into the `scheduled_contacts` table.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ScheduledContactEntity>)

    /**
     * Deletes all child contacts associated with a specific schedule ID.
     */
    @Query("DELETE FROM scheduled_contacts WHERE scheduleId = :scheduleId")
    suspend fun deleteContactsForSchedule(scheduleId: Long)

    /**
     * Toggles a schedule's enabled flag in-place without touching contact records.
     */
    @Query("UPDATE call_schedules SET isEnabled = :isEnabled WHERE id = :scheduleId")
    suspend fun updateScheduleEnabled(scheduleId: Long, isEnabled: Boolean)

    /**
     * Updates the next calculated run time and completion flag for a schedule after execution.
     */
    @Query("UPDATE call_schedules SET nextRunTimeEpochMs = :nextRunTime, isCompleted = :isCompleted WHERE id = :scheduleId")
    suspend fun updateNextRunTime(scheduleId: Long, nextRunTime: Long, isCompleted: Boolean)

    /**
     * Atomically upserts a campaign and synchronizes its contact queue within a single SQLite transaction.
     *
     * @param schedule The campaign configuration entity.
     * @param contacts The updated list of contacts to bind to this campaign.
     * @return The resulting campaign ID.
     */
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
