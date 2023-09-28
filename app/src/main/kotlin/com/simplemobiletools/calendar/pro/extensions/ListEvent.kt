package com.simplemobiletools.calendar.pro.extensions

import com.simplemobiletools.calendar.pro.models.ListEvent

fun ListEvent.shouldStrikeThrough() = isTaskCompleted || isAttendeeInviteDeclined
