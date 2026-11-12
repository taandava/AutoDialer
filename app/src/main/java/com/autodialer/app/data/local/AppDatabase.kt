package com.autodialer.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.autodialer.app.data.model.CallLogEntity
import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduledContactEntity

/**
 * ================================================================================================
 * WHAT IS IT:
 * The primary Room Object-Relational Mapping (ORM) database specification and SQLite manager for
 * the Auto Dialer application.
 *
 * WHAT IT DOES:
 * 1. Defines the database schema consisting of 3 core tables:
 *    - `call_schedules`: Campaign configurations, schedules, intervals, and repetitions.
 *    - `scheduled_contacts`: Contact queues associated with campaigns via foreign key.
 *    - `call_logs`: Historical call ledger records with timestamps, durations, and outcomes.
 * 2. Provides abstract factory accessors for [ScheduleDao] and [CallLogDao].
 * 3. Enforces thread-safe double-check locking singleton instantiation via `getDatabase(context)`.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Managed Entities:
 *   * [CallScheduleEntity]: Campaign records (`call_schedules`).
 *   * [ScheduledContactEntity]: Queued contact records (`scheduled_contacts`).
 *   * [CallLogEntity]: Historical call attempts and outcomes (`call_logs`).
 * - DAOs:
 *   * [ScheduleDao]: Campaign CRUD, relational joins, and schedule transactions.
 *   * [CallLogDao]: Ledger insertion, query flows, status updates, and clearing.
 * - Callers:
 *   * [AutoDialerApplication]: Global lazy singleton owner.
 *   * [ScheduleReceiver] & [HeartbeatReceiver]: Independent background database queries.
 * ================================================================================================
 */
@Database(
    entities = [CallScheduleEntity::class, ScheduledContactEntity::class, CallLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DAO accessor for campaign schedules and scheduled contact queries.
     */
    abstract fun scheduleDao(): ScheduleDao

    /**
     * DAO accessor for call history logs and ledger analytics.
     */
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the thread-safe singleton instance of [AppDatabase], constructing the SQLite
         * database file `autodialer_db` on first invocation.
         *
         * @param context Application context used for database creation.
         * @return The active [AppDatabase] instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autodialer_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
