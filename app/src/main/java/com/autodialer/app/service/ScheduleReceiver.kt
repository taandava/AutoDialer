package com.autodialer.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.autodialer.app.data.local.AppDatabase
import com.autodialer.app.data.local.AppLogLogger
import com.autodialer.app.data.model.ScheduleRepeatRule
import com.autodialer.app.data.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        AppLogLogger.log("ScheduleReceiver", "Receiver onReceive action=$action")

        val pendingResult = goAsync()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AutoDialer:ScheduleReceiverWakeLock"
        )
        wakeLock.acquire(15 * 1000L)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == Intent.ACTION_BOOT_COMPLETED) {
                    val db = AppDatabase.getDatabase(context)
                    val repository = ScheduleRepository(db.scheduleDao())
                    val scheduler = CallSchedulerManager(context)

                    val activeSchedules = repository.getActiveSchedules()
                    AppLogLogger.log("ScheduleReceiver", "Boot completed. Rescheduling ${activeSchedules.size} active schedules.")
                    for (s in activeSchedules) {
                        scheduler.scheduleAlarm(s.schedule)
                    }
                } else if (action == ACTION_TRIGGER_SCHEDULE) {
                    val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                    AppLogLogger.log("ScheduleReceiver", "ScheduleReceiver triggered for scheduleId=$scheduleId")
                    if (scheduleId != -1L) {
                        val db = AppDatabase.getDatabase(context)
                        val repository = ScheduleRepository(db.scheduleDao())
                        val scheduler = CallSchedulerManager(context)

                        val scheduleWithContacts = repository.getScheduleById(scheduleId)
                        if (scheduleWithContacts != null) {
                            val schedule = scheduleWithContacts.schedule
                            AppLogLogger.log("ScheduleReceiver", "Fetched schedule ID=$scheduleId title='${schedule.title}', contacts=${scheduleWithContacts.contacts.size}, enabled=${schedule.isEnabled}")

                            if (schedule.isEnabled && !schedule.isCompleted) {
                                val repeatRule = ScheduleRepeatRule.fromName(schedule.repeatRule)
                                val (nextRunMs, isCompleted) = CallSchedulerManager.calculateNextRunTime(
                                    schedule.nextRunTimeEpochMs,
                                    repeatRule,
                                    schedule.customIntervalSeconds,
                                    enforceFutureTime = true
                                )

                                repository.updateNextRunTime(scheduleId, nextRunMs, isCompleted)
                                if (!isCompleted) {
                                    scheduler.scheduleAlarm(schedule.copy(nextRunTimeEpochMs = nextRunMs))
                                }

                                val serviceIntent = Intent(context, AutoDialerService::class.java).apply {
                                    putExtra(AutoDialerService.EXTRA_SCHEDULE_ID, scheduleId)
                                }

                                AppLogLogger.log("ScheduleReceiver", "Starting AutoDialerService for schedule ID=$scheduleId")
                                context.startForegroundService(serviceIntent)
                            }
                        } else {
                            AppLogLogger.log("ScheduleReceiver", "Schedule ID=$scheduleId not found in database!")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogLogger.log("ScheduleReceiver", "Error processing ScheduleReceiver: ${e.message}")
            } finally {
                if (wakeLock.isHeld) {
                    try {
                        wakeLock.release()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_SCHEDULE = "com.autodialer.app.ACTION_TRIGGER_SCHEDULE"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    }
}
