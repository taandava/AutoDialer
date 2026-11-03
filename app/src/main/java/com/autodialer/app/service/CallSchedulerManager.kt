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

class CallSchedulerManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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
