package com.simplemobiletools.calendar.pro.models

import android.provider.CalendarContract.Attendees
import androidx.collection.LongSparseArray
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.commons.extensions.addBitIf
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.io.Serializable

@Entity(tableName = "events", indices = [(Index(value = ["id"], unique = true))])
data class Event(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "start_ts") var startTS: Long = 0L,
    @ColumnInfo(name = "end_ts") var endTS: Long = 0L,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "location") var location: String = "",
    @ColumnInfo(name = "description") var description: String = "",
    @ColumnInfo(name = "reminder_1_minutes") var reminder1Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "reminder_2_minutes") var reminder2Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "reminder_3_minutes") var reminder3Minutes: Int = REMINDER_OFF,
    @ColumnInfo(name = "reminder_1_type") var reminder1Type: Int = REMINDER_NOTIFICATION,
    @ColumnInfo(name = "reminder_2_type") var reminder2Type: Int = REMINDER_NOTIFICATION,
    @ColumnInfo(name = "reminder_3_type") var reminder3Type: Int = REMINDER_NOTIFICATION,
    @ColumnInfo(name = "repeat_interval") var repeatInterval: Int = 0,
    @ColumnInfo(name = "repeat_rule") var repeatRule: Int = 0,
    @ColumnInfo(name = "repeat_limit") var repeatLimit: Long = 0L,
    @ColumnInfo(name = "repetition_exceptions") var repetitionExceptions: List<String> = emptyList(),
    @ColumnInfo(name = "attendees") var attendees: List<Attendee> = emptyList(),
    @ColumnInfo(name = "import_id") var importId: String = "",
    @ColumnInfo(name = "time_zone") var timeZone: String = "",
    @ColumnInfo(name = "flags") var flags: Int = 0,
    @ColumnInfo(name = "event_type") var eventType: Long = REGULAR_EVENT_TYPE_ID,
    @ColumnInfo(name = "parent_id") var parentId: Long = 0,
    @ColumnInfo(name = "last_updated") var lastUpdated: Long = 0L,
    @ColumnInfo(name = "source") var source: String = SOURCE_SIMPLE_CALENDAR,
    @ColumnInfo(name = "availability") var availability: Int = 0,
    @ColumnInfo(name = "color") var color: Int = 0,
    @ColumnInfo(name = "type") var type: Int = TYPE_EVENT
) : Serializable {

    companion object {
        private const val serialVersionUID = -32456795132345616L
    }

    fun addIntervalTime(original: Event) {
        val oldStart = Formatter.getDateTimeFromTS(startTS)
        val newStart = when (repeatInterval) {
            DAY -> oldStart.plusDays(1)
            else -> {
                when {
                    repeatInterval % YEAR == 0 -> when (repeatRule) {
                        REPEAT_ORDER_WEEKDAY -> addXthDayInterval(oldStart, original, false)
                        REPEAT_ORDER_WEEKDAY_USE_LAST -> addXthDayInterval(oldStart, original, true)
                        else -> addYearsWithSameDay(oldStart)
                    }

                    repeatInterval % MONTH == 0 -> when (repeatRule) {
                        REPEAT_SAME_DAY -> addMonthsWithSameDay(oldStart, original)
                        REPEAT_ORDER_WEEKDAY -> addXthDayInterval(oldStart, original, false)
                        REPEAT_ORDER_WEEKDAY_USE_LAST -> addXthDayInterval(oldStart, original, true)
                        else -> oldStart.plusMonths(repeatInterval / MONTH).dayOfMonth().withMaximumValue()
                    }

                    repeatInterval % WEEK == 0 -> {
                        // step through weekly repetition by days too, as events can trigger multiple times a week
                        oldStart.plusDays(1)
                    }

                    else -> oldStart.plusSeconds(repeatInterval)
                }
            }
        }

        val newStartTS = newStart.seconds()
        val newEndTS = newStartTS + (endTS - startTS)
        startTS = newStartTS
        endTS = newEndTS
    }

    // if an event should happen on 29th Feb. with Same Day yearly repetition, show it only on leap years
    private fun addYearsWithSameDay(currStart: DateTime): DateTime {
        var newDateTime = currStart.plusYears(repeatInterval / YEAR)

        // Date may slide within the same month
        if (newDateTime.dayOfMonth != currStart.dayOfMonth) {
            while (newDateTime.dayOfMonth().maximumValue < currStart.dayOfMonth) {
                newDateTime = newDateTime.plusYears(repeatInterval / YEAR)
            }
            newDateTime = newDateTime.withDayOfMonth(currStart.dayOfMonth)
        }
        return newDateTime
    }

    // if an event should happen on 31st with Same Day monthly repetition, dont show it at all at months with 30 or less days
    private fun addMonthsWithSameDay(currStart: DateTime, original: Event): DateTime {
        var newDateTime = currStart.plusMonths(repeatInterval / MONTH)
        if (newDateTime.dayOfMonth == currStart.dayOfMonth) {
            return newDateTime
        }

        while (newDateTime.dayOfMonth().maximumValue < Formatter.getDateTimeFromTS(original.startTS).dayOfMonth().maximumValue) {
            newDateTime = newDateTime.plusMonths(repeatInterval / MONTH)
            newDateTime = try {
                newDateTime.withDayOfMonth(currStart.dayOfMonth)
            } catch (e: Exception) {
                newDateTime
            }
        }
        return newDateTime
    }

    // handle monthly repetitions like Third Monday
    private fun addXthDayInterval(currStart: DateTime, original: Event, forceLastWeekday: Boolean): DateTime {
        val day = currStart.dayOfWeek
        var order = (currStart.dayOfMonth - 1) / 7
        var properMonth = currStart.withDayOfMonth(7).plusMonths(repeatInterval / MONTH).withDayOfWeek(day)
        var wantedDay: Int

        // check if it should be for example Fourth Monday, or Last Monday
        if (forceLastWeekday && (order == 3 || order == 4)) {
            val originalDateTime = Formatter.getDateTimeFromTS(original.startTS)
            val isLastWeekday = originalDateTime.monthOfYear != originalDateTime.plusDays(7).monthOfYear
            if (isLastWeekday)
                order = -1
        }

        if (order == -1) {
            wantedDay = properMonth.dayOfMonth + ((properMonth.dayOfMonth().maximumValue - properMonth.dayOfMonth) / 7) * 7
        } else {
            wantedDay = properMonth.dayOfMonth + (order - (properMonth.dayOfMonth - 1) / 7) * 7
            while (properMonth.dayOfMonth().maximumValue < wantedDay) {
                properMonth = properMonth.withDayOfMonth(7).plusMonths(repeatInterval / MONTH).withDayOfWeek(day)
                wantedDay = properMonth.dayOfMonth + (order - (properMonth.dayOfMonth - 1) / 7) * 7
            }
        }

        return properMonth.withDayOfMonth(wantedDay)
    }

    fun getIsAllDay() = flags and FLAG_ALL_DAY != 0
    fun hasMissingYear() = flags and FLAG_MISSING_YEAR != 0
    fun isTask() = type == TYPE_TASK
    fun isTaskCompleted() = isTask() && flags and FLAG_TASK_COMPLETED != 0

    fun getReminders() = listOf(
        Reminder(reminder1Minutes, reminder1Type),
        Reminder(reminder2Minutes, reminder2Type),
        Reminder(reminder3Minutes, reminder3Type)
    ).filter { it.minutes != REMINDER_OFF }

    // properly return the start time of all-day events as midnight
    fun getEventStartTS(): Long {
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

    // check if it's the proper week, for events repeating every x weeks
    // get the week number since 1970, not just in the current year
    fun isOnProperWeek(startTimes: LongSparseArray<Long>): Boolean {
        val initialWeekNumber = Formatter.getDateTimeFromTS(startTimes[id!!]!!).withTimeAtStartOfDay().millis / (7 * 24 * 60 * 60 * 1000f)
        val currentWeekNumber = Formatter.getDateTimeFromTS(startTS).withTimeAtStartOfDay().millis / (7 * 24 * 60 * 60 * 1000f)
        return (Math.round(initialWeekNumber) - Math.round(currentWeekNumber)) % (repeatInterval / WEEK) == 0
    }

    fun updateIsPastEvent() {
        val endTSToCheck = if (startTS < getNowSeconds() && getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(endTS))
        } else {
            endTS
        }
        isPastEvent = endTSToCheck < getNowSeconds()
    }

    fun addRepetitionException(dayCode: String) {
        var newRepetitionExceptions = repetitionExceptions.toMutableList()
        newRepetitionExceptions.add(dayCode)
        newRepetitionExceptions = newRepetitionExceptions.distinct().toMutableList() as ArrayList<String>
        repetitionExceptions = newRepetitionExceptions
    }

    var isPastEvent: Boolean
        get() = flags and FLAG_IS_IN_PAST != 0
        set(isPastEvent) {
            flags = flags.addBitIf(isPastEvent, FLAG_IS_IN_PAST)
        }

    fun getTimeZoneString(): String {
        return if (timeZone.isNotEmpty() && getAllTimeZones().map { it.zoneName }.contains(timeZone)) {
            timeZone
        } else {
            DateTimeZone.getDefault().id
        }
    }

    fun isAttendeeInviteDeclined() = attendees.any {
        it.isMe && it.status == Attendees.ATTENDEE_STATUS_DECLINED
    }
}
