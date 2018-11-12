package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import com.simplemobiletools.calendar.pro.extensions.dbHelper
import com.simplemobiletools.calendar.pro.interfaces.WeeklyCalendar
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.helpers.WEEK_SECONDS
import java.util.*

class WeeklyCalendarImpl(val callback: WeeklyCalendar, val context: Context) {
    var mEvents = ArrayList<Event>()

    fun updateWeeklyCalendar(weekStartTS: Int) {
        val endTS = weekStartTS + WEEK_SECONDS
        context.dbHelper.getEvents(weekStartTS, endTS) {
            mEvents = it
            callback.updateWeeklyCalendar(it)
        }
    }
}
