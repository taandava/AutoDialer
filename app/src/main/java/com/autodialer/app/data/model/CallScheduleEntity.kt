package com.autodialer.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ================================================================================================
 * WHAT IS IT:
 * Room entity representing a scheduled calling campaign stored in the `call_schedules` table.
 *
 * WHAT IT DOES:
 * 1. Stores campaign identification (title, creation timestamp, ID).
 * 2. Encapsulates scheduling timestamps (initial scheduled date/time and next execution epoch).
 * 3. Configures repeat policies using [ScheduleRepeatRule] (Once, Hourly, Daily, Weekly, Monthly,
 *    Custom Interval in seconds or minutes).
 * 4. Defines calling parameters (inter-call delay, retries per number, duration limits, SIM slot,
 *    speakerphone routing, and dialing time inclusion).
 * 5. Maintains runtime execution state flags (`isEnabled`, `isCompleted`).
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Table: `call_schedules`
 * - 1-to-Many Child: [ScheduledContactEntity] (bound via foreign key `scheduleId`).
 * - Relationship Container: [ScheduleWithContacts]
 * - DAOs & Services:
 *   * [com.autodialer.app.data.local.ScheduleDao]
 *   * [com.autodialer.app.service.CallSchedulerManager]: Calculates next alarm time.
 *   * [com.autodialer.app.service.AutoDialerService]: Executes campaign call queues.
 * ================================================================================================
 */
@Entity(tableName = "call_schedules")
data class CallScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val scheduledDateTimeEpochMs: Long,
    val nextRunTimeEpochMs: Long,
    val repeatRule: String = ScheduleRepeatRule.ONCE.name,
    val customIntervalMinutes: Int = 60,
    val customIntervalSeconds: Int = 60,
    val repeatIntervalUnit: String = "MINUTES",
    val intervalBetweenCallsSeconds: Int = 3,
    val repetitionsPerNumber: Int = 1,
    val durationLimitSeconds: Int = 10,
    val includeDialingTime: Boolean = true,
    val speakerphoneEnabled: Boolean = false,
    val simSlot: Int = 0,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
