package com.simplemobiletools.calendar.pro.helpers

import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.extensions.eventsDB
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.helpers.IcsImporter.ImportResult.*
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.calendar.pro.models.Reminder
import com.simplemobiletools.commons.extensions.areDigitsOnly
import com.simplemobiletools.commons.extensions.showErrorToast
import org.joda.time.DateTimeZone
import java.io.File

class IcsImporter(val activity: SimpleActivity) {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL, IMPORT_NOTHING_NEW
    }

    private var curStart = -1L
    private var curEnd = -1L
    private var curTitle = ""
    private var curLocation = ""
    private var curDescription = ""
    private var curImportId = ""
    private var curRecurrenceDayCode = ""
    private var curRrule = ""
    private var curFlags = 0
    private var curReminderMinutes = ArrayList<Int>()
    private var curReminderActions = ArrayList<Int>()
    private var curRepeatExceptions = ArrayList<String>()
    private var curRepeatInterval = 0
    private var curRepeatLimit = 0L
    private var curRepeatRule = 0
    private var curEventTypeId = REGULAR_EVENT_TYPE_ID
    private var curLastModified = 0L
    private var curCategoryColor = -2
    private var isNotificationDescription = false
    private var isProperReminderAction = false
    private var isDescription = false
    private var isSequence = false
    private var isParsingEvent = false
    private var curReminderTriggerMinutes = REMINDER_OFF
    private var curReminderTriggerAction = REMINDER_NOTIFICATION
    private val eventsHelper = activity.eventsHelper

    private var eventsImported = 0
    private var eventsFailed = 0
    private var eventsAlreadyExist = 0

    fun importEvents(path: String, defaultEventTypeId: Long, calDAVCalendarId: Int, overrideFileEventTypes: Boolean): ImportResult {
        try {
            val eventTypes = eventsHelper.getEventTypesSync()
            val existingEvents = activity.eventsDB.getEventsWithImportIds().toMutableList() as ArrayList<Event>
            val eventsToInsert = ArrayList<Event>()
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
                        curEventTypeId = defaultEventTypeId
                        isParsingEvent = true
                    } else if (line.startsWith(DTSTART)) {
                        if (isParsingEvent) {
                            curStart = getTimestamp(line.substring(DTSTART.length))

                            if (curRrule != "") {
                                parseRepeatRule()
                            }
                        }
                    } else if (line.startsWith(DTEND)) {
                        curEnd = getTimestamp(line.substring(DTEND.length))
                    } else if (line.startsWith(DURATION)) {
                        val duration = line.substring(DURATION.length)
                        curEnd = curStart + Parser().parseDurationSeconds(duration)
                    } else if (line.startsWith(SUMMARY) && !isNotificationDescription) {
                        curTitle = line.substring(SUMMARY.length)
                        curTitle = getTitle(curTitle).replace("\\n", "\n").replace("\\,", ",")
                    } else if (line.startsWith(DESCRIPTION) && !isNotificationDescription) {
                        curDescription = line.substring(DESCRIPTION.length).replace("\\n", "\n").replace("\\,", ",")
                        if (curDescription.trim().isEmpty()) {
                            curDescription = ""
                        }
                        isDescription = true
                    } else if (line.startsWith(UID)) {
                        curImportId = line.substring(UID.length).trim()
                    } else if (line.startsWith(RRULE)) {
                        curRrule = line.substring(RRULE.length)
                        // some RRULEs need to know the events start datetime. If it's yet unknown, postpone RRULE parsing
                        if (curStart != -1L) {
                            parseRepeatRule()
                        }
                    } else if (line.startsWith(ACTION)) {
                        isNotificationDescription = true
                        val action = line.substring(ACTION.length)
                        isProperReminderAction = action == DISPLAY || action == EMAIL
                        if (isProperReminderAction) {
                            curReminderTriggerAction = if (action == DISPLAY) REMINDER_NOTIFICATION else REMINDER_EMAIL
                        }
                    } else if (line.startsWith(TRIGGER)) {
                        curReminderTriggerMinutes = Parser().parseDurationSeconds(line.substring(TRIGGER.length)) / 60
                    } else if (line.startsWith(CATEGORY_COLOR)) {
                        val color = line.substring(CATEGORY_COLOR.length)
                        if (color.trimStart('-').areDigitsOnly()) {
                            curCategoryColor = Integer.parseInt(color)
                        }
                    } else if (line.startsWith(CATEGORIES) && !overrideFileEventTypes) {
                        val categories = line.substring(CATEGORIES.length)
                        tryAddCategories(categories)
                    } else if (line.startsWith(LAST_MODIFIED)) {
                        curLastModified = getTimestamp(line.substring(LAST_MODIFIED.length)) * 1000L
                    } else if (line.startsWith(EXDATE)) {
                        var value = line.substring(EXDATE.length)
                        if (value.endsWith('}')) {
                            value = value.substring(0, value.length - 1)
                        }

                        curRepeatExceptions.add(Formatter.getDayCodeFromTS(getTimestamp(value)))
                    } else if (line.startsWith(LOCATION)) {
                        curLocation = getLocation(line.substring(LOCATION.length).replace("\\,", ","))
                        if (curLocation.trim().isEmpty()) {
                            curLocation = ""
                        }
                    } else if (line.startsWith(RECURRENCE_ID)) {
                        val timestamp = getTimestamp(line.substring(RECURRENCE_ID.length))
                        curRecurrenceDayCode = Formatter.getDayCodeFromTS(timestamp)
                    } else if (line.startsWith(SEQUENCE)) {
                        isSequence = true
                    } else if (line == END_ALARM) {
                        if (isProperReminderAction && curReminderTriggerMinutes != REMINDER_OFF) {
                            curReminderMinutes.add(curReminderTriggerMinutes)
                            curReminderActions.add(curReminderTriggerAction)
                        }
                    } else if (line == END_EVENT) {
                        isParsingEvent = false
                        if (curStart != -1L && curEnd == -1L) {
                            curEnd = curStart
                        }

                        if (curTitle.isEmpty() || curStart == -1L) {
                            continue
                        }

                        // repeating event exceptions can have the same import id as their parents, so pick the latest event to update
                        val eventToUpdate = existingEvents.filter { curImportId.isNotEmpty() && curImportId == it.importId }.sortedByDescending { it.lastUpdated }.firstOrNull()
                        if (eventToUpdate != null && eventToUpdate.lastUpdated >= curLastModified) {
                            eventsAlreadyExist++
                            continue
                        }

                        var reminders = arrayListOf(
                            Reminder(curReminderMinutes.getOrElse(0) { REMINDER_OFF }, curReminderActions.getOrElse(0) { REMINDER_NOTIFICATION }),
                            Reminder(curReminderMinutes.getOrElse(1) { REMINDER_OFF }, curReminderActions.getOrElse(1) { REMINDER_NOTIFICATION }),
                            Reminder(curReminderMinutes.getOrElse(2) { REMINDER_OFF }, curReminderActions.getOrElse(2) { REMINDER_NOTIFICATION })
                        )

                        reminders = reminders.sortedBy { it.minutes }.sortedBy { it.minutes == REMINDER_OFF }.toMutableList() as ArrayList<Reminder>

                        val eventType = eventTypes.firstOrNull { it.id == curEventTypeId }
                        val source = if (calDAVCalendarId == 0 || eventType?.isSyncedEventType() == false) SOURCE_IMPORTED_ICS else "$CALDAV-$calDAVCalendarId"
                        val event = Event(null, curStart, curEnd, curTitle, curLocation, curDescription, reminders[0].minutes,
                            reminders[1].minutes, reminders[2].minutes, reminders[0].type, reminders[1].type, reminders[2].type, curRepeatInterval, curRepeatRule,
                            curRepeatLimit, curRepeatExceptions, "", curImportId, DateTimeZone.getDefault().id, curFlags, curEventTypeId, 0, curLastModified, source)

                        if (event.getIsAllDay() && curEnd > curStart) {
                            event.endTS -= DAY
                        }

                        if (event.importId.isEmpty()) {
                            event.importId = event.hashCode().toString()
                            if (existingEvents.map { it.importId }.contains(event.importId)) {
                                eventsAlreadyExist++
                                continue
                            }
                        }

                        if (eventToUpdate == null) {
                            // if an event belongs to a sequence insert it immediately, to avoid some glitches with linked events
                            if (isSequence) {
                                if (curRecurrenceDayCode.isEmpty()) {
                                    eventsHelper.insertEvent(event, true, false)
                                } else {
                                    // if an event contains the RECURRENCE-ID field, it is an exception to a recurring event, so update its parent too
                                    val parentEvent = activity.eventsDB.getEventWithImportId(event.importId)
                                    if (parentEvent != null && !parentEvent.repetitionExceptions.contains(curRecurrenceDayCode)) {
                                        parentEvent.addRepetitionException(curRecurrenceDayCode)
                                        eventsHelper.insertEvent(parentEvent, true, false)

                                        event.parentId = parentEvent.id!!
                                        eventsToInsert.add(event)
                                    }
                                }
                            } else {
                                eventsToInsert.add(event)
                            }
                        } else {
                            event.id = eventToUpdate.id
                            eventsHelper.updateEvent(event, true, false)
                        }
                        eventsImported++
                        resetValues()
                    }
                    prevLine = line
                }
            }

            eventsHelper.insertEvents(eventsToInsert, true)
        } catch (e: Exception) {
            activity.showErrorToast(e)
            eventsFailed++
        }

        return when {
            eventsImported == 0 -> {
                if (eventsAlreadyExist > 0) {
                    IMPORT_NOTHING_NEW
                } else {
                    IMPORT_FAIL
                }
            }
            eventsFailed > 0 -> IMPORT_PARTIAL
            else -> IMPORT_OK
        }
    }

    private fun getTimestamp(fullString: String): Long {
        return try {
            if (fullString.startsWith(';')) {
                val value = fullString.substring(fullString.lastIndexOf(':') + 1).replace(" ", "")
                if (value.isEmpty()) {
                    return 0
                } else if (!value.contains("T")) {
                    curFlags = curFlags or FLAG_ALL_DAY
                }

                Parser().parseDateTimeValue(value)
            } else {
                Parser().parseDateTimeValue(fullString.substring(1))
            }
        } catch (e: Exception) {
            activity.showErrorToast(e)
            eventsFailed++
            -1
        }
    }

    private fun getLocation(fullString: String): String {
        return if (fullString.startsWith(":")) {
            fullString.trimStart(':')
        } else {
            fullString.substringAfter(':').trim()
        }
    }

    private fun tryAddCategories(categories: String) {
        val eventTypeTitle = if (categories.contains(",")) {
            categories.split(",")[0]
        } else {
            categories
        }

        val eventId = eventsHelper.getEventTypeIdWithTitle(eventTypeTitle)
        curEventTypeId = if (eventId == -1L) {
            val newTypeColor = if (curCategoryColor == -2) activity.resources.getColor(R.color.color_primary) else curCategoryColor
            val eventType = EventType(null, eventTypeTitle, newTypeColor)
            eventsHelper.insertOrUpdateEventTypeSync(eventType)
        } else {
            eventId
        }
    }

    private fun getTitle(title: String): String {
        return if (title.startsWith(";") && title.contains(":")) {
            title.substring(title.lastIndexOf(':') + 1)
        } else {
            title.substring(1, Math.min(title.length, 180))
        }
    }

    private fun parseRepeatRule() {
        val repeatRule = Parser().parseRepeatInterval(curRrule, curStart)
        curRepeatRule = repeatRule.repeatRule
        curRepeatInterval = repeatRule.repeatInterval
        curRepeatLimit = repeatRule.repeatLimit
    }

    private fun resetValues() {
        curStart = -1L
        curEnd = -1L
        curTitle = ""
        curLocation = ""
        curDescription = ""
        curImportId = ""
        curRecurrenceDayCode = ""
        curRrule = ""
        curFlags = 0
        curReminderMinutes = ArrayList()
        curReminderActions = ArrayList()
        curRepeatExceptions = ArrayList()
        curRepeatInterval = 0
        curRepeatLimit = 0L
        curRepeatRule = 0
        curEventTypeId = REGULAR_EVENT_TYPE_ID
        curLastModified = 0L
        curCategoryColor = -2
        isNotificationDescription = false
        isProperReminderAction = false
        isSequence = false
        isParsingEvent = false
        curReminderTriggerMinutes = REMINDER_OFF
        curReminderTriggerAction = REMINDER_NOTIFICATION
    }
}
