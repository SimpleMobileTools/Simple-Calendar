package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.text.format.DateFormat
import com.simplemobiletools.calendar.R
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat

object Formatter {
    val DAYCODE_PATTERN = "YYYYMMdd"
    val YEAR_PATTERN = "YYYY"
    private val DAY_PATTERN = "d"
    private val DAY_OF_WEEK_PATTERN = "EEE"
    private val PATTERN_TIME_12 = "h:mm a"
    private val PATTERN_TIME_24 = "k:mm"

    fun getDate(context: Context, dayCode: String): String {
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
        val date = getDate(context, dayCode)
        val dateTime = getDateTimeFromCode(dayCode)
        val day = dateTime.toString(DAY_OF_WEEK_PATTERN)
        return "$date ($day)"
    }

    fun getDate(context: Context, dateTime: DateTime) = getDayTitle(context, getDayCodeFromDateTime(dateTime))

    fun getTime(context: Context, dateTime: DateTime) = dateTime.toString(getTimePattern(context))

    fun getDateTimeFromCode(dayCode: String) = DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.UTC).parseDateTime(dayCode)

    fun getLocalDateTimeFromCode(dayCode: String) = DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.getDefault()).parseDateTime(dayCode)

    fun getTimeFromTS(context: Context, ts: Int) = getTime(context, getDateTimeFromTS(ts))

    fun getDayStartTS(dayCode: String) = (getLocalDateTimeFromCode(dayCode).millis / 1000).toInt()

    fun getDayEndTS(dayCode: String) = (getLocalDateTimeFromCode(dayCode).plusDays(1).minusMinutes(1).millis / 1000).toInt()

    fun getDayCodeFromTS(ts: Int) = getDateTimeFromTS(ts).toString(DAYCODE_PATTERN)

    fun getDayCodeFromDateTime(dateTime: DateTime) = dateTime.toString(DAYCODE_PATTERN)

    fun getDateTimeFromTS(ts: Int) = DateTime(ts * 1000L, DateTimeZone.getDefault())

    // use manually translated month names, as DateFormat and Joda have issues with a lot of languages
    fun getMonthName(context: Context, id: Int) = context.resources.getStringArray(R.array.months)[id]

    fun getTimePattern(context: Context) = if (DateFormat.is24HourFormat(context)) PATTERN_TIME_24 else PATTERN_TIME_12
}
