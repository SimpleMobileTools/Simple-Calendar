package com.simplemobiletools.calendar.models

import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import org.joda.time.DateTime
import java.io.Serializable
import java.util.*

data class Event(var id: Int = 0, var startTS: Int = 0, var endTS: Int = 0, var title: String = "", var description: String = "",
                 var reminder1Minutes: Int = -1, var reminder2Minutes: Int = -1, var reminder3Minutes: Int = -1, var repeatInterval: Int = 0,
                 var importId: String? = "", var flags: Int = 0, var repeatLimit: Int = 0, var eventType: Int = DBHelper.REGULAR_EVENT_TYPE_ID,
                 var ignoreEventOccurrences: ArrayList<Int> = ArrayList()) : Serializable {

    companion object {
        private val serialVersionUID = -32456795132344616L
    }

    fun addIntervalTime() {
        val currStart = Formatter.getDateTimeFromTS(startTS)
        val newStart: DateTime
        newStart = when (repeatInterval) {
            DAY -> currStart.plusDays(1)
            WEEK -> currStart.plusWeeks(1)
            BIWEEK -> currStart.plusWeeks(2)
            else -> {
                if (repeatInterval % YEAR == 0) {
                    currStart.plusYears(repeatInterval / YEAR)
                } else if (repeatInterval % MONTH == 0) {
                    currStart.plusMonths(repeatInterval / MONTH)
                } else {
                    currStart.plusSeconds(repeatInterval)
                }
            }
        }
        val newStartTS = newStart.seconds()
        val newEndTS = newStartTS + (endTS - startTS)
        startTS = newStartTS
        endTS = newEndTS
    }

    val isAllDay = flags and FLAG_ALL_DAY != 0

    fun getReminders() = setOf(reminder1Minutes, reminder2Minutes, reminder3Minutes).filter { it != REMINDER_OFF }
}
