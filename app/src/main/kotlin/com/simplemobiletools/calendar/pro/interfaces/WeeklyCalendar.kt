package com.simplemobiletools.calendar.pro.interfaces

import com.simplemobiletools.calendar.pro.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}
