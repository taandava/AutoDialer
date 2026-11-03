package com.autodialer.app

import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduleRepeatRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallScheduleEntityTest {

    @Test
    fun testCallScheduleEntityDefaults() {
        val now = System.currentTimeMillis()
        val schedule = CallScheduleEntity(
            id = 1L,
            title = "Test Campaign",
            scheduledDateTimeEpochMs = now,
            nextRunTimeEpochMs = now
        )

        assertEquals(1L, schedule.id)
        assertEquals("Test Campaign", schedule.title)
        assertEquals(now, schedule.scheduledDateTimeEpochMs)
        assertEquals(now, schedule.nextRunTimeEpochMs)
        assertEquals(ScheduleRepeatRule.ONCE.name, schedule.repeatRule)
        assertEquals(3, schedule.intervalBetweenCallsSeconds)
        assertEquals(1, schedule.repetitionsPerNumber)
        assertEquals(10, schedule.durationLimitSeconds)
        assertTrue(schedule.includeDialingTime)
        assertTrue(schedule.isEnabled)
    }
}
