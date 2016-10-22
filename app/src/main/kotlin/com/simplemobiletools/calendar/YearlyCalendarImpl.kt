package com.simplemobiletools.calendar

import android.content.Context
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTime

class YearlyCalendarImpl(callback: YearlyCalendar, context: Context) : DBHelper.GetEventsListener {
    val mContext = context

    fun getEvents(year: Int) {
        val startDateTime = DateTime().withTime(0, 0, 0, 0).withDate(year, 1, 1)
        val startTS = (startDateTime.millis / 1000).toInt()
        val endTS = (startDateTime.plusYears(1).minusSeconds(1).millis / 1000).toInt()
        DBHelper(mContext).getEvents(startTS, endTS, this)
    }

    override fun gotEvents(events: MutableList<Event>) {

    }
}
