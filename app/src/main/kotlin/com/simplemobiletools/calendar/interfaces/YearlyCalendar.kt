package com.simplemobiletools.calendar.interfaces

import android.util.SparseArray
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<Int>>)
}
