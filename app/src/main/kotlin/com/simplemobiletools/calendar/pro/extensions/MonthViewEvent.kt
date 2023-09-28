package com.simplemobiletools.calendar.pro.extensions

import com.simplemobiletools.calendar.pro.models.MonthViewEvent

fun MonthViewEvent.shouldStrikeThrough() = isTaskCompleted || isAttendeeInviteDeclined
