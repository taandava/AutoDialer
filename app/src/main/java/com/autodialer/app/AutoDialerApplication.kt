package com.autodialer.app

import android.app.Application
import com.autodialer.app.data.local.AppDatabase
import com.autodialer.app.data.local.PreferencesManager
import com.autodialer.app.data.repository.CsvRepository
import com.autodialer.app.data.repository.ScheduleRepository
import com.autodialer.app.service.CallSchedulerManager
import com.autodialer.app.service.HeartbeatReceiver

class AutoDialerApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val preferencesManager by lazy { PreferencesManager(this) }
    val scheduleRepository by lazy { ScheduleRepository(database.scheduleDao()) }
    val csvRepository by lazy { CsvRepository(contentResolver) }
    val callSchedulerManager by lazy { CallSchedulerManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        HeartbeatReceiver.scheduleNextHeartbeat(this)
    }

    companion object {
        lateinit var instance: AutoDialerApplication
            private set
    }
}
