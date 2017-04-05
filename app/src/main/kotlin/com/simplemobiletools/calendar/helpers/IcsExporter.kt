package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.writeLn
import com.simplemobiletools.calendar.helpers.IcsExporter.ExportResult.*
import com.simplemobiletools.calendar.models.Event
import java.io.BufferedWriter
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
                event.eventType.let { out.writeLn("$CATEGORIES${context.dbHelper.getEventType(it)?.title}") }

                if (event.isAllDay) {
                    out.writeLn("$DTSTART;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.startTS)}")
                } else {
                    event.startTS.let { out.writeLn("$DTSTART:${Formatter.getExportedTime(it)}") }
                    event.endTS.let { out.writeLn("$DTEND:${Formatter.getExportedTime(it)}") }
                }

                fillRepeatInterval(event, out)
                fillReminders(event, out)
                fillIgnoredOccurrences(event, out)

                out.writeLn("$STATUS$CONFIRMED")
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

    private fun fillRepeatInterval(event: Event, out: BufferedWriter) {
        val repeatInterval = event.repeatInterval
        if (repeatInterval == 0)
            return

        val freq = getFreq(repeatInterval)
        val interval = getInterval(repeatInterval)
        val repeatLimit = getRepeatLimitString(event)
        val rrule = "$RRULE$FREQ=$freq;$INTERVAL=$interval$repeatLimit"
        out.writeLn(rrule)
    }

    private fun getFreq(interval: Int): String {
        return if (interval % YEAR == 0)
            YEARLY
        else if (interval % MONTH == 0)
            MONTHLY
        else if (interval % WEEK == 0)
            WEEKLY
        else
            DAILY
    }

    private fun getInterval(interval: Int): Int {
        return if (interval % YEAR == 0)
            interval / YEAR
        else if (interval % MONTH == 0)
            interval / MONTH
        else if (interval % WEEK == 0)
            interval / WEEK
        else
            interval / DAY
    }

    private fun getRepeatLimitString(event: Event): String {
        return if (event.repeatLimit == 0)
            ""
        else
            ";$UNTIL=${Formatter.getDayCodeFromTS(event.repeatLimit)}"
    }

    private fun fillReminders(event: Event, out: BufferedWriter) {
        checkReminder(event.reminder1Minutes, out)
        checkReminder(event.reminder2Minutes, out)
        checkReminder(event.reminder3Minutes, out)
    }

    private fun checkReminder(minutes: Int, out: BufferedWriter) {
        if (minutes != -1) {
            out.writeLn(BEGIN_ALARM)
            out.writeLn("$ACTION$DISPLAY")
            out.writeLn("$TRIGGER${getReminderString(minutes)}")
            out.writeLn(END_ALARM)
        }
    }

    private fun getReminderString(minutes: Int): String {
        var days = 0
        var hours = 0
        var remainder = minutes
        if (remainder >= DAY_MINUTES) {
            days = Math.floor(((remainder / DAY_MINUTES).toDouble())).toInt()
            remainder -= days * DAY_MINUTES
        }
        if (remainder >= 60) {
            hours = Math.floor(((remainder / 60).toDouble())).toInt()
            remainder -= hours * 60
        }
        return "-P${days}DT${hours}H${remainder}M0S"
    }

    private fun fillIgnoredOccurrences(event: Event, out: BufferedWriter) {
        event.ignoreEventOccurrences.forEach {
            out.writeLn("$EXDATE${Formatter.getDayCodeFromTS(it)}")
        }
    }
}
