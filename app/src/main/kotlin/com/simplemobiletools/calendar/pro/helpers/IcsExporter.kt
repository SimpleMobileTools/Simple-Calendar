package com.simplemobiletools.calendar.pro.helpers

import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.eventTypesDB
import com.simplemobiletools.calendar.pro.helpers.IcsExporter.ExportResult.*
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.getFileOutputStream
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.writeLn
import com.simplemobiletools.commons.models.FileDirItem
import java.io.BufferedWriter
import java.io.File

class IcsExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    private var eventsExported = 0
    private var eventsFailed = 0

    fun exportEvents(activity: BaseSimpleActivity, file: File, events: ArrayList<Event>, showExportingToast: Boolean, callback: (result: ExportResult) -> Unit) {
        val fileDirItem = FileDirItem(file.absolutePath, file.name)
        activity.getFileOutputStream(fileDirItem, true) {
            if (it == null) {
                callback(EXPORT_FAIL)
                return@getFileOutputStream
            }

            Thread {
                if (showExportingToast) {
                    activity.toast(R.string.exporting)
                }

                it.bufferedWriter().use { out ->
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
            }.start()
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
                writeLn("$TRIGGER-${Parser().getDurationCode(minutes.toLong())}")
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
