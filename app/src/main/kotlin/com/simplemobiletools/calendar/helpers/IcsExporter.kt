package com.simplemobiletools.calendar.helpers

import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.writeLn
import com.simplemobiletools.calendar.helpers.IcsExporter.ExportResult.*
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.getFileOutputStream
import java.io.BufferedWriter
import java.io.File

class IcsExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private var eventsExported = 0
    private var eventsFailed = 0

    fun exportEvents(activity: SimpleActivity, file: File, events: ArrayList<Event>, callback: (result: ExportResult) -> Unit) {
        activity.getFileOutputStream(file) {
            if (it == null) {
                callback(EXPORT_FAIL)
                return@getFileOutputStream
            }

            it.bufferedWriter().use { out ->
                out.writeLn(BEGIN_CALENDAR)
                for (event in events) {
                    out.writeLn(BEGIN_EVENT)
                    event.title.replace("\n", "\\n").let { if (it.isNotEmpty()) out.writeLn("$SUMMARY:$it") }
                    event.description.replace("\n", "\\n").let { if (it.isNotEmpty()) out.writeLn("$DESCRIPTION$it") }
                    event.importId.let { if (it.isNotEmpty()) out.writeLn("$UID$it") }
                    event.eventType.let { out.writeLn("$CATEGORIES${activity.dbHelper.getEventType(it)?.title}") }
                    event.lastUpdated.let { out.writeLn("$LAST_MODIFIED:${Formatter.getExportedTime(it)}") }
                    event.location.let { if (it.isNotEmpty()) out.writeLn("$LOCATION$it") }

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
        checkReminder(event.reminder1Minutes, out)
        checkReminder(event.reminder2Minutes, out)
        checkReminder(event.reminder3Minutes, out)
    }

    private fun checkReminder(minutes: Int, out: BufferedWriter) {
        if (minutes != -1) {
            out.apply {
                writeLn(BEGIN_ALARM)
                writeLn("$ACTION$DISPLAY")
                writeLn("$TRIGGER-${Parser().getDurationCode(minutes)}")
                writeLn(END_ALARM)
            }
        }
    }

    private fun fillIgnoredOccurrences(event: Event, out: BufferedWriter) {
        event.ignoreEventOccurrences.forEach {
            out.writeLn("$EXDATE:$it}")
        }
    }
}
