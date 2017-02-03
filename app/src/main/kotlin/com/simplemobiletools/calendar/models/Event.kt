package com.simplemobiletools.calendar.models

import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.*
import org.joda.time.DateTime
import java.io.Serializable

data class Event(var id: Int = 0, var startTS: Int = 0, var endTS: Int = 0, var title: String = "", var description: String = "",
                 var reminderMinutes: Int = 0, var repeatInterval: Int = 0, var importId: String = "", var flags: Int = 0) : Serializable {

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
}
