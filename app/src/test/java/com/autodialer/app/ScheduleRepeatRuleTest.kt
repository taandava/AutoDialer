package com.autodialer.app

import com.autodialer.app.data.model.ScheduleRepeatRule
import com.autodialer.app.service.CallSchedulerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleRepeatRuleTest {

    @Test
    fun testOnceRepeatRuleCompleted() {
        val now = System.currentTimeMillis()
        val (nextRunMs, isCompleted) = CallSchedulerManager.calculateNextRunTime(
            now,
            ScheduleRepeatRule.ONCE,
            60
        )
        assertEquals(now, nextRunMs)
        assertTrue(isCompleted)
    }

    @Test
    fun testDailyRepeatRuleCalculatesNextDay() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        }
        val currentRunMs = cal.timeInMillis

        val (nextRunMs, isCompleted) = CallSchedulerManager.calculateNextRunTime(
            currentRunMs,
            ScheduleRepeatRule.DAILY,
            60
        )

        val nextCal = Calendar.getInstance().apply {
            timeInMillis = nextRunMs
        }

        assertFalse(isCompleted)
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH))
        assertEquals(2, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, nextCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testHourlyRepeatRuleCalculatesNextHour() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 10, 30, 0)
        }
        val currentRunMs = cal.timeInMillis

        val (nextRunMs, isCompleted) = CallSchedulerManager.calculateNextRunTime(
            currentRunMs,
            ScheduleRepeatRule.HOURLY,
            60
        )

        val nextCal = Calendar.getInstance().apply {
            timeInMillis = nextRunMs
        }

        assertFalse(isCompleted)
        assertEquals(11, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCal.get(Calendar.MINUTE))
    }

    @Test
    fun testWeeklyRepeatRuleCalculatesNextWeek() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 10, 0, 0)
        }
        val currentRunMs = cal.timeInMillis

        val (nextRunMs, isCompleted) = CallSchedulerManager.calculateNextRunTime(
            currentRunMs,
            ScheduleRepeatRule.WEEKLY,
            60
        )

        val nextCal = Calendar.getInstance().apply {
            timeInMillis = nextRunMs
        }

        assertFalse(isCompleted)
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH))
        assertEquals(8, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, nextCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testMonthlyRepeatRuleCalculatesNextMonth() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 15, 14, 0, 0)
        }
        val currentRunMs = cal.timeInMillis

        val (nextRunMs, isCompleted) = CallSchedulerManager.calculateNextRunTime(
            currentRunMs,
            ScheduleRepeatRule.MONTHLY,
            60
        )

        val nextCal = Calendar.getInstance().apply {
            timeInMillis = nextRunMs
        }

        assertFalse(isCompleted)
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(15, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, nextCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testRepeatIntervalCalculatesCorrectSeconds() {
        val baseMs = 1000000L
        val intervalSec = 120
        val (nextRunMs, isCompleted) = CallSchedulerManager.calculateNextRunTime(
            baseMs,
            ScheduleRepeatRule.REPEAT_INTERVAL,
            intervalSec
        )
        assertFalse(isCompleted)
        assertEquals(baseMs + (120 * 1000L), nextRunMs)
    }

    @Test
    fun testEnforceFutureTimeFastForwardsPastTimestamp() {
        val pastMs = System.currentTimeMillis() - (3600 * 1000L) // 1 hour ago
        val (nextRunMs, isCompleted) = CallSchedulerManager.calculateNextRunTime(
            pastMs,
            ScheduleRepeatRule.HOURLY,
            60,
            enforceFutureTime = true
        )
        assertFalse(isCompleted)
        assertTrue(nextRunMs > System.currentTimeMillis())
    }

    @Test
    fun testScheduleRepeatRuleFromName() {
        assertEquals(ScheduleRepeatRule.ONCE, ScheduleRepeatRule.fromName("ONCE"))
        assertEquals(ScheduleRepeatRule.HOURLY, ScheduleRepeatRule.fromName("hourly"))
        assertEquals(ScheduleRepeatRule.DAILY, ScheduleRepeatRule.fromName("Daily"))
        assertEquals(ScheduleRepeatRule.WEEKLY, ScheduleRepeatRule.fromName("weekly"))
        assertEquals(ScheduleRepeatRule.MONTHLY, ScheduleRepeatRule.fromName("Monthly"))
        assertEquals(ScheduleRepeatRule.REPEAT_INTERVAL, ScheduleRepeatRule.fromName("repeat_interval"))
        assertEquals(ScheduleRepeatRule.ONCE, ScheduleRepeatRule.fromName("unknown_invalid_rule"))
    }
}
