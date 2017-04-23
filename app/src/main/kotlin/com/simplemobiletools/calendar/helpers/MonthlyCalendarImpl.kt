package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.util.SparseIntArray
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.getFilteredEvents
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.models.Day
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTime
import java.util.*

class MonthlyCalendarImpl(val mCallback: MonthlyCalendar, val mContext: Context) {
    private val DAYS_CNT = 42
    private val YEAR_PATTERN = "YYYY"

    private val mToday: String = DateTime().toString(Formatter.DAYCODE_PATTERN)
    var mEvents: List<Event>
    var mFilterEventTypes = true

    lateinit var mTargetDate: DateTime

    init {
        mEvents = ArrayList<Event>()
    }

    fun updateMonthlyCalendar(targetDate: DateTime, filterEventTypes: Boolean = true) {
        mFilterEventTypes = filterEventTypes
        mTargetDate = targetDate
        val startTS = mTargetDate.minusMonths(1).seconds()
        val endTS = mTargetDate.plusMonths(1).seconds()
        mContext.dbHelper.getEvents(startTS, endTS) {
            gotEvents(it)
        }
    }

    fun getPrevMonth() {
        updateMonthlyCalendar(mTargetDate.minusMonths(1))
    }

    fun getNextMonth() {
        updateMonthlyCalendar(mTargetDate.plusMonths(1))
    }

    fun getDays() {
        val days = ArrayList<Day>(DAYS_CNT)

        val currMonthDays = mTargetDate.dayOfMonth().maximumValue
        var firstDayIndex = mTargetDate.withDayOfMonth(1).dayOfWeek
        if (!mContext.config.isSundayFirst)
            firstDayIndex -= 1
        val prevMonthDays = mTargetDate.minusMonths(1).dayOfMonth().maximumValue

        var isThisMonth = false
        var isToday: Boolean
        var value = prevMonthDays - firstDayIndex + 1
        var curDay: DateTime = mTargetDate

        for (i in 0..DAYS_CNT - 1) {
            if (i < firstDayIndex) {
                isThisMonth = false
                curDay = mTargetDate.minusMonths(1)
            } else if (i == firstDayIndex) {
                value = 1
                isThisMonth = true
                curDay = mTargetDate
            } else if (value == currMonthDays + 1) {
                value = 1
                isThisMonth = false
                curDay = mTargetDate.plusMonths(1)
            }

            isToday = isThisMonth && isToday(mTargetDate, value)

            val newDay = curDay.withDayOfMonth(value)
            val dayCode = Formatter.getDayCodeFromDateTime(newDay)
            val day = Day(value, isThisMonth, isToday, dayCode, false, newDay.weekOfWeekyear, ArrayList<Int>())
            days.add(day)
            value++
        }

        markDaysWithEvents(days)
    }

    // it works more often than not, dont touch
    private fun markDaysWithEvents(days: ArrayList<Day>) {
        mContext.dbHelper.getEventTypes {
            val dayEvents = HashMap<String, ArrayList<Event>>()
            val eventTypeColors = SparseIntArray()
            it.forEach {
                eventTypeColors.put(it.id, it.color)
            }

            mEvents.forEach {
                val startDateTime = Formatter.getDateTimeFromTS(it.startTS)
                val endDateTime = Formatter.getDateTimeFromTS(it.endTS)
                val endCode = Formatter.getDayCodeFromDateTime(endDateTime)

                var currDay = startDateTime
                var dayCode = Formatter.getDayCodeFromDateTime(currDay)
                var currDayEvents = (dayEvents[dayCode] ?: ArrayList<Event>()).apply { add(it) }
                dayEvents.put(dayCode, currDayEvents)

                while (Formatter.getDayCodeFromDateTime(currDay) != endCode) {
                    currDay = currDay.plusDays(1)
                    dayCode = Formatter.getDayCodeFromDateTime(currDay)
                    currDayEvents = (dayEvents[dayCode] ?: ArrayList<Event>()).apply { add(it) }
                    dayEvents.put(dayCode, currDayEvents)
                }
            }

            days.filter { dayEvents.keys.contains(it.code) }.forEach {
                val day = it
                day.hasEvent = true

                val events = dayEvents[it.code]
                events!!.forEach {
                    day.eventColors.add(eventTypeColors[it.eventType])
                }
            }
            mCallback.updateMonthlyCalendar(monthName, days)
        }
    }

    private fun isToday(targetDate: DateTime, curDayInMonth: Int) =
            targetDate.withDayOfMonth(curDayInMonth).toString(Formatter.DAYCODE_PATTERN) == mToday

    private val monthName: String
        get() {
            var month = Formatter.getMonthName(mContext, mTargetDate.monthOfYear)
            val targetYear = mTargetDate.toString(YEAR_PATTERN)
            if (targetYear != DateTime().toString(YEAR_PATTERN)) {
                month += " $targetYear"
            }
            return month
        }

    fun gotEvents(events: MutableList<Event>) {
        if (mFilterEventTypes)
            mEvents = mContext.getFilteredEvents(events)
        else
            mEvents = events

        getDays()
    }
}
