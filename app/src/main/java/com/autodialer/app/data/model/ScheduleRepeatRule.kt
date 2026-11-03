package com.autodialer.app.data.model

enum class ScheduleRepeatRule(val label: String) {
    ONCE("Once"),
    HOURLY("Hourly"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    REPEAT_INTERVAL("Repeat Interval");

    companion object {
        fun fromName(name: String): ScheduleRepeatRule {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: ONCE
        }
    }
}
