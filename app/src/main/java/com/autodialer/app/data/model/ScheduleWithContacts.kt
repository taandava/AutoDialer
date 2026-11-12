package com.autodialer.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * ================================================================================================
 * WHAT IS IT:
 * Room 1-to-many relationship data carrier linking a parent [CallScheduleEntity] with its associated
 * list of child [ScheduledContactEntity] records.
 *
 * WHAT IT DOES:
 * Encapsulates the relational result of SQLite queries without requiring manual joins in code.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Parent: [CallScheduleEntity] (embedded)
 * - Children: List of [ScheduledContactEntity] (related via `schedule.id == contact.scheduleId`)
 * - Queries: [com.autodialer.app.data.local.ScheduleDao.getAllSchedulesWithContacts],
 *            [com.autodialer.app.data.local.ScheduleDao.getScheduleWithContactsById].
 * - ViewModels: [com.autodialer.app.ui.schedule.ScheduleViewModel] for rendering schedule lists.
 * ================================================================================================
 */
data class ScheduleWithContacts(
    @Embedded val schedule: CallScheduleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scheduleId"
    )
    val contacts: List<ScheduledContactEntity>
)
