package com.simplemobiletools.calendar.interfaces

import com.simplemobiletools.calendar.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}
