package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.getFilteredEvents
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.models.Day
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTime
import java.util.*

class MonthlyCalendarImpl(val mCallback: MonthlyCalendar, val mContext: Context) : DBHelper.GetEventsListener {
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
        DBHelper.newInstance(mContext).getEvents(startTS, endTS, this)
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
            val day = Day(value, isThisMonth, isToday, dayCode, false, newDay.weekOfWeekyear)
            days.add(day)
            value++
        }

        markDaysWithEvents(days)

        mCallback.updateMonthlyCalendar(monthName, days)
    }

    // it works more often than not, dont touch
    private fun markDaysWithEvents(days: ArrayList<Day>) {
        val eventCodes = ArrayList<String>()
        for (event in mEvents) {
            if (event.ignoreEventOccurrences.contains(event.startTS))
                continue

            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val endCode = Formatter.getDayCodeFromDateTime(endDateTime)

            var currDay = startDateTime
            eventCodes.add(Formatter.getDayCodeFromDateTime(currDay))

            while (Formatter.getDayCodeFromDateTime(currDay) != endCode) {
                currDay = currDay.plusDays(1)
                eventCodes.add(Formatter.getDayCodeFromDateTime(currDay))
            }
        }

        days.filter { eventCodes.contains(it.code) }.forEach { it.hasEvent = true }
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

    override fun gotEvents(events: MutableList<Event>) {
        if (mFilterEventTypes)
            mEvents = mContext.getFilteredEvents(events)
        else
            mEvents = events
        getDays()
    }
}
