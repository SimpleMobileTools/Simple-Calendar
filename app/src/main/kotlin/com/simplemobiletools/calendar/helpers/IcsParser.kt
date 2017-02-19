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
    private val END = "END:VEVENT"
    private val DTSTART = "DTSTART"
    private val DTEND = "DTEND"
    private val DURATION = "DURATION:"
    private val SUMMARY = "SUMMARY:"
    private val DESCRIPTION = "DESCRIPTION:"
    private val UID = "UID:"
    private val ACTION = "ACTION:"
    private val TRIGGER = "TRIGGER:"
    private val RRULE = "RRULE:"

    private val DISPLAY = "DISPLAY"
    private val FREQ = "FREQ"
    private val UNTIL = "UNTIL"
    private val INTERVAL = "INTERVAL"

    private val DAILY = "DAILY"
    private val WEEKLY = "WEEKLY"
    private val MONTHLY = "MONTHLY"
    private val YEARLY = "YEARLY"

    var curStart = -1
    var curEnd = -1
    var curTitle = ""
    var curDescription = ""
    var curImportId = ""
    var curFlags = 0
    var curReminderMinutes = -1
    var curRepeatInterval = 0
    var curRepeatLimit = 0
    var curShouldHaveNotification = false
    var isNotificationDescription = false

    var eventsImported = 0
    var eventsFailed = 0

    fun parseIcs(context: Context, path: String): ImportResult {
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
                        curEnd = curStart + decodeTime(duration)
                    } else if (line.startsWith(SUMMARY) && !isNotificationDescription) {
                        curTitle = line.substring(SUMMARY.length)
                        curTitle = curTitle.substring(0, Math.min(curTitle.length, 50))
                    } else if (line.startsWith(DESCRIPTION) && !isNotificationDescription) {
                        curDescription = line.substring(DESCRIPTION.length)
                    } else if (line.startsWith(UID)) {
                        curImportId = line.substring(UID.length)
                    } else if (line.startsWith(RRULE)) {
                        curRepeatInterval = parseRepeatInterval(line.substring(RRULE.length))
                    } else if (line.startsWith(ACTION)) {
                        isNotificationDescription = true
                        if (line.substring(ACTION.length) == DISPLAY)
                            curShouldHaveNotification = true
                    } else if (line.startsWith(TRIGGER)) {
                        if (curReminderMinutes == -1 && curShouldHaveNotification) {
                            curReminderMinutes = decodeTime(line.substring(TRIGGER.length)) / 60
                        }
                    } else if (line == END) {
                        if (curTitle.isEmpty() || curStart == -1 || curEnd == -1 || importIDs.contains(curImportId))
                            continue

                        importIDs.add(curImportId)
                        val event = Event(0, curStart, curEnd, curTitle, curDescription, curReminderMinutes, -1, -1, curRepeatInterval,
                                curImportId, curFlags, curRepeatLimit)
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
                val value = fullString.substring(fullString.lastIndexOf(':') + 1)
                parseDateTimeValue(value)
            } else {
                parseDateTimeValue(fullString.substring(1))
            }
        } catch (e: Exception) {
            eventsFailed++
            return -1
        }
    }

    private fun parseDateTimeValue(value: String): Int {
        val edited = value.replace("T", "").replace("Z", "")
        return if (edited.length == 14) {
            parseLongFormat(edited)
        } else {
            val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd")
            dateTimeFormat.parseDateTime(edited).withHourOfDay(1).seconds()
        }
    }

    // P0DT1H0M0S
    private fun decodeTime(duration: String): Int {
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

    private fun parseRepeatInterval(fullString: String): Int {
        val parts = fullString.split(";")
        var frequencySeconds = 0
        for (part in parts) {
            val keyValue = part.split("=")
            if (keyValue[0] == FREQ) {
                frequencySeconds = getFrequencySeconds(keyValue[1])
            } else if (keyValue[0] == INTERVAL) {
                return frequencySeconds * keyValue[1].toInt()
            } else if (keyValue[0] == UNTIL) {
                curRepeatLimit = parseDateTimeValue(keyValue[1])
            }
        }
        return frequencySeconds
    }

    private fun getFrequencySeconds(interval: String): Int {
        return when (interval) {
            DAILY -> DAY
            WEEKLY -> WEEK
            MONTHLY -> MONTH
            YEARLY -> YEAR
            else -> 0
        }
    }

    private fun resetValues() {
        curStart = -1
        curEnd = -1
        curTitle = ""
        curDescription = ""
        curImportId = ""
        curFlags = 0
        curReminderMinutes = -1
        curRepeatInterval = 0
        curRepeatLimit = 0
        curShouldHaveNotification = false
        isNotificationDescription = false
    }
}
