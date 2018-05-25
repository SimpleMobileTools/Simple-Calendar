package com.simplemobiletools.calendar.models

import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

data class Event(var id: Int = 0, var startTS: Int = 0, var endTS: Int = 0, var title: String = "", var description: String = "",
                 var reminder1Minutes: Int = -1, var reminder2Minutes: Int = -1, var reminder3Minutes: Int = -1, var repeatInterval: Int = 0,
                 var importId: String = "", var flags: Int = 0, var repeatLimit: Int = 0, var repeatRule: Int = 0,
                 var eventType: Int = DBHelper.REGULAR_EVENT_TYPE_ID, var ignoreEventOccurrences: ArrayList<Int> = ArrayList(),
                 var offset: String = "", var isDstIncluded: Boolean = false, var parentId: Int = 0, var lastUpdated: Long = 0L,
                 var source: String = SOURCE_SIMPLE_CALENDAR, var color: Int = 0, var location: String = "", var isPastEvent: Boolean = false)
    : Serializable {

    companion object {
        private const val serialVersionUID = -32456795132345616L
    }

    fun addIntervalTime(original: Event) {
        val currStart = Formatter.getDateTimeFromTS(startTS)
        val newStart: DateTime
        newStart = when (repeatInterval) {
            DAY -> currStart.plusDays(1)
            else -> {
                when {
                    repeatInterval % YEAR == 0 -> when (repeatRule) {
                        REPEAT_ORDER_WEEKDAY -> addXthDayInterval(currStart, original, false)
                        REPEAT_ORDER_WEEKDAY_USE_LAST -> addXthDayInterval(currStart, original, true)
                        else -> currStart.plusYears(repeatInterval / YEAR)
                    }
                    repeatInterval % MONTH == 0 -> when (repeatRule) {
                        REPEAT_SAME_DAY -> addMonthsWithSameDay(currStart, original)
                        REPEAT_ORDER_WEEKDAY -> addXthDayInterval(currStart, original, false)
                        REPEAT_ORDER_WEEKDAY_USE_LAST -> addXthDayInterval(currStart, original, true)
                        else -> currStart.plusMonths(repeatInterval / MONTH).dayOfMonth().withMaximumValue()
                    }
                    repeatInterval % WEEK == 0 -> {
                        // step through weekly repetition by days too, as events can trigger multiple times a week
                        currStart.plusDays(1)
                    }
                    else -> currStart.plusSeconds(repeatInterval)
                }
            }
        }
        val newStartTS = newStart.seconds()
        val newEndTS = newStartTS + (endTS - startTS)
        startTS = newStartTS
        endTS = newEndTS
    }

    // if an event should happen on 31st with Same Day monthly repetition, dont show it at all at months with 30 or less days
    private fun addMonthsWithSameDay(currStart: DateTime, original: Event): DateTime {
        var newDateTime = currStart.plusMonths(repeatInterval / MONTH)
        if (newDateTime.dayOfMonth == currStart.dayOfMonth) {
            return newDateTime
        }

        while (newDateTime.dayOfMonth().maximumValue < Formatter.getDateTimeFromTS(original.startTS).dayOfMonth().maximumValue) {
            newDateTime = newDateTime.plusMonths(repeatInterval / MONTH)
            newDateTime = newDateTime.withDayOfMonth(newDateTime.dayOfMonth().maximumValue)
        }
        return newDateTime
    }

    // handle monthly repetitions like Third Monday
    private fun addXthDayInterval(currStart: DateTime, original: Event, forceLastWeekday: Boolean): DateTime {
        val day = currStart.dayOfWeek
        var order = (currStart.dayOfMonth - 1) / 7
        val properMonth = currStart.withDayOfMonth(7).plusMonths(repeatInterval / MONTH).withDayOfWeek(day)
        var firstProperDay = properMonth.dayOfMonth % 7
        if (firstProperDay == 0)
            firstProperDay = properMonth.dayOfMonth

        // check if it should be for example Fourth Monday, or Last Monday
        if (forceLastWeekday && (order == 3 || order == 4)) {
            val originalDateTime = Formatter.getDateTimeFromTS(original.startTS)
            val isLastWeekday = originalDateTime.monthOfYear != originalDateTime.plusDays(7).monthOfYear
            if (isLastWeekday)
                order = -1
        }

        val daysCnt = properMonth.dayOfMonth().maximumValue
        var wantedDay = firstProperDay + order * 7
        if (wantedDay > daysCnt)
            wantedDay -= 7

        if (order == -1) {
            wantedDay = firstProperDay + ((daysCnt - firstProperDay) / 7) * 7
        }

        return properMonth.withDayOfMonth(wantedDay)
    }

    fun getIsAllDay() = flags and FLAG_ALL_DAY != 0

    fun getReminders() = setOf(reminder1Minutes, reminder2Minutes, reminder3Minutes).filter { it != REMINDER_OFF }

    // properly return the start time of all-day events as midnight
    fun getEventStartTS(): Int {
        return if (getIsAllDay()) {
            Formatter.getDateTimeFromTS(startTS).withTime(0, 0, 0, 0).seconds()
        } else {
            startTS
        }
    }

    fun getCalDAVEventId(): Long {
        return try {
            (importId.split("-").lastOrNull() ?: "0").toString().toLong()
        } catch (e: NumberFormatException) {
            0L
        }
    }

    fun getCalDAVCalendarId() = if (source.startsWith(CALDAV)) (source.split("-").lastOrNull() ?: "0").toString().toInt() else 0
}
