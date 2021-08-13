package com.simplemobiletools.calendar.pro.helpers

import android.provider.CalendarContract.Events
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

    private val MAX_LINE_LENGTH = 75
    private var eventsExported = 0
    private var eventsFailed = 0
    private var calendars = ArrayList<CalDAVCalendar>()

    fun exportEvents(
        activity: BaseSimpleActivity,
        outputStream: OutputStream?,
        events: ArrayList<Event>,
        showExportingToast: Boolean,
        callback: (result: ExportResult) -> Unit
    ) {
        if (outputStream == null) {
            callback(EXPORT_FAIL)
            return
        }

        ensureBackgroundThread {
            val reminderLabel = activity.getString(R.string.reminder)
            val exportTime = Formatter.getExportedTime(System.currentTimeMillis())

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
                    event.importId.let { if (it.isNotEmpty()) out.writeLn("$UID$it") }
                    event.eventType.let { out.writeLn("$CATEGORY_COLOR${activity.eventTypesDB.getEventTypeWithId(it)?.color}") }
                    event.eventType.let { out.writeLn("$CATEGORIES${activity.eventTypesDB.getEventTypeWithId(it)?.title}") }
                    event.lastUpdated.let { out.writeLn("$LAST_MODIFIED:${Formatter.getExportedTime(it)}") }
                    event.location.let { out.writeLn("$LOCATION:$it") }
                    event.availability.let { out.writeLn("$TRANSP${if (it == Events.AVAILABILITY_FREE) TRANSPARENT else OPAQUE}") }

                    if (event.getIsAllDay()) {
                        out.writeLn("$DTSTART;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.startTS)}")
                        out.writeLn("$DTEND;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.endTS + DAY)}")
                    } else {
                        event.startTS.let { out.writeLn("$DTSTART:${Formatter.getExportedTime(it * 1000L)}") }
                        event.endTS.let { out.writeLn("$DTEND:${Formatter.getExportedTime(it * 1000L)}") }
                    }
                    event.hasMissingYear().let { out.writeLn("$MISSING_YEAR${if (it) 1 else 0}") }

                    out.writeLn("$DTSTAMP$exportTime")
                    out.writeLn("$STATUS$CONFIRMED")
                    Parser().getRepeatCode(event).let { if (it.isNotEmpty()) out.writeLn("$RRULE$it") }

                    fillDescription(event.description.replace("\n", "\\n"), out)
                    fillReminders(event, out, reminderLabel)
                    fillIgnoredOccurrences(event, out)

                    eventsExported++
                    out.writeLn(END_EVENT)
                }
                out.writeLn(END_CALENDAR)
            }

            callback(
                when {
                    eventsExported == 0 -> EXPORT_FAIL
                    eventsFailed > 0 -> EXPORT_PARTIAL
                    else -> EXPORT_OK
                }
            )
        }
    }

    private fun fillReminders(event: Event, out: BufferedWriter, reminderLabel: String) {
        event.getReminders().forEach {
            val reminder = it
            out.apply {
                writeLn(BEGIN_ALARM)
                writeLn("$DESCRIPTION$reminderLabel")
                if (reminder.type == REMINDER_NOTIFICATION) {
                    writeLn("$ACTION$DISPLAY")
                } else {
                    writeLn("$ACTION$EMAIL")
                    val attendee = calendars.firstOrNull { it.id == event.getCalDAVCalendarId() }?.accountName
                    if (attendee != null) {
                        writeLn("$ATTENDEE$MAILTO$attendee")
                    }
                }

                val sign = if (reminder.minutes < -1) "" else "-"
                writeLn("$TRIGGER:$sign${Parser().getDurationCode(Math.abs(reminder.minutes.toLong()))}")
                writeLn(END_ALARM)
            }
        }
    }

    private fun fillIgnoredOccurrences(event: Event, out: BufferedWriter) {
        event.repetitionExceptions.forEach {
            out.writeLn("$EXDATE:$it")
        }
    }

    private fun fillDescription(description: String, out: BufferedWriter) {
        var index = 0
        var isFirstLine = true

        while (index < description.length) {
            val substring = description.substring(index, Math.min(index + MAX_LINE_LENGTH, description.length))
            if (isFirstLine) {
                out.writeLn("$DESCRIPTION$substring")
            } else {
                out.writeLn("\t$substring")
            }

            isFirstLine = false
            index += MAX_LINE_LENGTH
        }

        if (isFirstLine) {
            out.writeLn("$DESCRIPTION$description")
        }
    }
}
