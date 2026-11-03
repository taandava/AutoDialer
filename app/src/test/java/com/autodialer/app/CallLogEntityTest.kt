package com.autodialer.app

import com.autodialer.app.data.model.CallLogEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CallLogEntityTest {

    @Test
    fun testCallLogEntityCreation() {
        val now = System.currentTimeMillis()
        val log = CallLogEntity(
            id = 10L,
            phoneNumber = "9820443175",
            contactName = "John Doe",
            timestampEpochMs = now,
            simSlot = 0,
            status = "Dialed",
            durationSeconds = 15,
            scheduleTitle = "Sales Followup"
        )

        assertEquals(10L, log.id)
        assertEquals("9820443175", log.phoneNumber)
        assertEquals("John Doe", log.contactName)
        assertEquals(now, log.timestampEpochMs)
        assertEquals(0, log.simSlot)
        assertEquals("Dialed", log.status)
        assertEquals(15, log.durationSeconds)
        assertEquals("Sales Followup", log.scheduleTitle)
    }
}
