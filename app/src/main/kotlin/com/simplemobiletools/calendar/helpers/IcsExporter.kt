package com.simplemobiletools.calendar.helpers

import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.isXMonthlyRepetition
import com.simplemobiletools.calendar.extensions.isXWeeklyRepetition
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

    var eventsExported = 0
    var eventsFailed = 0

    fun exportEvents(activity: SimpleActivity, file: File, events: ArrayList<Event>, callback: (result: ExportResult) -> Unit) {
        activity.getFileOutputStream(file) {
            it.bufferedWriter().use { out ->
                out.writeLn(BEGIN_CALENDAR)
                for (event in events) {
                    out.writeLn(BEGIN_EVENT)
                    event.title.replace("\n", "\\n").let { if (it.isNotEmpty()) out.writeLn("$SUMMARY:$it") }
                    event.description.replace("\n", "\\n").let { if (it.isNotEmpty()) out.writeLn("$DESCRIPTION$it") }
                    event.importId?.let { if (it.isNotEmpty()) out.writeLn("$UID$it") }
                    event.eventType.let { out.writeLn("$CATEGORIES${activity.dbHelper.getEventType(it)?.title}") }

                    if (event.isAllDay) {
                        out.writeLn("$DTSTART;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.startTS)}")
                        out.writeLn("$DTEND;$VALUE=$DATE:${Formatter.getDayCodeFromTS(event.endTS + DAY)}")
                    } else {
                        event.startTS.let { out.writeLn("$DTSTART:${Formatter.getExportedTime(it)}") }
                        event.endTS.let { out.writeLn("$DTEND:${Formatter.getExportedTime(it)}") }
                    }

                    out.writeLn("$STATUS$CONFIRMED")

                    fillRepeatInterval(event, out)
                    fillReminders(event, out)
                    fillIgnoredOccurrences(event, out)

                    eventsExported++
                    out.writeLn(END_EVENT)
                }
                out.writeLn(END_CALENDAR)
            }

            callback(if (eventsExported == 0) {
                EXPORT_FAIL
            } else if (eventsFailed > 0) {
                EXPORT_PARTIAL
            } else {
                EXPORT_OK
            })
        }
    }

    private fun fillRepeatInterval(event: Event, out: BufferedWriter) {
        val repeatInterval = event.repeatInterval
        if (repeatInterval == 0)
            return

        val freq = getFreq(repeatInterval)
        val interval = getInterval(repeatInterval)
        val repeatLimit = getRepeatLimitString(event)
        val byDay = getByDay(event)
        val rrule = "$RRULE$FREQ=$freq;$INTERVAL=$interval$repeatLimit$byDay"
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
        else if (event.repeatLimit < 0)
            ";$COUNT=${-event.repeatLimit}"
        else
            ";$UNTIL=${Formatter.getDayCodeFromTS(event.repeatLimit)}"
    }

    private fun getByDay(event: Event): String {
        return if (event.repeatInterval.isXWeeklyRepetition()) {
            val days = getByDayString(event.repeatRule)
            ";$BYDAY=$days"
        } else if (event.repeatInterval.isXMonthlyRepetition()) {
            if (event.repeatRule == REPEAT_MONTH_LAST_DAY) {
                ";$BYMONTHDAY=-1"
            } else if (event.repeatRule == REPEAT_MONTH_EVERY_XTH_DAY) {
                val start = Formatter.getDateTimeFromTS(event.startTS)
                val dayOfMonth = start.dayOfMonth
                val order = (dayOfMonth - 1) / 7 + 1
                val day = getDayLetters(start.dayOfWeek)
                ";$BYDAY=$order$day"
            } else {
                ""
            }
        } else {
            ""
        }
    }

    private fun getByDayString(rule: Int): String {
        var result = ""
        if (rule and MONDAY != 0)
            result += "$MO,"
        if (rule and TUESDAY != 0)
            result += "$TU,"
        if (rule and WEDNESDAY != 0)
            result += "$WE,"
        if (rule and THURSDAY != 0)
            result += "$TH,"
        if (rule and FRIDAY != 0)
            result += "$FR,"
        if (rule and SATURDAY != 0)
            result += "$SA,"
        if (rule and SUNDAY != 0)
            result += "$SU,"
        return result.trimEnd(',')
    }

    private fun getDayLetters(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> MO
            2 -> TU
            3 -> WE
            4 -> TH
            5 -> FR
            6 -> SA
            else -> SU
        }
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
            out.writeLn("$EXDATE:$it}")
        }
    }
}
