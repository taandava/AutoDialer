package com.autodialer.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.autodialer.app.data.local.AppDatabase
import com.autodialer.app.data.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HeartbeatReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AutoDialerHeartbeat", "Heartbeat pulse received action=$action")

        val pendingResult = goAsync()
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AutoDialer:HeartbeatWakeLock"
        )
        wakeLock.acquire(10 * 1000L) // 10 second wake lock

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = ScheduleRepository(db.scheduleDao())
                val scheduler = CallSchedulerManager(context)

                val activeSchedules = repository.getActiveSchedules()
                Log.d("AutoDialerHeartbeat", "Heartbeat verifying ${activeSchedules.size} active schedules in database")

                for (s in activeSchedules) {
                    if (s.schedule.isEnabled && !s.schedule.isCompleted) {
                        scheduler.scheduleAlarm(s.schedule)
                    }
                }

                // Schedule next heartbeat pulse in 15 minutes
                scheduleNextHeartbeat(context)
            } catch (e: Exception) {
                Log.e("AutoDialerHeartbeat", "Error in HeartbeatReceiver", e)
            } finally {
                if (wakeLock.isHeld) {
                    try { wakeLock.release() } catch (e: Exception) { e.printStackTrace() }
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_HEARTBEAT = "com.autodialer.app.ACTION_HEARTBEAT"
        private const val HEARTBEAT_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        fun scheduleNextHeartbeat(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HeartbeatReceiver::class.java).apply {
                action = ACTION_HEARTBEAT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + HEARTBEAT_INTERVAL_MS
            try {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } catch (e: Exception) {
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }
}
