package com.simplemobiletools.calendar.models

import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.*
import org.joda.time.DateTime
import java.io.Serializable

data class Event(var id: Int = 0, var startTS: Int = 0, var endTS: Int = 0, var title: String = "", var description: String = "",
                 var reminder1Minutes: Int = -1, var reminder2Minutes: Int = -1, var reminder3Minutes: Int = -1, var repeatInterval: Int = 0,
                 var importId: String? = "", var flags: Int = 0, var repeatLimit: Int = 0) : Serializable {

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
            MONTH -> currStart.plusMonths(1)
            else -> currStart.plusYears(1)
        }
        val newStartTS = newStart.seconds()
        val newEndTS = newStartTS + (endTS - startTS)
        startTS = newStartTS
        endTS = newEndTS
    }

    val isAllDay = flags and FLAG_ALL_DAY != 0

    fun getRemindersCount(): Int {
        var cnt = 0
        if (reminder1Minutes != REMINDER_OFF)
            cnt++
        if (reminder2Minutes != REMINDER_OFF)
            cnt++
        if (reminder3Minutes != REMINDER_OFF)
            cnt++
        return cnt
    }

    fun getReminders() = arrayOf(reminder1Minutes, reminder2Minutes, reminder3Minutes).filter { it != REMINDER_OFF }
}
