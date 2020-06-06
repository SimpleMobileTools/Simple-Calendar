package com.simplemobiletools.calendar.pro.helpers

import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.calDAVHelper
import com.simplemobiletools.calendar.pro.extensions.eventTypesDB
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult.*
import com.simplemobiletools.calendar.pro.models.CalDAVCalendar
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.writeLn
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import java.io.BufferedWriter
import java.io.OutputStream

class IcsExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private var eventsExported = 0
    private var eventsFailed = 0
    private var calendars = ArrayList<CalDAVCalendar>()

    fun exportEvents(activity: BaseSimpleActivity, outputStream: OutputStream?, events: ArrayList<Event>, showExportingToast: Boolean, callback: (result: ExportResult) -> Unit) {
        if (outputStream == null) {
            callback(EXPORT_FAIL)
            return
        }

        ensureBackgroundThread {
            calendars = activity.calDAVHelper.getCalDAVCalendars("", false)
            if (showExportingToast) {
                activity.toast(R.string.exporting)
            }

            outputStream.bufferedWriter().use { out ->
                out.writeLn(BEGIN_CALENDAR)
                out.writeLn(CALENDAR_PRODID)
                out.writeLn(CALENDAR_VERSION)
                for (event in events) {
                    out.writeLn(BEGIN_EVENT)
                    event.title.replace("\n", "\\n").let { if (it.isNotEmpty()) out.writeLn("$SUMMARY:$it") }
                    event.description.replace("\n", "\\n").let { if (it.isNotEmpty()) out.writeLn("$DESCRIPTION$it") }
                    event.importId.let { if (it.isNotEmpty()) out.writeLn("$UID$it") }
                    event.eventType.let { out.writeLn("$CATEGORY_COLOR${activity.eventTypesDB.getEventTypeWithId(it)?.color}") }
                    event.eventType.let { out.writeLn("$CATEGORIES${activity.eventTypesDB.getEventTypeWithId(it)?.title}") }
                    event.lastUpdated.let { out.writeLn("$LAST_MODIFIED:${Formatter.getExportedTime(it)}") }
                    event.location.let { if (it.isNotEmpty()) out.writeLn("$LOCATION:$it") }

                    if (event.getIsAllDay()) {
                        out.writeLn("$DTSTART;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.startTS)}")
                        out.writeLn("$DTEND;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.endTS + DAY)}")
                    } else {
                        event.startTS.let { out.writeLn("$DTSTART:${Formatter.getExportedTime(it * 1000L)}") }
                        event.endTS.let { out.writeLn("$DTEND:${Formatter.getExportedTime(it * 1000L)}") }
                    }

                    out.writeLn("$STATUS$CONFIRMED")
                    Parser().getRepeatCode(event).let { if (it.isNotEmpty()) out.writeLn("$RRULE$it") }

                    fillReminders(event, out)
                    fillIgnoredOccurrences(event, out)

                    eventsExported++
                    out.writeLn(END_EVENT)
                }
                out.writeLn(END_CALENDAR)
            }

            callback(when {
                eventsExported == 0 -> EXPORT_FAIL
                eventsFailed > 0 -> EXPORT_PARTIAL
                else -> EXPORT_OK
            })
        }
    }

    private fun fillReminders(event: Event, out: BufferedWriter) {
        event.getReminders().forEach {
            val reminder = it
            out.apply {
                writeLn(BEGIN_ALARM)
                if (reminder.type == REMINDER_NOTIFICATION) {
                    writeLn("$ACTION$DISPLAY")
                } else {
                    writeLn("$ACTION$EMAIL")
                    val attendee = calendars.firstOrNull { it.id == event.getCalDAVCalendarId() }?.accountName
                    if (attendee != null) {
                        writeLn("$ATTENDEE$MAILTO$attendee")
                    }
                }
                writeLn("$TRIGGER-${Parser().getDurationCode(reminder.minutes.toLong())}")
                writeLn(END_ALARM)
            }
        }
    }

    private fun fillIgnoredOccurrences(event: Event, out: BufferedWriter) {
        event.repetitionExceptions.forEach {
            out.writeLn("$EXDATE:$it")
        }
    }
}
