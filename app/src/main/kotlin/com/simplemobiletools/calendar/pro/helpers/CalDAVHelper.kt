package com.simplemobiletools.calendar.pro.helpers

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import android.provider.CalendarContract.Reminders
import android.util.SparseIntArray
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.models.CalDAVCalendar
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.calendar.pro.objects.States.isUpdatingCalDAV
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.PERMISSION_READ_CALENDAR
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CALENDAR
import java.util.*
import kotlin.collections.ArrayList

class CalDAVHelper(val context: Context) {
    private val eventsHelper = context.eventsHelper

    fun refreshCalendars(showToasts: Boolean, callback: () -> Unit) {
        if (isUpdatingCalDAV) {
            return
        }

        isUpdatingCalDAV = true
        try {
            val calDAVCalendars = getCalDAVCalendars(context.config.caldavSyncedCalendarIDs, showToasts)
            for (calendar in calDAVCalendars) {
                val localEventType = eventsHelper.getEventTypeWithCalDAVCalendarId(calendar.id) ?: continue
                localEventType.apply {
                    title = calendar.displayName
                    caldavDisplayName = calendar.displayName
                    caldavEmail = calendar.accountName
                    eventsHelper.insertOrUpdateEventTypeSync(this)
                }

                fetchCalDAVCalendarEvents(calendar.id, localEventType.id!!, showToasts)
            }
            context.scheduleCalDAVSync(true)
            callback()
        } finally {
            isUpdatingCalDAV = false
        }
    }

    @SuppressLint("MissingPermission")
    fun getCalDAVCalendars(ids: String, showToasts: Boolean): List<CalDAVCalendar> {
        val calendars = ArrayList<CalDAVCalendar>()
        if (!context.hasPermission(PERMISSION_WRITE_CALENDAR) || !context.hasPermission(PERMISSION_READ_CALENDAR)) {
            return calendars
        }

        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
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
                    val accountType = cursor.getStringValue(CalendarContract.Calendars.ACCOUNT_TYPE)
                    val ownerName = cursor.getStringValue(CalendarContract.Calendars.OWNER_ACCOUNT) ?: ""
                    val color = cursor.getIntValue(CalendarContract.Calendars.CALENDAR_COLOR)
                    val accessLevel = cursor.getIntValue(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
                    val calendar = CalDAVCalendar(id, displayName, accountName, accountType, ownerName, color, accessLevel)
                    calendars.add(calendar)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            if (showToasts) {
                context.showErrorToast(e)
            }
        } finally {
            cursor?.close()
        }
        return calendars
    }

    fun updateCalDAVCalendar(eventType: EventType) {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val values = fillCalendarContentValues(eventType)
        val newUri = ContentUris.withAppendedId(uri, eventType.caldavCalendarId.toLong())
        try {
            context.contentResolver.update(newUri, values, null, null)
        } catch (e: IllegalArgumentException) {
        }
    }

    private fun fillCalendarContentValues(eventType: EventType): ContentValues {
        val colorKey = getEventTypeColorKey(eventType)
        return ContentValues().apply {
            put(CalendarContract.Calendars.CALENDAR_COLOR_KEY, colorKey)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, eventType.title)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getEventTypeColorKey(eventType: EventType): Int {
        val uri = CalendarContract.Colors.CONTENT_URI
        val projection = arrayOf(CalendarContract.Colors.COLOR_KEY)
        val selection = "${CalendarContract.Colors.COLOR_TYPE} = ? AND ${CalendarContract.Colors.COLOR} = ? AND ${CalendarContract.Colors.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(CalendarContract.Colors.TYPE_CALENDAR.toString(), eventType.color.toString(), eventType.caldavEmail)

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getStringValue(CalendarContract.Colors.COLOR_KEY).toInt()
            }
        } finally {
            cursor?.close()
        }

