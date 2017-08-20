package com.simplemobiletools.calendar.helpers

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import android.provider.CalendarContract.Reminders
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.hasCalendarPermission
import com.simplemobiletools.calendar.extensions.scheduleCalDAVSync
import com.simplemobiletools.calendar.models.CalDAVCalendar
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getLongValue
import com.simplemobiletools.commons.extensions.getStringValue
import java.util.*

class CalDAVEventsHandler(val context: Context) {
    fun refreshCalendars(callback: () -> Unit) {
        getCalDAVCalendars(context.config.caldavSyncedCalendarIDs).forEach {
            val eventTypeId = context.dbHelper.getEventTypeIdWithTitle(it.displayName)
            CalDAVEventsHandler(context).fetchCalDAVCalendarEvents(it.id, eventTypeId)
        }
        context.scheduleCalDAVSync(true)
        callback()
    }

    fun getCalDAVCalendars(ids: String = ""): List<CalDAVCalendar> {
        val calendars = ArrayList<CalDAVCalendar>()
        if (!context.hasCalendarPermission()) {
            return calendars
        }

        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.OWNER_ACCOUNT,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

        val selection = if (ids.trim().isNotEmpty()) "${CalendarContract.Calendars._ID} IN ($ids)" else null
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getIntValue(CalendarContract.Calendars._ID)
                    val displayName = cursor.getStringValue(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                    val accountName = cursor.getStringValue(CalendarContract.Calendars.ACCOUNT_NAME)
                    val ownerName = cursor.getStringValue(CalendarContract.Calendars.OWNER_ACCOUNT)
                    val color = cursor.getIntValue(CalendarContract.Calendars.CALENDAR_COLOR)
                    val accessLevel = cursor.getIntValue(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
                    val calendar = CalDAVCalendar(id, displayName, accountName, ownerName, color, accessLevel)
                    calendars.add(calendar)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return calendars
    }

    private fun fetchCalDAVCalendarEvents(calendarId: Int, eventTypeId: Int) {
        val importIdsMap = HashMap<String, Event>()
        val fetchedEventIds = ArrayList<String>()
        val existingEvents = context.dbHelper.getEventsFromCalDAVCalendar(calendarId)
        existingEvents.forEach {
            importIdsMap.put(it.importId, it)
        }

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.DURATION,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.RRULE)

        val selection = "${CalendarContract.Events.CALENDAR_ID} = $calendarId"
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getLongValue(CalendarContract.Events._ID)
                    val title = cursor.getStringValue(CalendarContract.Events.TITLE) ?: continue
                    val description = cursor.getStringValue(CalendarContract.Events.DESCRIPTION)
                    val startTS = (cursor.getLongValue(CalendarContract.Events.DTSTART) / 1000).toInt()
                    var endTS = (cursor.getLongValue(CalendarContract.Events.DTEND) / 1000).toInt()
                    val allDay = cursor.getIntValue(CalendarContract.Events.ALL_DAY)
                    val rrule = cursor.getStringValue(CalendarContract.Events.RRULE) ?: ""
                    val reminders = getCalDAVEventReminders(id)

                    if (endTS == 0) {
                        val duration = cursor.getStringValue(CalendarContract.Events.DURATION)
                        endTS = startTS + Parser().parseDurationSeconds(duration)
                    }

                    val importId = getCalDAVEventImportId(calendarId, id)
                    val repeatRule = Parser().parseRepeatInterval(rrule, startTS)
                    val event = Event(0, startTS, endTS, title, description, reminders.getOrElse(0, { -1 }),
                            reminders.getOrElse(1, { -1 }), reminders.getOrElse(2, { -1 }), repeatRule.repeatInterval,
                            importId, allDay, repeatRule.repeatLimit, repeatRule.repeatRule, eventTypeId, source = "$CALDAV-$calendarId")

                    if (event.getIsAllDay() && endTS > startTS) {
                        event.endTS -= DAY
                    }

                    fetchedEventIds.add(importId)
                    if (importIdsMap.containsKey(event.importId)) {
                        val existingEvent = importIdsMap[importId]
                        val originalEventId = existingEvent!!.id
                        existingEvent.id = 0
                        if (existingEvent.hashCode() != event.hashCode()) {
                            event.id = originalEventId
                            context.dbHelper.update(event) {
                            }
                        }
                    } else {
                        context.dbHelper.insert(event, false) {
                            importIdsMap.put(event.importId, event)
                        }
                    }
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        val eventIdsToDelete = ArrayList<String>()
        importIdsMap.keys.filter { !fetchedEventIds.contains(it) }.forEach {
            val caldavEventId = it
            existingEvents.forEach {
                if (it.importId == caldavEventId) {
                    eventIdsToDelete.add(it.id.toString())
                }
            }
        }

        eventIdsToDelete.forEach {
            context.dbHelper.deleteEvents(eventIdsToDelete.toTypedArray(), false)
        }
    }

    fun addCalDAVEvent(event: Event) {
        val calendarId = event.getCalDAVCalendarId()
        val uri = CalendarContract.Events.CONTENT_URI
        val calendarValues = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startTS * 1000L)
            put(CalendarContract.Events.ALL_DAY, if (event.getIsAllDay()) 1 else 0)
            put(CalendarContract.Events.RRULE, Parser().getRepeatCode(event))
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().toString())

