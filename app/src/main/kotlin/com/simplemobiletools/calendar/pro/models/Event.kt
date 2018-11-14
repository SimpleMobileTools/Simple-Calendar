package com.simplemobiletools.calendar.pro.models

import androidx.collection.LongSparseArray
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.commons.extensions.addBit
import com.simplemobiletools.commons.extensions.removeBit
import org.joda.time.DateTime
import java.io.Serializable

@Entity(tableName = "events", indices = [(Index(value = ["id"], unique = true))])
data class Event(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "start_ts") var startTS: Int = 0,
        @ColumnInfo(name = "end_ts") var endTS: Int = 0,
        @ColumnInfo(name = "title") var title: String = "",
        @ColumnInfo(name = "location") var location: String = "",
        @ColumnInfo(name = "description") var description: String = "",
        @ColumnInfo(name = "reminder_1_minutes") var reminder1Minutes: Int = -1,
        @ColumnInfo(name = "reminder_2_minutes") var reminder2Minutes: Int = -1,
        @ColumnInfo(name = "reminder_3_minutes") var reminder3Minutes: Int = -1,
        @ColumnInfo(name = "repeat_interval") var repeatInterval: Int = 0,
        @ColumnInfo(name = "repeat_rule") var repeatRule: Int = 0,
        @ColumnInfo(name = "repeat_limit") var repeatLimit: Int = 0,
        @ColumnInfo(name = "import_id") var importId: String = "",
        @ColumnInfo(name = "flags") var flags: Int = 0,
        @ColumnInfo(name = "event_type") var eventType: Long = REGULAR_EVENT_TYPE_ID,
        @ColumnInfo(name = "parent_id") var parentId: Long = 0,
        @ColumnInfo(name = "last_updated") var lastUpdated: Long = 0L,
        @ColumnInfo(name = "source") var source: String = SOURCE_SIMPLE_CALENDAR)
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

    fun getEventRepetition() = EventRepetition(null, id!!, repeatInterval, repeatRule, repeatLimit)

    // check if its the proper week, for events repeating every x weeks
    fun isOnProperWeek(startTimes: LongSparseArray<Int>): Boolean {
        val initialWeekOfYear = Formatter.getDateTimeFromTS(startTimes[id!!]!!).weekOfWeekyear
        val currentWeekOfYear = Formatter.getDateTimeFromTS(startTS).weekOfWeekyear
        return (currentWeekOfYear - initialWeekOfYear) % (repeatInterval / WEEK) == 0
    }

    fun updateIsPastEvent() {
        val endTSToCheck = if (startTS < getNowSeconds() && getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(endTS))
        } else {
            endTS
        }
        isPastEvent = endTSToCheck < getNowSeconds()
    }

    var isPastEvent: Boolean
        get() = flags and FLAG_IS_PAST_EVENT != 0
        set(isPastEvent) {
            flags = if (isPastEvent) {
                flags.addBit(FLAG_IS_PAST_EVENT)
            } else {
                flags.removeBit(FLAG_IS_PAST_EVENT)
            }
        }

    var color: Int = 0
}