        return -1
    }

    @SuppressLint("MissingPermission")
    fun getAvailableCalDAVCalendarColors(eventType: EventType): ArrayList<Int> {
        val colors = SparseIntArray()
        val uri = CalendarContract.Colors.CONTENT_URI
        val projection = arrayOf(CalendarContract.Colors.COLOR, CalendarContract.Colors.COLOR_KEY)
        val selection = "${CalendarContract.Colors.COLOR_TYPE} = ? AND ${CalendarContract.Colors.ACCOUNT_NAME} = ?"
        val selectionArgs = arrayOf(CalendarContract.Colors.TYPE_CALENDAR.toString(), eventType.caldavEmail)

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val colorKey = cursor.getIntValue(CalendarContract.Colors.COLOR_KEY)
                    val color = cursor.getIntValue(CalendarContract.Colors.COLOR)
                    colors.put(colorKey, color)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        var sortedColors = ArrayList<Int>(colors.size())
        (0 until colors.size()).mapTo(sortedColors) { colors[it] }
        if (sortedColors.isNotEmpty()) {
            sortedColors = sortedColors.distinct() as ArrayList<Int>
        }

        return sortedColors
    }

    @SuppressLint("MissingPermission")
    private fun fetchCalDAVCalendarEvents(calendarId: Int, eventTypeId: Long, showToasts: Boolean) {
        val importIdsMap = HashMap<String, Event>()
        val fetchedEventIds = ArrayList<String>()
        val existingEvents = context.eventsDB.getEventsFromCalDAVCalendar("$CALDAV-$calendarId")
        existingEvents.forEach {
            importIdsMap[it.importId] = it
        }

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.DURATION,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.RRULE,
                CalendarContract.Events.ORIGINAL_ID,
                CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
                CalendarContract.Events.EVENT_LOCATION)

        val selection = "${CalendarContract.Events.CALENDAR_ID} = $calendarId"
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getLongValue(CalendarContract.Events._ID)
                    val title = cursor.getStringValue(CalendarContract.Events.TITLE) ?: ""
                    val description = cursor.getStringValue(CalendarContract.Events.DESCRIPTION) ?: ""
                    val startTS = cursor.getLongValue(CalendarContract.Events.DTSTART) / 1000L
                    var endTS = cursor.getLongValue(CalendarContract.Events.DTEND) / 1000L
                    val allDay = cursor.getIntValue(CalendarContract.Events.ALL_DAY)
                    val rrule = cursor.getStringValue(CalendarContract.Events.RRULE) ?: ""
                    val location = cursor.getStringValue(CalendarContract.Events.EVENT_LOCATION) ?: ""
                    val originalId = cursor.getStringValue(CalendarContract.Events.ORIGINAL_ID)
                    val originalInstanceTime = cursor.getLongValue(CalendarContract.Events.ORIGINAL_INSTANCE_TIME)
                    val reminders = getCalDAVEventReminders(id)

                    if (endTS == 0L) {
                        val duration = cursor.getStringValue(CalendarContract.Events.DURATION) ?: ""
                        endTS = startTS + Parser().parseDurationSeconds(duration)
                    }

                    val importId = getCalDAVEventImportId(calendarId, id)
                    val source = "$CALDAV-$calendarId"
                    val repeatRule = Parser().parseRepeatInterval(rrule, startTS)
                    val event = Event(null, startTS, endTS, title, location, description, reminders.getOrElse(0) { -1 },
                            reminders.getOrElse(1) { -1 }, reminders.getOrElse(2) { -1 }, repeatRule.repeatInterval, repeatRule.repeatRule,
                            repeatRule.repeatLimit, ArrayList(), importId, allDay, eventTypeId, source = source)

                    if (event.getIsAllDay()) {
                        event.startTS = Formatter.getShiftedImportTimestamp(event.startTS)
                        event.endTS = Formatter.getShiftedImportTimestamp(event.endTS)
                        if (event.endTS > event.startTS) {
                            event.endTS -= DAY
                        }
                    }

                    fetchedEventIds.add(importId)

                    // if the event is an exception from another events repeat rule, find the original parent event
                    if (originalInstanceTime != 0L) {
                        val parentImportId = "$source-$originalId"
                        val parentEvent = context.eventsDB.getEventWithImportId(parentImportId)
                        val originalDayCode = Formatter.getDayCodeFromTS(originalInstanceTime / 1000L)
                        if (parentEvent != null && !parentEvent.repetitionExceptions.contains(originalDayCode)) {
                            event.parentId = parentEvent.id!!
                            parentEvent.addRepetitionException(originalDayCode)
                            eventsHelper.insertEvent(parentEvent, false, false)

                            event.parentId = parentEvent.id!!
                            eventsHelper.insertEvent(event, false, false)
                            continue
                        }
                    }

                    if (importIdsMap.containsKey(event.importId)) {
                        val existingEvent = importIdsMap[importId]
                        val originalEventId = existingEvent!!.id

                        existingEvent.apply {
                            this.id = null
                            color = 0
                            lastUpdated = 0L
                            repetitionExceptions = ArrayList()
                        }

                        if (existingEvent.hashCode() != event.hashCode() && title.isNotEmpty()) {
                            event.id = originalEventId
                            eventsHelper.updateEvent(event, false, false)
                        }
                    } else {
                        if (title.isNotEmpty()) {
                            importIdsMap[event.importId] = event
                            eventsHelper.insertEvent(event, false, false)
                        }
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            if (showToasts) {
                context.showErrorToast(e)
            }
        } finally {
            cursor?.close()
        }

        val eventIdsToDelete = ArrayList<Long>()
        importIdsMap.keys.filter { !fetchedEventIds.contains(it) }.forEach {
            val caldavEventId = it
            existingEvents.forEach {
                if (it.importId == caldavEventId) {
                    eventIdsToDelete.add(it.id!!)
                }
            }
        }

        eventsHelper.deleteEvents(eventIdsToDelete.toMutableList(), false)
    }

    @SuppressLint("MissingPermission")
    fun insertCalDAVEvent(event: Event) {
        val uri = CalendarContract.Events.CONTENT_URI
        val values = fillEventContentValues(event)
        val newUri = context.contentResolver.insert(uri, values)

        val calendarId = event.getCalDAVCalendarId()
        val eventRemoteID = java.lang.Long.parseLong(newUri.lastPathSegment)
        event.importId = getCalDAVEventImportId(calendarId, eventRemoteID)

        setupCalDAVEventReminders(event)
        setupCalDAVEventImportId(event)
        refreshCalDAVCalendar(event)
    }

    fun updateCalDAVEvent(event: Event) {
        val uri = CalendarContract.Events.CONTENT_URI
        val values = fillEventContentValues(event)
        val eventRemoteID = event.getCalDAVEventId()
        event.importId = getCalDAVEventImportId(event.getCalDAVCalendarId(), eventRemoteID)

        val newUri = ContentUris.withAppendedId(uri, eventRemoteID)
        context.contentResolver.update(newUri, values, null, null)

        setupCalDAVEventReminders(event)
        setupCalDAVEventImportId(event)
        refreshCalDAVCalendar(event)
    }

    @SuppressLint("MissingPermission")
    private fun setupCalDAVEventReminders(event: Event) {
        clearEventReminders(event)
        event.getReminders().forEach {
            val contentValues = ContentValues().apply {
                put(Reminders.MINUTES, it)
                put(Reminders.EVENT_ID, event.getCalDAVEventId())
                put(Reminders.METHOD, Reminders.METHOD_ALERT)
            }

            try {
                context.contentResolver.insert(Reminders.CONTENT_URI, contentValues)
            } catch (e: Exception) {
                context.toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun setupCalDAVEventImportId(event: Event) {
        context.eventsDB.updateEventImportIdAndSource(event.importId, "$CALDAV-${event.getCalDAVCalendarId()}", event.id!!)
    }

    private fun fillEventContentValues(event: Event): ContentValues {
        return ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, event.getCalDAVCalendarId())
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.DTSTART, event.startTS * 1000L)
            put(CalendarContract.Events.ALL_DAY, if (event.getIsAllDay()) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().toString())
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)

            val repeatRule = Parser().getRepeatCode(event)
            if (repeatRule.isEmpty()) {
                putNull(CalendarContract.Events.RRULE)
            } else {
                put(CalendarContract.Events.RRULE, repeatRule)
            }

            if (event.getIsAllDay() && event.endTS > event.startTS)
                event.endTS += DAY

            if (event.repeatInterval > 0) {
                put(CalendarContract.Events.DURATION, getDurationCode(event))
                putNull(CalendarContract.Events.DTEND)
            } else {
                put(CalendarContract.Events.DTEND, event.endTS * 1000L)
                putNull(CalendarContract.Events.DURATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun clearEventReminders(event: Event) {
        val selection = "${Reminders.EVENT_ID} = ?"
        val selectionArgs = arrayOf(event.getCalDAVEventId().toString())
        context.contentResolver.delete(Reminders.CONTENT_URI, selection, selectionArgs)
    }

    private fun getDurationCode(event: Event): String {
        return if (event.getIsAllDay()) {
            val dur = Math.max(1, (event.endTS - event.startTS) / DAY)
            "P${dur}D"
        } else {
            Parser().getDurationCode((event.endTS - event.startTS) / 60L)
        }
    }

    fun deleteCalDAVCalendarEvents(calendarId: Long) {
        val eventIds = context.eventsDB.getCalDAVCalendarEvents("$CALDAV-$calendarId").toMutableList()
        eventsHelper.deleteEvents(eventIds, false)
    }

    fun deleteCalDAVEvent(event: Event) {
        val uri = CalendarContract.Events.CONTENT_URI
        val contentUri = ContentUris.withAppendedId(uri, event.getCalDAVEventId())
        try {
            context.contentResolver.delete(contentUri, null, null)
        } catch (ignored: Exception) {
        }
        refreshCalDAVCalendar(event)
    }

    @SuppressLint("MissingPermission")
    fun insertEventRepeatException(event: Event, occurrenceTS: Long): Long {
        val uri = CalendarContract.Events.CONTENT_URI
        val values = fillEventRepeatExceptionValues(event, occurrenceTS)
        val newUri = context.contentResolver.insert(uri, values)
        refreshCalDAVCalendar(event)
        return java.lang.Long.parseLong(newUri.lastPathSegment)
    }

    private fun fillEventRepeatExceptionValues(event: Event, occurrenceTS: Long): ContentValues {
        return ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, event.getCalDAVCalendarId())
            put(CalendarContract.Events.DTSTART, occurrenceTS)
            put(CalendarContract.Events.DTEND, occurrenceTS + (event.endTS - event.startTS))
            put(CalendarContract.Events.ORIGINAL_ID, event.getCalDAVEventId())
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().toString())
            put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, occurrenceTS * 1000L)
        }
    }

    @SuppressLint("MissingPermission")
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

    private fun getCalDAVEventImportId(calendarId: Int, eventId: Long) = "$CALDAV-$calendarId-$eventId"

    private fun refreshCalDAVCalendar(event: Event) = context.refreshCalDAVCalendars(event.getCalDAVCalendarId().toString(), false)
}
