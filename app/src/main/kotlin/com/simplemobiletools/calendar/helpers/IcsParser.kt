package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.IcsParser.ImportResult.*
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.io.File

class IcsParser {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    private val BEGIN_EVENT = "BEGIN:VEVENT"
    private val BEGIN_ALARM = "BEGIN:VALARM"
    private val END = "END:VEVENT"
    private val DTSTART = "DTSTART"
    private val DTEND = "DTEND"
    private val DURATION = "DURATION:"
    private val SUMMARY = "SUMMARY:"
    private val DESCRIPTION = "DESCRIPTION:"
    private val UID = "UID:"

    var curStart = -1
    var curEnd = -1
    var curTitle = ""
    var curDescription = ""
    var curImportId = ""
    var curFlags = 0

    var eventsImported = 0
    var eventsFailed = 0

    fun parseIcs(context: Context, reminderMinutes: Int, path: String): ImportResult {
        try {
            val dbHelper = DBHelper.newInstance(context)
            val importIDs = dbHelper.getImportIds()
            var prevLine = ""

            File(path).inputStream().bufferedReader().use {
                while (true) {
                    var line = it.readLine() ?: break
                    if (line.trim().isEmpty())
                        continue

                    if (line.substring(0, 1) == " ") {
                        line = prevLine + line.trim()
                        eventsFailed--
                    }

                    if (line == BEGIN_EVENT) {
                        resetValues()
                    } else if (line.startsWith(DTSTART)) {
                        curStart = getTimestamp(line.substring(DTSTART.length))
                    } else if (line.startsWith(DTEND)) {
                        curEnd = getTimestamp(line.substring(DTEND.length))
                    } else if (line.startsWith(DURATION)) {
                        val duration = line.substring(DURATION.length)
                        curEnd = curStart + calculateEndTime(duration)
                    } else if (line.startsWith(SUMMARY)) {
                        curTitle = line.substring(SUMMARY.length)
                        curTitle = curTitle.substring(0, Math.min(curTitle.length, 50))
                    } else if (line.startsWith(DESCRIPTION)) {
                        curDescription = line.substring(DESCRIPTION.length)
                    } else if (line.startsWith(UID)) {
                        curImportId = line.substring(UID.length)
                    } else if (line == END || line == BEGIN_ALARM) {
                        if (curTitle.isEmpty() || curStart == -1 || curEnd == -1 || importIDs.contains(curImportId))
                            continue

                        val event = Event(0, curStart, curEnd, curTitle, curDescription, reminderMinutes, -1, -1, importId = curImportId, flags = curFlags)
                        dbHelper.insert(event) { }
                        eventsImported++
                        resetValues()
                    }
                    prevLine = line
                }
            }
        } catch (e: Exception) {
            eventsFailed++
        }

        return if (eventsImported == 0) {
            IMPORT_FAIL
        } else if (eventsFailed > 0) {
            IMPORT_PARTIAL
        } else {
            IMPORT_OK
        }
    }

    private fun getTimestamp(fullString: String): Int {
        try {
            return if (fullString.startsWith(';')) {
                curFlags = curFlags or FLAG_ALL_DAY
                val value = fullString.substring(fullString.lastIndexOf(':') + 1).replace("T", "").replace("Z", "")
                if (value.length == 14) {
                    parseLongFormat(value)
                } else {
                    val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd")
                    dateTimeFormat.parseDateTime(value).withHourOfDay(1).seconds()
                }
            } else {
                val digitString = fullString.substring(1).replace("T", "").replace("Z", "")
                parseLongFormat(digitString)
            }
        } catch (e: Exception) {
            eventsFailed++
            return -1
        }
    }

    // P0DT1H0M0S
    private fun calculateEndTime(duration: String): Int {
        val weeks = getDurationValue(duration, "W")
        val days = getDurationValue(duration, "DT")
        val hours = getDurationValue(duration, "H")
        val minutes = getDurationValue(duration, "M")
        val seconds = getDurationValue(duration, "S")

        val minSecs = 60
        val hourSecs = minSecs * 60
        val daySecs = hourSecs * 24
        val weekSecs = daySecs * 7

        return seconds + (minutes * minSecs) + (hours * hourSecs) + (days * daySecs) + (weeks * weekSecs)
    }

    private fun getDurationValue(duration: String, char: String): Int = Regex("[0-9]+(?=$char)").find(duration)?.value?.toInt() ?: 0

    private fun parseLongFormat(digitString: String): Int {
        val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss")
        return dateTimeFormat.parseDateTime(digitString).withZoneRetainFields(DateTimeZone.UTC).seconds()
    }

    private fun resetValues() {
        curStart = -1
        curEnd = -1
        curTitle = ""
        curDescription = ""
        curImportId = ""
        curFlags = 0
    }
}
