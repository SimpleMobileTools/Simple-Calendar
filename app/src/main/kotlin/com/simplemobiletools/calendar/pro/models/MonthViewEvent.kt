package com.simplemobiletools.calendar.pro.models

data class MonthViewEvent(
    val id: Long,
    val title: String,
    val startTS: Long,
    val endTS: Long,
    val color: Int,
    val startDayIndex: Int,
    val daysCnt: Int,
    val originalStartDayIndex: Int,
    val isAllDay: Boolean,
    val isPastEvent: Boolean,
    val isTask: Boolean,
    val isTaskCompleted: Boolean,
    val isAttendeeInviteDeclined: Boolean,
)
