package com.autodialer.app.data.model

/**
 * ================================================================================================
 * WHAT IS IT:
 * Domain enumeration defining the recurring schedule recurrence policies supported by the Auto Dialer
 * scheduling engine.
 *
 * WHAT IT DOES:
 * 1. Enumerates scheduling frequencies:
 *    - [ONCE]: Fires once at scheduled time and transitions to completed.
 *    - [HOURLY]: Adds 1 calendar hour per cycle.
 *    - [DAILY]: Adds 1 calendar day per cycle.
 *    - [WEEKLY]: Adds 1 calendar week (7 days) per cycle.
 *    - [MONTHLY]: Adds 1 calendar month, preserving day-of-month bounds.
 *    - [REPEAT_INTERVAL]: Advances by custom user-defined intervals (seconds/minutes).
 * 2. Provides safe lookup helper `fromName()` falling back to [ONCE] on unknown names.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Entity: [CallScheduleEntity.repeatRule]
 * - Computational Engine: [com.autodialer.app.service.CallSchedulerManager.calculateNextRunTime]
 * - UI Screens: [com.autodialer.app.ui.schedule.AddEditScheduleScreen] repeat rule picker.
 * ================================================================================================
 */
enum class ScheduleRepeatRule(val label: String) {
    ONCE("Once"),
    HOURLY("Hourly"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    REPEAT_INTERVAL("Repeat Interval");

    companion object {
        /**
         * Resolves enum instance case-insensitively, defaulting to [ONCE].
         */
        fun fromName(name: String): ScheduleRepeatRule {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: ONCE
        }
    }
}
