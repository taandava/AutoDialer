package com.autodialer.app

import com.autodialer.app.data.model.ScheduledContactEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ================================================================================================
 * WHAT IS IT: Unit test suite for [com.autodialer.app.data.model.ScheduledContactEntity].
 * WHAT IT DOES: Verifies contact queue model instantiation, foreign key binding, and queue indices.
 * WHAT THINGS IT IS LINKED WITH: [com.autodialer.app.data.model.ScheduledContactEntity]
 * ================================================================================================
 */
class ScheduledContactEntityTest {

    @Test
    fun testScheduledContactEntityCreationAndCopy() {
        val contact = ScheduledContactEntity(
            id = 5L,
            scheduleId = 2L,
            phoneNumber = "+1234567890",
            contactName = "Alice Smith",
            orderIndex = 1,
            status = "PENDING"
        )

        assertEquals(5L, contact.id)
        assertEquals(2L, contact.scheduleId)
        assertEquals("+1234567890", contact.phoneNumber)
        assertEquals("Alice Smith", contact.contactName)
        assertEquals(1, contact.orderIndex)
        assertEquals("PENDING", contact.status)

        val updated = contact.copy(status = "COMPLETED", orderIndex = 2)
        assertEquals("COMPLETED", updated.status)
        assertEquals(2, updated.orderIndex)
        assertEquals(5L, updated.id)
    }
}

