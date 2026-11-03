package com.autodialer.app.data.repository

import com.autodialer.app.data.local.ScheduleDao
import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduleWithContacts
import com.autodialer.app.data.model.ScheduledContactEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val scheduleDao: ScheduleDao) {

    val allSchedules: Flow<List<ScheduleWithContacts>> = scheduleDao.getAllSchedulesWithContacts()

    suspend fun getScheduleById(scheduleId: Long): ScheduleWithContacts? {
        return scheduleDao.getScheduleWithContactsById(scheduleId)
    }

    suspend fun getActiveSchedules(): List<ScheduleWithContacts> {
        return scheduleDao.getActiveSchedules()
    }

    suspend fun saveSchedule(
        schedule: CallScheduleEntity,
        contacts: List<ScheduledContactEntity>
    ): Long {
        return scheduleDao.upsertScheduleWithContacts(schedule, contacts)
    }

    suspend fun updateScheduleOnly(schedule: CallScheduleEntity) {
        scheduleDao.updateSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: CallScheduleEntity) {
        scheduleDao.deleteSchedule(schedule)
    }

    suspend fun toggleScheduleEnabled(scheduleId: Long, isEnabled: Boolean) {
        scheduleDao.updateScheduleEnabled(scheduleId, isEnabled)
    }

    suspend fun updateNextRunTime(scheduleId: Long, nextRunTime: Long, isCompleted: Boolean) {
        scheduleDao.updateNextRunTime(scheduleId, nextRunTime, isCompleted)
    }
}
