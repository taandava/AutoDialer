package com.autodialer.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.autodialer.app.data.local.AppLogLogger
import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduleRepeatRule
import java.util.Calendar

/**
 * ================================================================================================
 * WHAT IS IT:
 * Hardware and OS scheduling engine responsible for setting, updating, and cancelling exact alarms
 * via Android's [AlarmManager] to trigger automated calling campaigns at exact times.
 *
 * WHAT IT DOES:
 * 1. Coordinates with [AlarmManager.setAlarmClock] as primary tier to guarantee wakeups even under
 *    aggressive OEM power-saving modes (Samsung, Xiaomi, Huawei Doze).
 * 2. Provides [AlarmManager.setExactAndAllowWhileIdle] as a robust fallback.
 * 3. Builds immutable [PendingIntent] targets directing to [ScheduleReceiver].
 * 4. Cancels active alarms when schedules are disabled, completed, or deleted.
 * 5. Contains pure static calculation logic in [calculateNextRunTime] for calculating calendar
 *    recurrences (Hourly, Daily, Weekly, Monthly, and custom intervals) with past-time fast-forwarding.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - System Framework: [AlarmManager]
 * - Destination Target: [ScheduleReceiver] (`ACTION_TRIGGER_SCHEDULE`)
 * - Data Model: [CallScheduleEntity], [ScheduleRepeatRule]
 * - Direct Callers:
 *   * [com.autodialer.app.ui.schedule.ScheduleViewModel]: Schedules alarms on create/toggle.
 *   * [com.autodialer.app.service.ScheduleReceiver]: Reschedules recurring alarms after trigger.
 *   * [com.autodialer.app.service.HeartbeatReceiver]: Periodically validates active alarms.
 *   * [com.autodialer.app.AutoDialerApplication]: Singleton owner.
 * ================================================================================================
 */
class CallSchedulerManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules an exact system alarm for the provided campaign entity.
     *
     * @param schedule The campaign configuration entity containing next run time.
     */
    fun scheduleAlarm(schedule: CallScheduleEntity) {
        if (!schedule.isEnabled || schedule.isCompleted) {
            AppLogLogger.log("CallSchedulerManager", "Schedule ${schedule.id} disabled or completed. Cancelling alarm.")
            cancelAlarm(schedule.id)
            return
        }

        var targetTimeMs = schedule.nextRunTimeEpochMs
        if (targetTimeMs <= System.currentTimeMillis()) {
            targetTimeMs = System.currentTimeMillis() + 1000L
        }

        val secondsFromNow = (targetTimeMs - System.currentTimeMillis()) / 1000
        AppLogLogger.log("CallSchedulerManager", "Scheduling alarm ID=${schedule.id} ('${schedule.title}') at $targetTimeMs (in ${secondsFromNow}s)")

        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_TRIGGER_SCHEDULE
            putExtra(ScheduleReceiver.EXTRA_SCHEDULE_ID, schedule.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(targetTimeMs, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            AppLogLogger.log("CallSchedulerManager", "AlarmClock set successfully for schedule ID=${schedule.id}")
        } catch (e: Exception) {
            AppLogLogger.log("CallSchedulerManager", "Error setting alarm clock for schedule ID=${schedule.id}: ${e.message}")
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTimeMs,
                    pendingIntent
                )
            } catch (ex: Exception) {
                AppLogLogger.log("CallSchedulerManager", "Fallback alarm set failed for schedule ID=${schedule.id}: ${ex.message}")
            }
        }
    }

    /**
     * Cancels any pending AlarmManager wakeups for the specified campaign ID.
     *
     * @param scheduleId Unique ID of the campaign to cancel.
     */
    fun cancelAlarm(scheduleId: Long) {
        AppLogLogger.log("CallSchedulerManager", "Cancelling alarm for schedule ID=$scheduleId")
        val intent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ScheduleReceiver.ACTION_TRIGGER_SCHEDULE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    companion object {
        /**
         * Pure mathematical and calendar function calculating the next recurrence timestamp.
         *
         * @param currentRunTimeMs Base timestamp from which to advance.
         * @param repeatRule Frequency type (Once, Hourly, Daily, Weekly, Monthly, Repeat Interval).
         * @param customIntervalSeconds Interval magnitude in seconds for custom interval rule.
         * @param enforceFutureTime When true, fast-forwards through any cycles that have already passed.
         * @return Pair containing the next timestamp in epoch ms and a boolean indicating if campaign is complete.
         */
        fun calculateNextRunTime(
            currentRunTimeMs: Long,
            repeatRule: ScheduleRepeatRule,
            customIntervalSeconds: Int,
            enforceFutureTime: Boolean = false
        ): Pair<Long, Boolean> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = currentRunTimeMs
            }

            return when (repeatRule) {
                ScheduleRepeatRule.ONCE -> {
                    Pair(currentRunTimeMs, true)
                }
                ScheduleRepeatRule.HOURLY -> {
                    cal.add(Calendar.HOUR_OF_DAY, 1)
                    if (enforceFutureTime) {
                        val now = System.currentTimeMillis()
                        while (cal.timeInMillis <= now) {
                            cal.add(Calendar.HOUR_OF_DAY, 1)
                        }
                    }
                    Pair(cal.timeInMillis, false)
                }
                ScheduleRepeatRule.DAILY -> {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                    if (enforceFutureTime) {
                        val now = System.currentTimeMillis()
                        while (cal.timeInMillis <= now) {
                            cal.add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    Pair(cal.timeInMillis, false)
                }
                ScheduleRepeatRule.WEEKLY -> {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                    if (enforceFutureTime) {
                        val now = System.currentTimeMillis()
                        while (cal.timeInMillis <= now) {
                            cal.add(Calendar.WEEK_OF_YEAR, 1)
                        }
                    }
                    Pair(cal.timeInMillis, false)
                }
                ScheduleRepeatRule.MONTHLY -> {
                    cal.add(Calendar.MONTH, 1)
                    if (enforceFutureTime) {
                        val now = System.currentTimeMillis()
                        while (cal.timeInMillis <= now) {
                            cal.add(Calendar.MONTH, 1)
                        }
                    }
                    Pair(cal.timeInMillis, false)
                }
                ScheduleRepeatRule.REPEAT_INTERVAL -> {
                    val intervalMs = (customIntervalSeconds.coerceAtLeast(5)) * 1000L
                    var nextRunMs = currentRunTimeMs + intervalMs
                    if (enforceFutureTime && nextRunMs <= System.currentTimeMillis()) {
                        val now = System.currentTimeMillis()
                        val diff = now - currentRunTimeMs
                        val steps = (diff / intervalMs) + 1
                        nextRunMs = currentRunTimeMs + (steps * intervalMs)
                    }
                    Pair(nextRunMs, false)
                }
            }
        }
    }
}
