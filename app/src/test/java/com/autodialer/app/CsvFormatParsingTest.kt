package com.autodialer.app

import com.autodialer.app.data.repository.CsvRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvFormatParsingTest {

    @Test
    fun testParseCsvLineWithEmbeddedCommasAndEscapedQuotes() {
        val line = """"Doe, John","CEO, ""Top"" Corp",+1-555-0199"""
        val tokens = CsvRepository.parseCsvLine(line)

        assertEquals(3, tokens.size)
        assertEquals("Doe, John", tokens[0])
        assertEquals("CEO, \"Top\" Corp", tokens[1])
        assertEquals("+1-555-0199", tokens[2])
    }

    @Test
    fun testParseCsvLinesWithHeaderAndQuotes() {
        val csvData = """
            "Phone Number","Contact Name"
            "9820443175","John Doe"
            "+1 (555) 123-4567","Jane Smith"
            "9999999999"
        """.trimIndent()

        val parsedContacts = mutableListOf<Pair<String, String>>()
        val lines = csvData.lines()

        for ((index, line) in lines.withIndex()) {
            val currentLine = line.trim()
            if (currentLine.isEmpty()) continue

            if (index == 0 && (currentLine.lowercase().contains("name") || currentLine.lowercase().contains("phone"))) {
                continue
            }

            val parts = CsvRepository.parseCsvLine(currentLine)
            if (parts.isNotEmpty()) {
                val rawPhone = parts[0].trim()
                val name = if (parts.size > 1) parts[1].trim() else ""
                val cleanPhone = rawPhone.replace(Regex("[^0-9+]"), "")
                if (cleanPhone.isNotEmpty()) {
                    parsedContacts.add(Pair(cleanPhone, name))
                }
            }
        }

        assertEquals(3, parsedContacts.size)
        assertEquals("9820443175", parsedContacts[0].first)
        assertEquals("John Doe", parsedContacts[0].second)

        assertEquals("+15551234567", parsedContacts[1].first)
        assertEquals("Jane Smith", parsedContacts[1].second)

        assertEquals("9999999999", parsedContacts[2].first)
        assertEquals("", parsedContacts[2].second)
    }

    @Test
    fun testBulkNumbersExtraction() {
        val input = "9820443175, +1-800-555-0199; 9769602020 \n 9820011223"
        val numbers = input.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.replace(Regex("[^0-9+]"), "") }
            .filter { it.length >= 3 }

        assertEquals(4, numbers.size)
        assertTrue(numbers.contains("9820443175"))
        assertTrue(numbers.contains("+18005550199"))
        assertTrue(numbers.contains("9769602020"))
        assertTrue(numbers.contains("9820011223"))
    }
}