            if (event.getIsAllDay() && event.endTS > event.startTS)
                event.endTS += DAY

            if (event.repeatInterval > 0) {
                put(CalendarContract.Events.DURATION, getDurationCode(event))
            } else {
                put(CalendarContract.Events.DTEND, event.endTS * 1000L)
            }
        }

        val newUri = context.contentResolver.insert(uri, calendarValues)
        val eventRemoteID = java.lang.Long.parseLong(newUri.lastPathSegment)

        event.getReminders().forEach {
            ContentValues().apply {
                put(Reminders.MINUTES, it)
                put(Reminders.EVENT_ID, eventRemoteID)
                put(Reminders.METHOD, Reminders.METHOD_ALERT)
                context.contentResolver.insert(Reminders.CONTENT_URI, this)
            }
        }

        val importId = getCalDAVEventImportId(calendarId, eventRemoteID)
        context.dbHelper.updateEventImportIdAndSource(event.id, importId, "$CALDAV-$calendarId")
    }

    private fun getDurationCode(event: Event): String {
        return if (event.getIsAllDay()) {
            val dur = Math.max(1, (event.endTS - event.startTS) / DAY)
            "P${dur}D"
        } else {
            Parser().getDurationCode((event.endTS - event.startTS) / 60)
        }
    }

    fun deleteCalDAVCalendarEvents(calendarId: Long) {
        val events = context.dbHelper.getCalDAVCalendarEvents(calendarId)
        val eventIds = events.map { it.id.toString() }.toTypedArray()
        context.dbHelper.deleteEvents(eventIds, false)
    }

    fun deleteCalDAVEvent(event: Event) {
        val uri = CalendarContract.Events.CONTENT_URI
        val contentUri = ContentUris.withAppendedId(uri, event.getCalDAVEventId())
        context.contentResolver.delete(contentUri, null, null)
    }

    private fun getCalDAVEventReminders(eventId: Long): List<Int> {
        val reminders = ArrayList<Int>()
        val uri = CalendarContract.Reminders.CONTENT_URI
        val projection = arrayOf(
                CalendarContract.Reminders.MINUTES,
                CalendarContract.Reminders.METHOD)
        val selection = "${CalendarContract.Reminders.EVENT_ID} = $eventId"
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val minutes = cursor.getIntValue(CalendarContract.Reminders.MINUTES)
                    val method = cursor.getIntValue(CalendarContract.Reminders.METHOD)
                    if (method == CalendarContract.Reminders.METHOD_ALERT) {
                        reminders.add(minutes)
                    }
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return reminders
    }

    fun getCalDAVEventImportId(calendarId: Int, eventId: Long) = "$CALDAV-$calendarId-$eventId"
}
