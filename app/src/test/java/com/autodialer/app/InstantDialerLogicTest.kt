package com.autodialer.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstantDialerLogicTest {

    @Test
    fun testPhoneNumberParsingWithDiverseDelimiters() {
        val rawInput = " +1 (800) 555-0199 , 9820443175 ; +91-9769602020 \n 99999-88888 ; invalid "
        val parsed = rawInput.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.replace(Regex("[^0-9+]"), "") }
            .filter { it.length >= 3 }

        assertEquals(4, parsed.size)
        assertEquals("+18005550199", parsed[0])
        assertEquals("9820443175", parsed[1])
        assertEquals("+919769602020", parsed[2])
        assertEquals("9999988888", parsed[3])
    }

    @Test
    fun testDtmfSanitizationLogic() {
        val rawDtmf = "101#*abcd9"
        val sanitized = rawDtmf.filter { it.isDigit() || it == '*' || it == '#' }
        assertEquals("101#*9", sanitized)
    }

    @Test
    fun testRepetitionAndIntervalClamping() {
        val repetitionInput = 150000
        val clampedRepetitions = repetitionInput.coerceIn(1, 100000)
        assertEquals(100000, clampedRepetitions)

        val intervalInput = -5
        val clampedInterval = intervalInput.coerceIn(1, 3600)
        assertEquals(1, clampedInterval)

        val durationInput = 7200
        val clampedDuration = durationInput.coerceIn(1, 3600)
        assertEquals(3600, clampedDuration)
    }
}

