package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.util.SparseArray
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.interfaces.YearlyCalendar
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTime
import java.util.*

class YearlyCalendarImpl(val callback: YearlyCalendar, val context: Context, val year: Int) : DBHelper.GetEventsListener {

    fun getEvents(year: Int) {
        val startDateTime = DateTime().withTime(0, 0, 0, 0).withDate(year, 1, 1)
        val startTS = startDateTime.seconds()
        val endTS = startDateTime.plusYears(1).minusSeconds(1).seconds()
        DBHelper(context).getEvents(startTS, endTS, this)
    }

    override fun gotEvents(events: MutableList<Event>) {
        val arr = SparseArray<ArrayList<Int>>(12)
        for (event in events) {
            val startDateTime = DateTime().withMillis(event.startTS * 1000L)
            markDay(arr, startDateTime)

            val startCode = Formatter.getDayCodeFromDateTime(startDateTime)
            val endDateTime = DateTime().withMillis(event.endTS * 1000L)
            val endCode = Formatter.getDayCodeFromDateTime(endDateTime)
            if (startCode != endCode) {
                var currDateTime = startDateTime
                while (Formatter.getDayCodeFromDateTime(currDateTime) != endCode) {
                    currDateTime = currDateTime.plusDays(1)
                    markDay(arr, currDateTime)
                }
            }
        }
        callback.updateYearlyCalendar(arr)
    }

    private fun markDay(arr: SparseArray<ArrayList<Int>>, dateTime: DateTime) {
        val month = dateTime.monthOfYear
        val day = dateTime.dayOfMonth

        if (arr[month] == null)
            arr.put(month, ArrayList<Int>())

        if (dateTime.year == year)
            arr.get(month).add(day)
    }
}
