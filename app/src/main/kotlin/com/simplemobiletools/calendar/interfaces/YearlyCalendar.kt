package com.simplemobiletools.calendar.interfaces

import android.util.SparseArray
import com.simplemobiletools.calendar.models.DayYearly
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}
