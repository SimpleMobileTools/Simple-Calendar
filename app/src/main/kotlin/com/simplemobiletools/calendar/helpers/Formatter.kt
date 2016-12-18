package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.simplemobiletools.calendar.R
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

object Formatter {
    val DAYCODE_PATTERN = "YYYYMMdd"
    val YEAR_PATTERN = "YYYY"
    private val DAY_PATTERN = "d"
    private val DAY_OF_WEEK_PATTERN = "EEE"
    private val EVENT_TIME_PATTERN = "HH:mm"

    fun getEventDate(context: Context, dayCode: String): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_PATTERN)
        val year = dateTime.toString(YEAR_PATTERN)
        val monthIndex = Integer.valueOf(dayCode.substring(4, 6))!! - 1
        val month = getMonthName(context, monthIndex)
        var date = "$month $day"
        if (year != DateTime().toString(YEAR_PATTERN))
            date += " $year"
        return date
    }

    fun getDayTitle(context: Context, dayCode: String): String {
        val date = getEventDate(context, dayCode)
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_OF_WEEK_PATTERN)
        return "$date ($day)"
    }

    fun getEventDate(context: Context, dateTime: DateTime) = getDayTitle(context, getDayCodeFromDateTime(dateTime))

    fun getEventTime(dateTime: DateTime) = dateTime.toString(EVENT_TIME_PATTERN)

    fun getDateTimeFromCode(dayCode: String) = DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.UTC).parseDateTime(dayCode)

    fun getLocalDateTimeFromCode(dayCode: String) = DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.getDefault()).parseDateTime(dayCode)

    fun getTime(ts: Int) = getEventTime(getDateTimeFromTS(ts))

    fun getDayStartTS(dayCode: String) = (getLocalDateTimeFromCode(dayCode).millis / 1000).toInt()

    fun getDayEndTS(dayCode: String) = (getLocalDateTimeFromCode(dayCode).plusDays(1).minusMinutes(1).millis / 1000).toInt()

    fun getDayCodeFromTS(ts: Int) = getDateTimeFromTS(ts).toString(DAYCODE_PATTERN)

    fun getDayCodeFromDateTime(dateTime: DateTime) = dateTime.toString(DAYCODE_PATTERN)

    fun getDateTimeFromTS(ts: Int) = DateTime(ts * 1000L, DateTimeZone.getDefault())

    // use manually translated month names, as DateFormat and Joda have issues with a lot of languages
    fun getMonthName(context: Context, id: Int) = context.resources.getStringArray(R.array.months)[id]
}
