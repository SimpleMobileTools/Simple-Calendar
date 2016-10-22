package com.simplemobiletools.calendar

import android.content.Context
import android.util.SparseArray
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTime
import java.util.*

class YearlyCalendarImpl(val callback: YearlyCalendar, val context: Context) : DBHelper.GetEventsListener {

    fun getEvents(year: Int) {
        val startDateTime = DateTime().withTime(0, 0, 0, 0).withDate(year, 1, 1)
        val startTS = (startDateTime.millis / 1000).toInt()
        val endTS = (startDateTime.plusYears(1).minusSeconds(1).millis / 1000).toInt()
        DBHelper(context).getEvents(startTS, endTS, this)
    }

    override fun gotEvents(events: MutableList<Event>) {
        val arr = SparseArray<ArrayList<Int>>(12)
        for (e in events) {
            val dateTime = DateTime().withMillis(e.startTS * 1000L)
            val month = dateTime.monthOfYear
            val day = dateTime.dayOfMonth
            if (arr[month] == null)
                arr.put(month, ArrayList<Int>())

            arr.get(month).add(day)
        }
        callback.updateYearlyCalendar(arr)
    }
}
