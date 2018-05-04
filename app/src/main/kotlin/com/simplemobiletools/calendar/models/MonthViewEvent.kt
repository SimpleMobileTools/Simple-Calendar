package com.simplemobiletools.calendar.models

data class MonthViewEvent(val id: Int, val title: String, val startTS: Int, val color: Int, val startDayIndex: Int, val daysCnt: Int, val originalStartDayIndex: Int,
                          val isAllDay: Boolean, val isPastEvent: Boolean)
