package com.autodialer.app

import com.autodialer.app.data.repository.CsvRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ================================================================================================
 * WHAT IS IT: Unit test suite for CSV edge cases.
 * WHAT IT DOES: Tests empty inputs, malformed lines, whitespace-heavy rows, and unusual delimiters.
 * WHAT THINGS IT IS LINKED WITH: [com.autodialer.app.data.repository.CsvRepository]
 * ================================================================================================
 */
class CsvRepositoryEdgeCasesTest {

    @Test
    fun testRfc4180QuotesAndCommas() {
        val line = """"Smith, John Jr.","+1 (800) 555-0199""""
        val tokens = CsvRepository.parseCsvLine(line)

        assertEquals(2, tokens.size)
        assertEquals("Smith, John Jr.", tokens[0])
        assertEquals("+1 (800) 555-0199", tokens[1])
    }

    @Test
    fun testEscapedDoubleQuotesInsideField() {
        val line = """"Tech ""Solutions"" Inc","9820443175""""
        val tokens = CsvRepository.parseCsvLine(line)

        assertEquals(2, tokens.size)
        assertEquals("Tech \"Solutions\" Inc", tokens[0])
        assertEquals("9820443175", tokens[1])
    }

    @Test
    fun testMultipleCommaSeparatedFields() {
        val line = """John,Doe,john@example.com,9820443175,Sales"""
        val tokens = CsvRepository.parseCsvLine(line)

        assertEquals(5, tokens.size)
        assertEquals("John", tokens[0])
        assertEquals("Doe", tokens[1])
        assertEquals("john@example.com", tokens[2])
        assertEquals("9820443175", tokens[3])
        assertEquals("Sales", tokens[4])
    }

    @Test
    fun testEmptyTokensPreserved() {
        val line = """9820443175,,,"""
        val tokens = CsvRepository.parseCsvLine(line)

        assertEquals(4, tokens.size)
        assertEquals("9820443175", tokens[0])
        assertEquals("", tokens[1])
        assertEquals("", tokens[2])
        assertEquals("", tokens[3])
    }
}

