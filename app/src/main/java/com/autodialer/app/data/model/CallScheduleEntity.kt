package com.autodialer.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
