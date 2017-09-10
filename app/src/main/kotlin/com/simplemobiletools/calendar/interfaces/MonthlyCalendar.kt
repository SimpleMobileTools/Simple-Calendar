package com.simplemobiletools.calendar.interfaces

import com.simplemobiletools.calendar.models.DayMonthly

interface MonthlyCalendar {
    fun updateMonthlyCalendar(month: String, days: List<DayMonthly>)
}
