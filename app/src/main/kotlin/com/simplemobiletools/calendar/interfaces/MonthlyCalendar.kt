package com.simplemobiletools.calendar.interfaces

import android.content.Context
import com.simplemobiletools.calendar.models.DayMonthly

interface MonthlyCalendar {
    fun updateMonthlyCalendar(context: Context, month: String, days: ArrayList<DayMonthly>, checkedEvents: Boolean)
}
