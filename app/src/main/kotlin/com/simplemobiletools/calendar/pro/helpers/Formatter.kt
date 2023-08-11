package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.commons.extensions.substringTo
import org.joda.time.DateTime
import org.joda.time.DateTimeFieldType
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

object Formatter {
    const val DAYCODE_PATTERN = "YYYYMMdd"
    const val YEAR_PATTERN = "YYYY"
    const val TIME_PATTERN = "HHmmss"
    private const val MONTH_PATTERN = "MMM"
    private const val DAY_PATTERN = "d"
    private const val DAY_OF_WEEK_PATTERN = "EEE"
    private const val DATE_DAY_PATTERN = "d EEEE"
    private const val PATTERN_TIME_12 = "hh:mm a"
    private const val PATTERN_TIME_24 = "HH:mm"

    private const val PATTERN_HOURS_12 = "h a"
    private const val PATTERN_HOURS_24 = "HH"

    fun getDateFromCode(dayCode: String, shortMonth: Boolean = false): String {
        val dateTime = getDateTimeFromCode(dayCode)
        var pattern = if (shortMonth) "MMMd" else "MMMMd";
        if (dateTime.get(DateTimeFieldType.year()) != DateTime().get(DateTimeFieldType.year())) {
            pattern = pattern + YEAR_PATTERN
        }
        val loc_pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern)
        return SimpleDateFormat(loc_pattern, Locale.getDefault()).format(dateTime.toDate())
    }

    fun getDayTitle(context: Context, dayCode: String, addDayOfWeek: Boolean = true): String {
        val date = getDateFromCode(dayCode)
        val dateTime = getDateTimeFromCode(dayCode)
        val day = SimpleDateFormat("E", Locale.getDefault()).format(dateTime.toDate())
        return if (addDayOfWeek)
            "$date ($day)"
        else
            date
    }

    fun getDateDayTitle(dayCode: String): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val loc_pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), DATE_DAY_PATTERN)
        return SimpleDateFormat(loc_pattern, Locale.getDefault()).format(dateTime.toDate())
    }

    fun getLongMonthYear(context: Context, dayCode: String): String {
        val dateTime = getDateTimeFromCode(dayCode)
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(dateTime.toDate())
        val year = dateTime.toString(YEAR_PATTERN)
        var date = month

        if (year != DateTime().toString(YEAR_PATTERN)) {
            date += " $year"
        }

        return date
    }

    fun getDate(context: Context, dateTime: DateTime, addDayOfWeek: Boolean = true) = getDayTitle(context, getDayCodeFromDateTime(dateTime), addDayOfWeek)

    fun getFullDate(context: Context, dateTime: DateTime): String {
        return DateFormat.getDateInstance(1).format(dateTime.toDate())
    }

    fun getTodayCode() = getDayCodeFromTS(getNowSeconds())

    fun getTodayDayNumber() = getDateTimeFromTS(getNowSeconds()).toString(DAY_PATTERN)

    fun getCurrentMonthShort() = getDateTimeFromTS(getNowSeconds()).toString(MONTH_PATTERN)

    fun getTime(context: Context, dateTime: DateTime) = dateTime.toString(getTimePattern(context))

    fun getDateTimeFromCode(dayCode: String) = DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.UTC).parseDateTime(dayCode)

    fun getLocalDateTimeFromCode(dayCode: String) =
        DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.getDefault()).parseLocalDate(dayCode).toDateTimeAtStartOfDay()

    fun getTimeFromTS(context: Context, ts: Long) = getTime(context, getDateTimeFromTS(ts))

    fun getDayStartTS(dayCode: String) = getLocalDateTimeFromCode(dayCode).seconds()

    fun getDayEndTS(dayCode: String) = getLocalDateTimeFromCode(dayCode).plusDays(1).minusMinutes(1).seconds()

    fun getDayCodeFromDateTime(dateTime: DateTime) = dateTime.toString(DAYCODE_PATTERN)

    fun getDateFromTS(ts: Long) = LocalDate(ts * 1000L, DateTimeZone.getDefault())

    fun getDateTimeFromTS(ts: Long) = DateTime(ts * 1000L, DateTimeZone.getDefault())

    fun getUTCDateTimeFromTS(ts: Long) = DateTime(ts * 1000L, DateTimeZone.UTC)

    // use manually translated month names, as DateFormat and Joda have issues with a lot of languages
    fun getMonthName(context: Context, id: Int) = context.resources.getStringArray(R.array.months)[id - 1]

    fun getShortMonthName(context: Context, id: Int) = context.resources.getStringArray(R.array.months_short)[id - 1]

    fun getHourPattern(context: Context) = if (context.config.use24HourFormat) PATTERN_HOURS_24 else PATTERN_HOURS_12

    fun getTimePattern(context: Context) = if (context.config.use24HourFormat) PATTERN_TIME_24 else PATTERN_TIME_12

    fun getExportedTime(ts: Long): String {
        val dateTime = DateTime(ts, DateTimeZone.UTC)
        return "${dateTime.toString(DAYCODE_PATTERN)}T${dateTime.toString(TIME_PATTERN)}Z"
    }

    fun getDayCodeFromTS(ts: Long): String {
        val daycode = getDateTimeFromTS(ts).toString(DAYCODE_PATTERN)
        return if (daycode.isNotEmpty()) {
            daycode
        } else {
            "0"
        }
    }

    fun getUTCDayCodeFromTS(ts: Long) = getUTCDateTimeFromTS(ts).toString(DAYCODE_PATTERN)

    fun getYearFromDayCode(dayCode: String) = getDateTimeFromCode(dayCode).toString(YEAR_PATTERN)

    fun getShiftedTS(dateTime: DateTime, toZone: DateTimeZone) = dateTime.withTimeAtStartOfDay().withZoneRetainFields(toZone).seconds()

    fun getShiftedLocalTS(ts: Long) = getShiftedTS(dateTime = getUTCDateTimeFromTS(ts), toZone = DateTimeZone.getDefault())

    fun getShiftedUtcTS(ts: Long) = getShiftedTS(dateTime = getDateTimeFromTS(ts), toZone = DateTimeZone.UTC)
}
