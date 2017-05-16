package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.isXMonthlyRepetition
import com.simplemobiletools.calendar.extensions.isXWeeklyRepetition
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.IcsImporter.ImportResult.*
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.EventType
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.io.File

class IcsImporter {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    var curStart = -1
    var curEnd = -1
    var curTitle = ""
    var curDescription = ""
    var curImportId = ""
    var curFlags = 0
    var curReminderMinutes = ArrayList<Int>()
    var curRepeatExceptions = ArrayList<Int>()
    var curRepeatInterval = 0
    var curRepeatLimit = 0
    var curRepeatRule = 0
    var curEventType = DBHelper.REGULAR_EVENT_TYPE_ID
    var isNotificationDescription = false
    var lastReminderAction = ""

    var eventsImported = 0
    var eventsFailed = 0

    fun importEvents(context: Context, path: String, defaultEventType: Int): ImportResult {
        try {
            val importIDs = context.dbHelper.getImportIds()
            var prevLine = ""

            val inputStream = if (path.contains("/")) {
                File(path).inputStream()
            } else {
                context.assets.open(path)
            }

            inputStream.bufferedReader().use {
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
                        curEventType = defaultEventType
                    } else if (line.startsWith(DTSTART)) {
                        curStart = getTimestamp(line.substring(DTSTART.length))
                    } else if (line.startsWith(DTEND)) {
                        curEnd = getTimestamp(line.substring(DTEND.length))
                    } else if (line.startsWith(DURATION)) {
                        val duration = line.substring(DURATION.length)
                        curEnd = curStart + decodeTime(duration)
                    } else if (line.startsWith(SUMMARY) && !isNotificationDescription) {
                        curTitle = line.substring(SUMMARY.length)
                        curTitle = getTitle(curTitle).replace("\\n", "\n")
                    } else if (line.startsWith(DESCRIPTION) && !isNotificationDescription) {
                        curDescription = line.substring(DESCRIPTION.length).replace("\\n", "\n")
                    } else if (line.startsWith(UID)) {
                        curImportId = line.substring(UID.length)
                    } else if (line.startsWith(RRULE)) {
                        curRepeatInterval = parseRepeatInterval(line.substring(RRULE.length))
                    } else if (line.startsWith(ACTION)) {
                        isNotificationDescription = true
                        lastReminderAction = line.substring(ACTION.length)
                    } else if (line.startsWith(TRIGGER)) {
                        if (lastReminderAction == DISPLAY)
                            curReminderMinutes.add(decodeTime(line.substring(TRIGGER.length)) / 60)
                    } else if (line.startsWith(CATEGORIES)) {
                        val categories = line.substring(CATEGORIES.length)
                        tryAddCategories(categories, context)
                    } else if (line.startsWith(EXDATE)) {
                        curRepeatExceptions.add(getTimestamp(line.substring(EXDATE.length)))
                    } else if (line == END_EVENT) {
                        if (curTitle.isEmpty() || curStart == -1 || curEnd == -1 || importIDs.contains(curImportId))
                            continue

                        if (curImportId.isNotEmpty())
                            importIDs.add(curImportId)

                        val event = Event(0, curStart, curEnd, curTitle, curDescription, curReminderMinutes.getOrElse(0, { -1 }),
                                curReminderMinutes.getOrElse(1, { -1 }), curReminderMinutes.getOrElse(2, { -1 }), curRepeatInterval,
                                curImportId, curFlags, curRepeatLimit, curRepeatRule, curEventType)

                        if (event.isAllDay && curEnd > curStart) {
                            event.endTS -= DAY
                        }

                        val eventId = context.dbHelper.insert(event)

                        for (exceptionTS in curRepeatExceptions) {
                            context.dbHelper.addEventRepeatException(eventId, exceptionTS)
                        }
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
                val value = fullString.substring(fullString.lastIndexOf(':') + 1)
                if (!value.contains("T"))
                    curFlags = curFlags or FLAG_ALL_DAY

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
            parseLongFormat(edited, value.endsWith("Z"))
        } else {
            val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd")
            dateTimeFormat.parseDateTime(edited).withZoneRetainFields(DateTimeZone.getDefault()).withHourOfDay(1).seconds()
        }
    }

    private fun tryAddCategories(categories: String, context: Context) {
        val eventTypeTitle = if (categories.contains(",")) {
            categories.split(",")[0]
        } else {
            categories
        }

        val eventId = context.dbHelper.getEventTypeIdWithTitle(eventTypeTitle)
        if (eventId == -1) {
            val eventType = EventType(0, eventTypeTitle, context.resources.getColor(R.color.color_primary))
            curEventType = context.dbHelper.insertEventType(eventType)
        } else {
            curEventType = eventId
        }
    }

    private fun getTitle(title: String): String {
        return if (title.startsWith(";") && title.contains(":")) {
            title.substring(title.lastIndexOf(':') + 1)
        } else {
            title.substring(1, Math.min(title.length, 80))
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

    private fun parseLongFormat(digitString: String, useUTC: Boolean): Int {
        val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss")
        val dateTimeZone = if (useUTC) DateTimeZone.UTC else DateTimeZone.getDefault()
        return dateTimeFormat.parseDateTime(digitString).withZoneRetainFields(dateTimeZone).seconds()
    }

    private fun parseRepeatInterval(fullString: String): Int {
        val parts = fullString.split(";")
        var frequencySeconds = 0
        for (part in parts) {
            val keyValue = part.split("=")
            val key = keyValue[0]
            val value = keyValue[1]
            if (key == FREQ) {
                frequencySeconds = getFrequencySeconds(value)
                if (value == WEEKLY) {
                    val start = Formatter.getDateTimeFromTS(curStart)
                    curRepeatRule = Math.pow(2.0, (start.dayOfWeek - 1).toDouble()).toInt()
                } else if (value == MONTHLY) {
                    curRepeatRule = REPEAT_MONTH_SAME_DAY
                }
            } else if (key == COUNT) {
                curRepeatLimit = -value.toInt()
            } else if (key == UNTIL) {
                curRepeatLimit = parseDateTimeValue(value)
            } else if (key == INTERVAL) {
                frequencySeconds *= value.toInt()
            } else if (key == BYDAY) {
                if (frequencySeconds.isXWeeklyRepetition()) {
                    handleRepeatRule(value)
                } else if (frequencySeconds.isXMonthlyRepetition()) {
                    curRepeatRule = REPEAT_MONTH_EVERY_XTH_DAY
                }
            } else if (key == BYMONTHDAY && value.toInt() == -1) {
                curRepeatRule = REPEAT_MONTH_LAST_DAY
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

    private fun handleRepeatRule(value: String) {
        if (value.contains(MO))
            curRepeatRule = curRepeatRule or MONDAY
        if (value.contains(TU))
            curRepeatRule = curRepeatRule or TUESDAY
        if (value.contains(WE))
            curRepeatRule = curRepeatRule or WEDNESDAY
        if (value.contains(TH))
            curRepeatRule = curRepeatRule or THURSDAY
        if (value.contains(FR))
            curRepeatRule = curRepeatRule or FRIDAY
        if (value.contains(SA))
            curRepeatRule = curRepeatRule or SATURDAY
        if (value.contains(SU))
            curRepeatRule = curRepeatRule or SUNDAY
    }

    private fun resetValues() {
        curStart = -1
        curEnd = -1
        curTitle = ""
        curDescription = ""
        curImportId = ""
        curFlags = 0
        curReminderMinutes = ArrayList<Int>()
        curRepeatExceptions = ArrayList<Int>()
        curRepeatInterval = 0
        curRepeatLimit = 0
        curRepeatRule = 0
        curEventType = DBHelper.REGULAR_EVENT_TYPE_ID
        isNotificationDescription = false
        lastReminderAction = ""
    }
}
