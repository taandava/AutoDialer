package com.autodialer.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val contactName: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val simSlot: Int = 0,
    val status: String = "Dialed",
    val durationSeconds: Int = 0,
    val scheduleTitle: String? = null
)
