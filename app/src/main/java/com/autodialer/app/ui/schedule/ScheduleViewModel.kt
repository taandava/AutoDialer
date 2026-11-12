package com.autodialer.app.ui.schedule

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodialer.app.AutoDialerApplication
import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduleWithContacts
import com.autodialer.app.data.model.ScheduledContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ================================================================================================
 * WHAT IS IT:
 * Architecture ViewModel managing scheduled campaign lists, alarm toggling, and campaign deletion.
 *
 * WHAT IT DOES:
 * 1. Observes `scheduleRepository.allSchedules` and exposes a reactive [StateFlow] of [ScheduleWithContacts].
 * 2. Implements [toggleSchedule] which flips campaign enabled status, resets past run times, updates
 *    the schedule record without wiping child contacts, and registers/cancels alarms via [CallSchedulerManager].
 * 3. Handles [deleteSchedule] by cancelling system alarms and cascading deletion in Room.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - UI Screens: [ScheduleListScreen], [AddEditScheduleScreen]
 * - Repositories: [com.autodialer.app.data.repository.ScheduleRepository], [com.autodialer.app.data.repository.CsvRepository]
 * - Scheduler: [com.autodialer.app.service.CallSchedulerManager]
 * ================================================================================================
 */
class ScheduleViewModel : ViewModel() {

    private val repository = AutoDialerApplication.instance.scheduleRepository
    private val csvRepository = AutoDialerApplication.instance.csvRepository
    private val scheduler = AutoDialerApplication.instance.callSchedulerManager

    val schedules: StateFlow<List<ScheduleWithContacts>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleSchedule(schedule: CallScheduleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStatus = !schedule.isEnabled
            var nextRun = schedule.nextRunTimeEpochMs
            var completed = schedule.isCompleted

            if (newStatus && (completed || nextRun <= System.currentTimeMillis())) {
                completed = false
                nextRun = System.currentTimeMillis() + 5000L // Reset to run 5 seconds from now
            }

            val updated = schedule.copy(
                isEnabled = newStatus,
                isCompleted = completed,
                nextRunTimeEpochMs = nextRun
            )

            // Safely update the schedule record WITHOUT wiping contacts
            repository.updateScheduleOnly(updated)

            if (newStatus) {
                scheduler.cancelAlarm(schedule.id)
                scheduler.scheduleAlarm(updated)
            } else {
                scheduler.cancelAlarm(schedule.id)
            }
        }
    }

    fun deleteSchedule(schedule: CallScheduleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.cancelAlarm(schedule.id)
            repository.deleteSchedule(schedule)
        }
    }

    fun saveScheduleWithContacts(
        schedule: CallScheduleEntity,
        contacts: List<ScheduledContactEntity>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.saveSchedule(schedule, contacts)
            val savedSchedule = schedule.copy(id = id)
            // Cancel old alarm first so edited time/parameters replace the old trigger cleanly
            scheduler.cancelAlarm(id)
            if (savedSchedule.isEnabled) {
                scheduler.scheduleAlarm(savedSchedule)
            }
        }
    }

    fun importCsvToContacts(uri: Uri, onImported: (List<ScheduledContactEntity>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val contacts = csvRepository.importContactsFromCsv(uri)
            onImported(contacts)
        }
    }

    fun exportScheduleToCsv(uri: Uri, contacts: List<ScheduledContactEntity>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = csvRepository.exportContactsToCsv(uri, contacts)
            onResult(success)
        }
    }
}
