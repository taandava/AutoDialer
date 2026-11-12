package com.autodialer.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.autodialer.app.data.local.AppDatabase
import com.autodialer.app.data.local.AppLogLogger
import com.autodialer.app.data.model.ScheduleRepeatRule
import com.autodialer.app.data.repository.ScheduleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ================================================================================================
 * WHAT IS IT:
 * Core broadcast entry point handling system alarm triggers (`ACTION_TRIGGER_SCHEDULE`) and device
 * boot events (`Intent.ACTION_BOOT_COMPLETED`).
 *
 * WHAT IT DOES:
 * 1. Reschedules all active database campaigns when the device finishes rebooting (`BOOT_COMPLETED`).
 * 2. When a scheduled alarm fires:
 *    - Acquires a CPU wake lock for 15 seconds.
 *    - Fetches the campaign and its queued contacts from Room database.
 *    - Calculates and commits the next recurrence cycle via [CallSchedulerManager.calculateNextRunTime].
 *    - If device is locked, launches [WakeAndCallActivity] to turn the screen on and bypass keyguard.
 *    - Otherwise, starts [AutoDialerService] as a Foreground Service to execute the call sequence.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Upstream Triggers:
 *   * [android.content.Intent.ACTION_BOOT_COMPLETED]: System boot completion.
 *   * [ACTION_TRIGGER_SCHEDULE]: [CallSchedulerManager] exact alarms.
 * - Downstream Executors:
 *   * [WakeAndCallActivity]: Lockscreen wakeup activity.
 *   * [AutoDialerService]: Foreground dialing service.
 *   * [ScheduleRepository]: Campaign updates in Room database.
 * ================================================================================================
 */
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
                                    val updatedSchedule = schedule.copy(
                                        nextRunTimeEpochMs = nextRunMs,
                                        isCompleted = false
                                    )
                                    scheduler.scheduleAlarm(updatedSchedule)
                                }

                                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                                val isLocked = keyguardManager?.isKeyguardLocked ?: false

                                if (isLocked && scheduleWithContacts.contacts.isNotEmpty()) {
                                    AppLogLogger.log("ScheduleReceiver", "Device locked. Launching WakeAndCallActivity.")
                                    val wakeIntent = Intent(context, WakeAndCallActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        putExtra(WakeAndCallActivity.EXTRA_PHONE_NUMBER, scheduleWithContacts.contacts.first().phoneNumber)
                                        putExtra(WakeAndCallActivity.EXTRA_SIM_SLOT, schedule.simSlot)
                                    }
                                    context.startActivity(wakeIntent)
                                } else {
                                    AppLogLogger.log("ScheduleReceiver", "Starting AutoDialerService for schedule ID=$scheduleId")
                                    val serviceIntent = Intent(context, AutoDialerService::class.java).apply {
                                        putExtra(AutoDialerService.EXTRA_SCHEDULE_ID, scheduleId)
                                    }
                                    ContextCompat.startForegroundService(context, serviceIntent)
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogLogger.log("ScheduleReceiver", "Exception in ScheduleReceiver: ${e.message}")
            } finally {
                if (wakeLock.isHeld) {
                    try { wakeLock.release() } catch (e: Exception) { e.printStackTrace() }
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
