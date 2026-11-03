package com.autodialer.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class ScheduleWithContacts(
    @Embedded val schedule: CallScheduleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scheduleId"
    )
    val contacts: List<ScheduledContactEntity>
)
