package com.simplemobiletools.calendar

import android.content.Context
import com.simplemobiletools.calendar.models.Day
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTime
import java.util.*

class MonthlyCalendarImpl(val mCallback: MonthlyCalendar, val mContext: Context) : DBHelper.GetEventsListener {
    private val DAYS_CNT = 42
    private val YEAR_PATTERN = "YYYY"

    private val mToday: String
    var mEvents: List<Event>

    lateinit var mTargetDate: DateTime

    init {
        mToday = DateTime().toString(Formatter.DAYCODE_PATTERN)
        mEvents = ArrayList<Event>()
    }

    fun updateMonthlyCalendar(targetDate: DateTime) {
        mTargetDate = targetDate
        val startTS = Formatter.getDayStartTS(Formatter.getDayCodeFromDateTime(mTargetDate.minusMonths(1)))
        val endTS = Formatter.getDayEndTS(Formatter.getDayCodeFromDateTime(mTargetDate.plusMonths(1)))
        DBHelper(mContext).getEvents(startTS, endTS, this)
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
        if (!Config.newInstance(mContext).isSundayFirst)
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
            val day = Day(value, isThisMonth, isToday, dayCode, hasEvent(dayCode), newDay.weekOfWeekyear)
            days.add(day)
            value++
        }

        mCallback.updateMonthlyCalendar(monthName, days)
    }

    private fun hasEvent(dayCode: String): Boolean {
        for (event in mEvents) {
            if (Formatter.getDayCodeFromTS(event.startTS) == dayCode) {
                return true
            }
        }
        return false
    }

    private fun isToday(targetDate: DateTime, curDayInMonth: Int): Boolean {
        return targetDate.withDayOfMonth(curDayInMonth).toString(Formatter.DAYCODE_PATTERN) == mToday
    }

    private val monthName: String
        get() {
            var month = Formatter.getMonthName(mContext, mTargetDate.monthOfYear - 1)
            val targetYear = mTargetDate.toString(YEAR_PATTERN)
            if (targetYear != DateTime().toString(YEAR_PATTERN)) {
                month += " $targetYear"
            }
            return month
        }

    override fun gotEvents(events: MutableList<Event>) {
        mEvents = events
        getDays()
    }
}
