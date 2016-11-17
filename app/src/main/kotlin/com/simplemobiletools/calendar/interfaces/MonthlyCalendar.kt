package com.simplemobiletools.calendar.interfaces

import com.simplemobiletools.calendar.models.Day

interface MonthlyCalendar {
    fun updateMonthlyCalendar(month: String, days: List<Day>)
}
