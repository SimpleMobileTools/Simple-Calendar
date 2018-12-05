package com.simplemobiletools.calendar.pro.interfaces

import android.util.SparseArray
import com.simplemobiletools.calendar.pro.models.DayYearly
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}
