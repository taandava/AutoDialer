package com.autodialer.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ================================================================================================
 * WHAT IS IT:
 * Room entity representing an individual contact target queued within a scheduled campaign, stored
 * in the `scheduled_contacts` table.
 *
 * WHAT IT DOES:
 * 1. Binds each contact to a parent campaign using foreign key [CallScheduleEntity.id] with
 *    CASCADE deletion.
 * 2. Stores clean phone numbers, display names, and queue execution order index.
 * 3. Tracks contact status (e.g. "PENDING", "DIALING", "COMPLETED", "FAILED", "SKIPPED").
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Table: `scheduled_contacts`
 * - Parent Entity: [CallScheduleEntity]
 * - Parent Relationship: [ScheduleWithContacts]
 * - DAOs & Services:
 *   * [com.autodialer.app.data.local.ScheduleDao]
 *   * [com.autodialer.app.service.AutoDialerService]: Iterates queue during scheduled execution.
 *   * [com.autodialer.app.data.repository.CsvRepository]: Maps CSV rows to this entity.
 * ================================================================================================
 */
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
