package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.widget.Toast
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.helpers.IcsImporter.ImportResult.*
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.areDigitsOnly
import com.simplemobiletools.commons.extensions.showErrorToast
import java.io.File

class IcsImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

    private var curStart = -1
    private var curEnd = -1
    private var curTitle = ""
    private var curDescription = ""
    private var curImportId = ""
    private var curFlags = 0
    private var curReminderMinutes = ArrayList<Int>()
    private var curRepeatExceptions = ArrayList<Int>()
    private var curRepeatInterval = 0
    private var curRepeatLimit = 0
    private var curRepeatRule = 0
    private var curEventType = DBHelper.REGULAR_EVENT_TYPE_ID
    private var curLastModified = 0L
    private var curLocation = ""
    private var curCategoryColor = -2
    private var isNotificationDescription = false
    private var isProperReminderAction = false
    private var isDescription = false
    private var curReminderTriggerMinutes = -1

    private var eventsImported = 0
    private var eventsFailed = 0

    fun importEvents(path: String, defaultEventType: Int, calDAVCalendarId: Int): ImportResult {
        try {
            val existingEvents = activity.dbHelper.getEventsWithImportIds()
            var prevLine = ""

            val inputStream = if (path.contains("/")) {
                File(path).inputStream()
            } else {
                activity.assets.open(path)
            }

            inputStream.bufferedReader().use {
                while (true) {
                    var line = it.readLine() ?: break
                    if (line.trim().isEmpty()) {
                        continue
                    }

                    if (line.substring(0, 1) == " ") {
                        line = prevLine + line.trim()
                        eventsFailed--
                    }

                    if (isDescription) {
                        if (line.startsWith('\t')) {
                            curDescription += line.trimStart('\t').replace("\\n", "\n")
                        } else {
                            isDescription = false
                        }
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
                        curEnd = curStart + Parser().parseDurationSeconds(duration)
                    } else if (line.startsWith(SUMMARY) && !isNotificationDescription) {
                        curTitle = line.substring(SUMMARY.length)
                        curTitle = getTitle(curTitle).replace("\\n", "\n")
                    } else if (line.startsWith(DESCRIPTION) && !isNotificationDescription) {
                        curDescription = line.substring(DESCRIPTION.length).replace("\\n", "\n")
                        isDescription = true
                    } else if (line.startsWith(UID)) {
                        curImportId = line.substring(UID.length).trim()
                    } else if (line.startsWith(RRULE)) {
                        val repeatRule = Parser().parseRepeatInterval(line.substring(RRULE.length), curStart)
                        curRepeatRule = repeatRule.repeatRule
                        curRepeatInterval = repeatRule.repeatInterval
                        curRepeatLimit = repeatRule.repeatLimit
                    } else if (line.startsWith(ACTION)) {
                        isNotificationDescription = true
                        isProperReminderAction = line.substring(ACTION.length) == DISPLAY
                    } else if (line.startsWith(TRIGGER)) {
                        curReminderTriggerMinutes = Parser().parseDurationSeconds(line.substring(TRIGGER.length)) / 60
                    } else if (line.startsWith(CATEGORY_COLOR)) {
                        val color = line.substring(CATEGORY_COLOR.length)
                        if (color.trimStart('-').areDigitsOnly()) {
                            curCategoryColor = Integer.parseInt(color)
                        }
                    } else if (line.startsWith(CATEGORIES)) {
                        val categories = line.substring(CATEGORIES.length)
                        tryAddCategories(categories, activity)
                    } else if (line.startsWith(LAST_MODIFIED)) {
                        curLastModified = getTimestamp(line.substring(LAST_MODIFIED.length)) * 1000L
                    } else if (line.startsWith(EXDATE)) {
                        var value = line.substring(EXDATE.length)
                        if (value.endsWith('}')) {
                            value = value.substring(0, value.length - 1)
                        }

                        curRepeatExceptions.add(getTimestamp(value))
                    } else if (line.startsWith(LOCATION)) {
                        curLocation = line.substring(LOCATION.length)
                    } else if (line == END_ALARM) {
                        if (isProperReminderAction && curReminderTriggerMinutes != -1) {
                            curReminderMinutes.add(curReminderTriggerMinutes)
                        }
                    } else if (line == END_EVENT) {
                        if (curStart != -1 && curEnd == -1) {
                            curEnd = curStart
                        }

                        if (curTitle.isEmpty() || curStart == -1) {
                            continue
                        }


                        val eventToUpdate = existingEvents.firstOrNull { curImportId.isNotEmpty() && curImportId == it.importId }
                        if (eventToUpdate != null && eventToUpdate.lastUpdated >= curLastModified) {
                            continue
                        }

                        val source = if (calDAVCalendarId == 0) SOURCE_IMPORTED_ICS else "$CALDAV-$calDAVCalendarId"
                        val event = Event(0, curStart, curEnd, curTitle, curDescription, curReminderMinutes.getOrElse(0, { -1 }),
                                curReminderMinutes.getOrElse(1, { -1 }), curReminderMinutes.getOrElse(2, { -1 }), curRepeatInterval,
                                curImportId, curFlags, curRepeatLimit, curRepeatRule, curEventType, lastUpdated = curLastModified,
                                source = source, location = curLocation)

                        if (event.getIsAllDay() && curEnd > curStart) {
                            event.endTS -= DAY
                        }

                        if (eventToUpdate == null) {
                            activity.dbHelper.insert(event, true) {
                                for (exceptionTS in curRepeatExceptions) {
                                    activity.dbHelper.addEventRepeatException(it, exceptionTS, true)
                                }
                                existingEvents.add(event)
                            }
                        } else {
                            event.id = eventToUpdate.id
                            activity.dbHelper.update(event, true)
                        }
                        eventsImported++
                        resetValues()
                    }
                    prevLine = line
                }
            }
        } catch (e: Exception) {
            activity.showErrorToast(e, Toast.LENGTH_LONG)
            eventsFailed++
        }

        return when {
            eventsImported == 0 -> IMPORT_FAIL
            eventsFailed > 0 -> IMPORT_PARTIAL
            else -> IMPORT_OK
        }
    }

    private fun getTimestamp(fullString: String): Int {
        return try {
            if (fullString.startsWith(';')) {
                val value = fullString.substring(fullString.lastIndexOf(':') + 1)
                if (!value.contains("T")) {
                    curFlags = curFlags or FLAG_ALL_DAY
                }

                Parser().parseDateTimeValue(value)
            } else {
                Parser().parseDateTimeValue(fullString.substring(1))
            }
        } catch (e: Exception) {
            activity.showErrorToast(e, Toast.LENGTH_LONG)
            eventsFailed++
            -1
        }
    }

    private fun tryAddCategories(categories: String, context: Context) {
        val eventTypeTitle = if (categories.contains(",")) {
            categories.split(",")[0]
        } else {
            categories
        }

        val eventId = context.dbHelper.getEventTypeIdWithTitle(eventTypeTitle)
        curEventType = if (eventId == -1) {
            val newTypeColor = if (curCategoryColor == -2) context.resources.getColor(R.color.color_primary) else curCategoryColor
            val eventType = EventType(0, eventTypeTitle, newTypeColor)
            context.dbHelper.insertEventType(eventType)
        } else {
            eventId
        }
    }

    private fun getTitle(title: String): String {
        return if (title.startsWith(";") && title.contains(":")) {
            title.substring(title.lastIndexOf(':') + 1)
        } else {
            title.substring(1, Math.min(title.length, 80))
        }
    }

    private fun resetValues() {
        curStart = -1
        curEnd = -1
        curTitle = ""
        curDescription = ""
        curImportId = ""
        curFlags = 0
        curReminderMinutes = ArrayList()
        curRepeatExceptions = ArrayList()
        curRepeatInterval = 0
        curRepeatLimit = 0
        curRepeatRule = 0
        curEventType = DBHelper.REGULAR_EVENT_TYPE_ID
        curLastModified = 0L
        curCategoryColor = -2
        curLocation = ""
        isNotificationDescription = false
        isProperReminderAction = false
        curReminderTriggerMinutes = -1
    }
}
