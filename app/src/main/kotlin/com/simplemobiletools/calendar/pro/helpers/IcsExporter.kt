package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import android.provider.CalendarContract.Events
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.calDAVHelper
import com.simplemobiletools.calendar.pro.extensions.eventTypesDB
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult.EXPORT_FAIL
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult.EXPORT_OK
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult.EXPORT_PARTIAL
import com.simplemobiletools.calendar.pro.models.CalDAVCalendar
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.writeLn
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

class IcsExporter(private val context: Context) {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private val MAX_LINE_LENGTH = 75
    private var eventsExported = 0
    private var eventsFailed = 0
    private var calendars = ArrayList<CalDAVCalendar>()
    private val reminderLabel = context.getString(R.string.reminder)
    private val exportTime = Formatter.getExportedTime(System.currentTimeMillis())

    fun exportEvents(
        outputStream: OutputStream?,
        events: List<Event>,
        showExportingToast: Boolean,
        callback: (result: ExportResult) -> Unit
    ) {
        if (outputStream == null) {
            callback(EXPORT_FAIL)
            return
        }

        ensureBackgroundThread {
            calendars = context.calDAVHelper.getCalDAVCalendars("", false)
            if (showExportingToast) {
                context.toast(com.simplemobiletools.commons.R.string.exporting)
            }

            object : BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)) {
                val lineSeparator = "\r\n"

                /**
                 * Writes a line separator. The line separator string is defined by RFC 5545 in 3.1. Content Lines:
                 * Content Lines are delimited by a line break, which is a CRLF sequence (CR character followed by LF character).
                 *
                 * @see <a href="https://icalendar.org/iCalendar-RFC-5545/3-1-content-lines.html">RFC 5545 - 3.1. Content Lines</a>
                 */
                override fun newLine() {
                    write(lineSeparator)
                }
            }.use { out ->
                out.writeLn(BEGIN_CALENDAR)
                out.writeLn(CALENDAR_PRODID)
                out.writeLn(CALENDAR_VERSION)
                for (event in events) {
                    if (event.isTask()) {
                        writeTask(out, event)
                    } else {
                        writeEvent(out, event)
                    }
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
                writeLn("$DESCRIPTION_EXPORT$reminderLabel")
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
                out.writeLn("$DESCRIPTION_EXPORT$substring")
            } else {
                out.writeLn("\t$substring")
            }

            isFirstLine = false
            index += MAX_LINE_LENGTH
        }
    }

    private fun writeEvent(writer: BufferedWriter, event: Event) {
        with(writer) {
            writeLn(BEGIN_EVENT)
            event.title.replace("\n", "\\n").let { if (it.isNotEmpty()) writeLn("$SUMMARY:$it") }
            event.importId.let { if (it.isNotEmpty()) writeLn("$UID$it") }
            writeLn("$CATEGORY_COLOR${context.eventTypesDB.getEventTypeWithId(event.eventType)?.color}")
            writeLn("$CATEGORIES${context.eventTypesDB.getEventTypeWithId(event.eventType)?.title}")
            writeLn("$LAST_MODIFIED:${Formatter.getExportedTime(event.lastUpdated)}")
            writeLn("$TRANSP${if (event.availability == Events.AVAILABILITY_FREE) TRANSPARENT else OPAQUE}")
            event.location.let { if (it.isNotEmpty()) writeLn("$LOCATION:$it") }

            if (event.getIsAllDay()) {
                writeLn("$DTSTART;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.startTS)}")
                writeLn("$DTEND;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.endTS + TWELVE_HOURS)}")
            } else {
                writeLn("$DTSTART:${Formatter.getExportedTime(event.startTS * 1000L)}")
                writeLn("$DTEND:${Formatter.getExportedTime(event.endTS * 1000L)}")
            }
            writeLn("$MISSING_YEAR${if (event.hasMissingYear()) 1 else 0}")

            writeLn("$DTSTAMP$exportTime")
            writeLn("$STATUS$CONFIRMED")
            Parser().getRepeatCode(event).let { if (it.isNotEmpty()) writeLn("$RRULE$it") }

            fillDescription(event.description.replace("\n", "\\n"), writer)
            fillReminders(event, writer, reminderLabel)
            fillIgnoredOccurrences(event, writer)

            eventsExported++
            writeLn(END_EVENT)
        }
    }

    private fun writeTask(writer: BufferedWriter, task: Event) {
        with(writer) {
            writeLn(BEGIN_TASK)
            task.title.replace("\n", "\\n").let { if (it.isNotEmpty()) writeLn("$SUMMARY:$it") }
            task.importId.let { if (it.isNotEmpty()) writeLn("$UID$it") }
            writeLn("$CATEGORY_COLOR${context.eventTypesDB.getEventTypeWithId(task.eventType)?.color}")
            writeLn("$CATEGORIES${context.eventTypesDB.getEventTypeWithId(task.eventType)?.title}")
            writeLn("$LAST_MODIFIED:${Formatter.getExportedTime(task.lastUpdated)}")
            task.location.let { if (it.isNotEmpty()) writeLn("$LOCATION:$it") }

            if (task.getIsAllDay()) {
                writeLn("$DTSTART;$VALUE=$DATE:${Formatter.getDayCodeFromTS(task.startTS)}")
            } else {
                writeLn("$DTSTART:${Formatter.getExportedTime(task.startTS * 1000L)}")
            }

            writeLn("$DTSTAMP$exportTime")
            if (task.isTaskCompleted()) {
                writeLn("$STATUS$COMPLETED")
            }
            Parser().getRepeatCode(task).let { if (it.isNotEmpty()) writeLn("$RRULE$it") }

            fillDescription(task.description.replace("\n", "\\n"), writer)
            fillReminders(task, writer, reminderLabel)
            fillIgnoredOccurrences(task, writer)

            eventsExported++
            writeLn(END_TASK)
        }
    }
}
