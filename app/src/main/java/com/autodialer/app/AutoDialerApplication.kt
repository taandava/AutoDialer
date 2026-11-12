package com.autodialer.app

import android.app.Application
import com.autodialer.app.data.local.AppDatabase
import com.autodialer.app.data.local.PreferencesManager
import com.autodialer.app.data.repository.CsvRepository
import com.autodialer.app.data.repository.ScheduleRepository
import com.autodialer.app.service.CallSchedulerManager
import com.autodialer.app.service.HeartbeatReceiver

/**
 * ================================================================================================
 * WHAT IS IT:
 * Global Application class serving as the singleton process lifecycle anchor and primary dependency
 * injection container for the Auto Dialer application.
 *
 * WHAT IT DOES:
 * 1. Initializes process-wide singletons via lazy delegates (Database, Repositories, Preferences,
 *    Scheduler).
 * 2. Caches the global static `instance` reference on process initialization in `onCreate()`.
 * 3. Enqueues the initial recurring 15-minute background watchdog heartbeat through
 *    `HeartbeatReceiver.scheduleNextHeartbeat()`.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Upstream Trigger: Android OS process startup (`AndroidManifest.xml` android:name entry).
 * - Downstream Dependencies:
 *   * [AppDatabase]: SQLite Room database singleton (`autodialer_db`).
 *   * [PreferencesManager]: Key-value configuration storage in SharedPreferences.
 *   * [ScheduleRepository]: Domain abstraction layer over [ScheduleDao].
 *   * [CsvRepository]: ContentResolver-backed CSV import and export service.
 *   * [CallSchedulerManager]: Exact alarm dispatch engine via AlarmManager.
 *   * [HeartbeatReceiver]: Periodic background watchdog preventing OEM task termination.
 * ================================================================================================
 */
class AutoDialerApplication : Application() {

    /**
     * Singleton instance of Room database holding campaign schedules, queued contacts, and call logs.
     */
    val database by lazy { AppDatabase.getDatabase(this) }

    /**
     * Singleton SharedPreferences manager maintaining SIM selection, default delays, and theme states.
     */
    val preferencesManager by lazy { PreferencesManager(this) }

    /**
     * Domain repository managing ACID transactions and queries for scheduled campaigns and contacts.
     */
    val scheduleRepository by lazy { ScheduleRepository(database.scheduleDao()) }

    /**
     * File processing repository managing parsing and serializing CSV contact lists.
     */
    val csvRepository by lazy { CsvRepository(contentResolver) }

    /**
     * System AlarmManager wrapper coordinating exact RTC_WAKEUP alarms and repeat calculations.
     */
    val callSchedulerManager by lazy { CallSchedulerManager(this) }

    /**
     * Called when the application is starting, before any activity, service, or receiver objects
     * have been created. Sets up global singletons and kicks off the background heartbeat pulse.
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        HeartbeatReceiver.scheduleNextHeartbeat(this)
    }

    companion object {
        /**
         * Process-wide application context accessor.
         */
        lateinit var instance: AutoDialerApplication
            private set
    }
}
