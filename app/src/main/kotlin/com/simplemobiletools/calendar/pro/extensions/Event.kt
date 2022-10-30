package com.simplemobiletools.calendar.pro.extensions

import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.TWELVE_HOURS
import com.simplemobiletools.calendar.pro.models.Event
import org.joda.time.DateTimeZone

/** Shifts all-day events to local timezone such that the event starts and ends on the same time as in UTC */
fun Event.toLocalAllDayEvent() {
    require(this.getIsAllDay()) { "Must be an all day event!" }

    timeZone = DateTimeZone.getDefault().id
    startTS = Formatter.getShiftedLocalTS(startTS)
    endTS = Formatter.getShiftedLocalTS(endTS)
    if (endTS > startTS) {
        endTS -= TWELVE_HOURS
    }
}

/** Shifts all-day events to UTC such that the event starts on the same time in UTC too */
fun Event.toUtcAllDayEvent() {
    require(getIsAllDay()) { "Must be an all day event!" }

    if (endTS >= startTS) {
        endTS += TWELVE_HOURS
    }
    timeZone = DateTimeZone.UTC.id
    startTS = Formatter.getShiftedUtcTS(startTS)
    endTS = Formatter.getShiftedUtcTS(endTS)
}
