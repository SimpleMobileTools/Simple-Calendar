package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.pro.models.DayMonthly
import com.simplemobiletools.calendar.pro.models.Event
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

class MonthlyCalendarImpl(val callback: MonthlyCalendar, val context: Context) {
    private val DAYS_CNT = 42
    private val YEAR_PATTERN = "YYYY"

    private val mToday: String = DateTime().toString(Formatter.DAYCODE_PATTERN)
    private var mEvents = ArrayList<Event>()

    lateinit var mTargetDate: DateTime

    fun updateMonthlyCalendar(targetDate: DateTime) {
        mTargetDate = targetDate
        val startTS = mTargetDate.minusDays(7).seconds()
        val endTS = mTargetDate.plusDays(43).seconds()
        context.eventsHelper.getEvents(startTS, endTS) {
            gotEvents(it)
        }
    }

    fun getMonth(targetDate: DateTime) {
        updateMonthlyCalendar(targetDate)
    }

    fun getDays(markDaysWithEvents: Boolean) {
        val days = ArrayList<DayMonthly>(DAYS_CNT)
        val currMonthDays = mTargetDate.dayOfMonth().maximumValue
        var firstDayIndex = mTargetDate.withDayOfMonth(1).dayOfWeek
        if (!context.config.isSundayFirst)
            firstDayIndex -= 1
        val prevMonthDays = mTargetDate.minusMonths(1).dayOfMonth().maximumValue

        var isThisMonth = false
        var isToday: Boolean
        var value = prevMonthDays - firstDayIndex + 1
        var curDay = mTargetDate

        for (i in 0 until DAYS_CNT) {
            when {
                i < firstDayIndex -> {
                    isThisMonth = false
                    curDay = mTargetDate.withDayOfMonth(1).minusMonths(1)
                }
                i == firstDayIndex -> {
                    value = 1
                    isThisMonth = true
                    curDay = mTargetDate
                }
                value == currMonthDays + 1 -> {
                    value = 1
                    isThisMonth = false
                    curDay = mTargetDate.withDayOfMonth(1).plusMonths(1)
                }
            }

            isToday = isToday(curDay, value)

            val newDay = curDay.withDayOfMonth(value)
            val dayCode = Formatter.getDayCodeFromDateTime(newDay)
            val day = DayMonthly(value, isThisMonth, isToday, dayCode, newDay.weekOfWeekyear, ArrayList(), i)
            days.add(day)
            value++
        }

        if (markDaysWithEvents) {
            markDaysWithEvents(days)
        } else {
            callback.updateMonthlyCalendar(context, monthName, days, false, mTargetDate)
        }
    }

    // it works more often than not, dont touch
    private fun markDaysWithEvents(days: ArrayList<DayMonthly>) {
        val dayEvents = HashMap<String, ArrayList<Event>>()
        mEvents.forEach {
            val startDateTime = Formatter.getDateTimeFromTS(it.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(it.endTS)
            val endCode = Formatter.getDayCodeFromDateTime(endDateTime)

            var currDay = startDateTime
            var dayCode = Formatter.getDayCodeFromDateTime(currDay)
            var currDayEvents = dayEvents[dayCode] ?: ArrayList()
            currDayEvents.add(it)
            dayEvents[dayCode] = currDayEvents

            while (Formatter.getDayCodeFromDateTime(currDay) != endCode) {
                currDay = currDay.plusDays(1)
                dayCode = Formatter.getDayCodeFromDateTime(currDay)
                currDayEvents = dayEvents[dayCode] ?: ArrayList()
                currDayEvents.add(it)
                dayEvents[dayCode] = currDayEvents
            }
        }

        days.filter { dayEvents.keys.contains(it.code) }.forEach {
            it.dayEvents = dayEvents[it.code]!!
        }
        callback.updateMonthlyCalendar(context, monthName, days, true, mTargetDate)
    }

    private fun isToday(targetDate: DateTime, curDayInMonth: Int): Boolean {
        val targetMonthDays = targetDate.dayOfMonth().maximumValue
        return targetDate.withDayOfMonth(Math.min(curDayInMonth, targetMonthDays)).toString(Formatter.DAYCODE_PATTERN) == mToday
    }

    private val monthName: String
        get() {
            var month = Formatter.getMonthName(context, mTargetDate.monthOfYear)
            val targetYear = mTargetDate.toString(YEAR_PATTERN)
            if (targetYear != DateTime().toString(YEAR_PATTERN)) {
                month += " $targetYear"
            }
            return month
        }

    private fun gotEvents(events: ArrayList<Event>) {
        mEvents = events
        getDays(true)
    }
}
