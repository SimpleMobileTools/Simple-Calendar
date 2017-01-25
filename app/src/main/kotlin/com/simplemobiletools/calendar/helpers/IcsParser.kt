package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.models.Event

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

    fun parseIcs(context: Context) {
        val inputStream = context.resources.openRawResource(R.raw.sample)

        inputStream.bufferedReader().use {
            while (true) {
                val line = it.readLine()?.trim() ?: break
                if (line == BEGIN_EVENT) {
                    resetValues()
                } else if (line.startsWith(DTSTART)) {
                    val datetime = line.substring(DTSTART.length)
                    curStart = 0
                } else if (line.startsWith(DTEND)) {
                    val datetime = line.substring(DTEND.length)
                    curEnd = 0
                } else if (line.startsWith(SUMMARY)) {
                    curTitle = line.substring(SUMMARY.length)
                } else if (line.startsWith(DESCRIPTION)) {
                    curDescription = line.substring(DESCRIPTION.length)
                } else if (line == END || line == BEGIN_ALARM) {
                    if (curTitle.isEmpty() || curStart == -1 || curEnd == -1)
                        continue

                    val event = Event(0, 0, 0, curTitle, curDescription)
                    resetValues()
                }
            }
        }
    }

    fun resetValues() {
        curStart = -1
        curEnd = -1
        curTitle = ""
        curDescription = ""
    }
}
