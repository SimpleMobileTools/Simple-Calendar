package com.simplemobiletools.calendar.pro.models

import android.util.Range

data class EventWeeklyView(val range: Range<Int>, var slot: Int = 0, var slotMax: Int = 0, var collisions: ArrayList<Long> = ArrayList())
