package com.autodialer.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_contacts",
    foreignKeys = [
        ForeignKey(
            entity = CallScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scheduleId"])]
)
data class ScheduledContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val phoneNumber: String,
    val contactName: String = "",
    val orderIndex: Int = 0,
    val status: String = "PENDING" // PENDING, DIALING, COMPLETED, FAILED, SKIPPED
)
