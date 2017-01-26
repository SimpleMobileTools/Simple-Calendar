package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.extensions.scheduleNotification
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.IcsParser.ImportResult.*
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.io.File

class IcsParser {
    enum class ImportResult {
        IMPORT_FAIL, IMPORT_OK, IMPORT_PARTIAL
    }

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

    var eventsImported = 0
    var eventsFailed = 0

    fun parseIcs(context: Context, reminderMinutes: Int, path: String): ImportResult {
        try {
            val dbHelper = DBHelper(context)

            File(path).inputStream().bufferedReader().use {
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
                        dbHelper.insert(event) {
                            context.scheduleNotification(event)
                        }
                        eventsImported++
                        resetValues()
                    }
                }
            }
        } catch (e: Exception) {
            eventsFailed++
        }

        return if (eventsImported == 0) {
            IMPORT_FAIL
        } else if (eventsFailed > 0) {
            IMPORT_PARTIAL
        } else {
            IMPORT_OK
        }
    }

    private fun getTimestamp(fullString: String): Int {
        try {
            val digitString = fullString.replace("T", "").replace("Z", "")
            val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss")
            return dateTimeFormat.parseDateTime(digitString).withZoneRetainFields(DateTimeZone.UTC).seconds()
        } catch (e: Exception) {
            eventsFailed++
            return -1
        }
    }

    private fun resetValues() {
        curStart = -1
        curEnd = -1
        curTitle = ""
        curDescription = ""
    }
}
