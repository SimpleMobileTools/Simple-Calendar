package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.extensions.writeLn
import com.simplemobiletools.calendar.helpers.IcsExporter.ExportResult.*
import com.simplemobiletools.calendar.models.Event
import java.io.File

class IcsExporter {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    var eventsExported = 0
    var eventsFailed = 0

    fun exportEvents(context: Context, path: String, events: ArrayList<Event>): ExportResult {
        val filename = "events_${System.currentTimeMillis() / 1000}.ics"
        File(path, filename).bufferedWriter().use { out ->
            out.writeLn(BEGIN_CALENDAR)
            for (event in events) {
                out.writeLn(BEGIN_EVENT)

                event.title.let { if (it.isNotEmpty()) out.writeLn("$SUMMARY$it") }
                event.description.let { if (it.isNotEmpty()) out.writeLn("$DESCRIPTION$it") }
                event.importId?.let { if (it.isNotEmpty()) out.writeLn("$UID$it") }
                event.startTS.let { out.writeLn("$DTSTART:${Formatter.getExportedTime(it)}") }
                event.endTS.let { out.writeLn("$DTEND:${Formatter.getExportedTime(it)}") }

                out.writeLn(END_EVENT)
            }
            out.writeLn(END_CALENDAR)
        }

        return if (eventsExported == 0) {
            EXPORT_FAIL
        } else if (eventsFailed > 0) {
            EXPORT_PARTIAL
        } else {
            EXPORT_OK
        }
    }
}
