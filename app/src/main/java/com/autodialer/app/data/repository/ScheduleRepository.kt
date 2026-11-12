package com.autodialer.app.data.repository

import com.autodialer.app.data.local.ScheduleDao
import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduleWithContacts
import com.autodialer.app.data.model.ScheduledContactEntity
import kotlinx.coroutines.flow.Flow

/**
 * ================================================================================================
 * WHAT IS IT:
 * Domain repository managing business logic, persistence operations, and reactive data streams
 * for campaign schedules and their associated contact lists.
 *
 * WHAT IT DOES:
 * 1. Mediates between ViewModels/Receivers and Room SQLite persistence through [ScheduleDao].
 * 2. Exposes a live reactive stream [allSchedules] of campaigns with child contacts.
 * 3. Provides atomic save/update methods ([saveSchedule], [updateScheduleOnly]).
 * 4. Provides individual property updater methods ([toggleScheduleEnabled], [updateNextRunTime]).
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Data Source: [ScheduleDao]
 * - Direct Consumers:
 *   * [com.autodialer.app.ui.schedule.ScheduleViewModel]: Schedule list & toggling.
 *   * [com.autodialer.app.service.ScheduleReceiver]: Fetches campaign details when alarm fires.
 *   * [com.autodialer.app.service.HeartbeatReceiver]: Background health checks.
 *   * [com.autodialer.app.AutoDialerApplication]: Global singleton owner.
 * ================================================================================================
 */
class ScheduleRepository(private val scheduleDao: ScheduleDao) {

    /**
     * Observable stream of all campaigns with their child contacts, sorted by next execution time.
     */
    val allSchedules: Flow<List<ScheduleWithContacts>> = scheduleDao.getAllSchedulesWithContacts()

    /**
     * Retrieves a single campaign with contacts by primary key.
     */
    suspend fun getScheduleById(scheduleId: Long): ScheduleWithContacts? {
        return scheduleDao.getScheduleWithContactsById(scheduleId)
    }

    /**
     * Retrieves all currently active (enabled and not completed) campaigns.
     */
    suspend fun getActiveSchedules(): List<ScheduleWithContacts> {
        return scheduleDao.getActiveSchedules()
    }

    /**
     * Atomically saves or updates a campaign and its contact queue.
     */
    suspend fun saveSchedule(
        schedule: CallScheduleEntity,
        contacts: List<ScheduledContactEntity>
    ): Long {
        return scheduleDao.upsertScheduleWithContacts(schedule, contacts)
    }

    /**
     * Updates only schedule properties (e.g. title, repeat rule, timings) without wiping contacts.
     */
    suspend fun updateScheduleOnly(schedule: CallScheduleEntity) {
        scheduleDao.updateSchedule(schedule)
    }

    /**
     * Deletes a campaign and cascades deletion to all associated contacts.
     */
    suspend fun deleteSchedule(schedule: CallScheduleEntity) {
        scheduleDao.deleteSchedule(schedule)
    }

    /**
     * Toggles the enabled state of a campaign without touching its contact records.
     */
    suspend fun toggleScheduleEnabled(scheduleId: Long, isEnabled: Boolean) {
        scheduleDao.updateScheduleEnabled(scheduleId, isEnabled)
    }

    /**
     * Updates the next execution timestamp and completion flag for a campaign after a cycle finishes.
     */
    suspend fun updateNextRunTime(scheduleId: Long, nextRunTime: Long, isCompleted: Boolean) {
        scheduleDao.updateNextRunTime(scheduleId, nextRunTime, isCompleted)
    }
}
