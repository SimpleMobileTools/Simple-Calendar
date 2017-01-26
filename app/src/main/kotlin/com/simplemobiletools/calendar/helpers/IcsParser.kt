package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

object IcsParser {
    private val BEGIN_EVENT = "BEGIN:VEVENT"
    private val BEGIN_ALARM = "BEGIN:VALARM"
    private val END = "END:VEVENT"
    private val DTSTART = "DTSTART:"
    private val DTEND = "DTEND:"
    private val SUMMARY = "SUMMARY:"
    private val DESCRIPTION = "DESCRIPTION:"

    var curStart = -1
    var curEnd = -1
    var curTitle = ""
    var curDescription = ""

    fun parseIcs(context: Context, reminderMinutes: Int, path: String) {
        val inputStream = context.resources.openRawResource(R.raw.sample)

        inputStream.bufferedReader().use {
            while (true) {
                val line = it.readLine()?.trim() ?: break
                if (line == BEGIN_EVENT) {
                    resetValues()
                } else if (line.startsWith(DTSTART)) {
                    curStart = getTimestamp(line.substring(DTSTART.length))
                } else if (line.startsWith(DTEND)) {
                    curEnd = getTimestamp(line.substring(DTEND.length))
                } else if (line.startsWith(SUMMARY)) {
                    curTitle = line.substring(SUMMARY.length)
                } else if (line.startsWith(DESCRIPTION)) {
                    curDescription = line.substring(DESCRIPTION.length)
                } else if (line == END || line == BEGIN_ALARM) {
                    if (curTitle.isEmpty() || curStart == -1 || curEnd == -1)
                        continue

                    val event = Event(0, curStart, curEnd, curTitle, curDescription, reminderMinutes)
                    resetValues()
                }
            }
        }
    }

    private fun getTimestamp(fullString: String): Int {
        val digitString = fullString.replace("T", "").replace("Z", "")
        val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss")
        return dateTimeFormat.parseDateTime(digitString).withZoneRetainFields(DateTimeZone.UTC).seconds()
    }

    private fun resetValues() {
        curStart = -1
        curEnd = -1
        curTitle = ""
        curDescription = ""
    }
}
