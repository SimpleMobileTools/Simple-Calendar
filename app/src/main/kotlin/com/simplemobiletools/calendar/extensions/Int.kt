package com.simplemobiletools.calendar.extensions

import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.Event

fun Int.isTsOnValidDay(event: Event): Boolean {
    val dateTime = Formatter.getDateTimeFromTS(this)
    val power = Math.pow(2.0, (dateTime.dayOfWeek - 1).toDouble()).toInt()
    return event.repeatRule and power != 0
}
