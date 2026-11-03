package com.autodialer.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.autodialer.app.data.model.ScheduledContactEntity
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class CsvRepository(private val contentResolver: ContentResolver) {

    fun importContactsFromCsv(uri: Uri, scheduleId: Long = 0): List<ScheduledContactEntity> {
        val contacts = mutableListOf<ScheduledContactEntity>()
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var lineIndex = 0
                    var phoneColumnIndex = -1
                    var nameColumnIndex = -1

                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.isEmpty()) continue

                        val tokens = parseCsvLine(currentLine)
                        if (tokens.isEmpty()) continue

                        // Header detection on first line
                        if (lineIndex == 0) {
                            val lowerTokens = tokens.map { it.lowercase() }
                            val phoneIdx = lowerTokens.indexOfFirst {
                                it.contains("phone") || it.contains("mobile") || it.contains("number") || it.contains("tel") || it.contains("cell")
                            }
                            val nameIdx = lowerTokens.indexOfFirst {
                                it.contains("name") || it.contains("contact") || it.contains("person") || it.contains("display")
                            }

                            if (phoneIdx != -1 || nameIdx != -1) {
                                phoneColumnIndex = if (phoneIdx != -1) phoneIdx else if (nameIdx == 0) 1 else 0
                                nameColumnIndex = if (nameIdx != -1) nameIdx else if (phoneIdx == 0) 1 else 0
                                lineIndex++
                                continue
                            }
                        }

                        // Determine which token is phone and which is name
                        var rawPhone = ""
                        var name = ""

                        if (phoneColumnIndex != -1 && phoneColumnIndex < tokens.size) {
                            rawPhone = tokens[phoneColumnIndex]
                            if (nameColumnIndex != -1 && nameColumnIndex < tokens.size) {
                                name = tokens[nameColumnIndex]
                            }
                        } else {
                            // Heuristic fallback: check if first token is phone or name
                            val first = tokens[0]
                            val second = if (tokens.size > 1) tokens[1] else ""

                            val firstDigits = first.replace(Regex("[^0-9+]"), "")
                            val secondDigits = second.replace(Regex("[^0-9+]"), "")

                            if (firstDigits.length >= 3 && secondDigits.length < 3) {
                                rawPhone = first
                                name = second
                            } else if (secondDigits.length >= 3 && firstDigits.length < 3) {
                                rawPhone = second
                                name = first
                            } else {
                                rawPhone = first
                                name = second
                            }
                        }

                        val cleanPhone = rawPhone.replace(Regex("[^0-9+]"), "")
                        if (cleanPhone.length >= 3) {
                            contacts.add(
                                ScheduledContactEntity(
                                    scheduleId = scheduleId,
                                    phoneNumber = cleanPhone,
                                    contactName = name.trim(),
                                    orderIndex = contacts.size
                                )
                            )
                        }
                        lineIndex++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contacts
    }

    fun exportContactsToCsv(uri: Uri, contacts: List<ScheduledContactEntity>): Boolean {
        return try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write("Phone Number,Contact Name\n")
                    for ((_, _, phoneNumber, contactName) in contacts) {
                        val escapedName = contactName.replace("\"", "\"\"")
                        writer.write("\"$phoneNumber\",\"$escapedName\"\n")
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        fun parseCsvLine(line: String): List<String> {
            val tokens = mutableListOf<String>()
            val sb = java.lang.StringBuilder()
            var inQuotes = false
            var i = 0

            while (i < line.length) {
                val c = line[i]
                if (c == '"') {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                } else if (c == ',' && !inQuotes) {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                } else {
                    sb.append(c)
                }
                i++
            }
            tokens.add(sb.toString().trim())
            return tokens
        }
    }
}
