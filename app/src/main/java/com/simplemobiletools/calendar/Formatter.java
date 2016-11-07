package com.simplemobiletools.calendar;

import android.content.Context;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Formatter {
    public static final String DAYCODE_PATTERN = "YYYYMMdd";
    public static final String YEAR_PATTERN = "YYYY";
    private static final String DAY_PATTERN = "d";
    private static final String DAY_OF_WEEK_PATTERN = "EEE";
    private static final String EVENT_DATE_PATTERN = "d YYYY"; // MMMM doesn't give the proper month name in some languages
    private static final String EVENT_TIME_PATTERN = "HH:mm";

    public static String getEventDate(Context context, String dayCode) {
        final DateTime dateTime = getDateTimeFromCode(dayCode);
        final String day = dateTime.toString(DAY_PATTERN);
        final String year = dateTime.toString(YEAR_PATTERN);
        final int monthIndex = Integer.valueOf(dayCode.substring(4, 6)) - 1;
        final String month = getMonthName(context, monthIndex);
        String date = month + " " + day;
        if (!year.equals(new DateTime().toString(YEAR_PATTERN)))
            date += " " + year;
        return date;
    }

    public static String getEventDate(Context context, DateTime dateTime) {
        return getDayTitle(context, getDayCodeFromDateTime(dateTime));
    }

    public static String getDayTitle(Context context, String dayCode) {
        String date = getEventDate(context, dayCode);
        final DateTime dateTime = getDateTimeFromCode(dayCode);
        final String day = dateTime.toString(DAY_OF_WEEK_PATTERN);
        return date + " (" + day + ")";
    }

    public static String getEventTime(DateTime dateTime) {
        return dateTime.toString(EVENT_TIME_PATTERN);
    }

    public static DateTime getDateTimeFromCode(String dayCode) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.UTC);
        return dateTimeFormatter.parseDateTime(dayCode);
    }

    public static DateTime getLocalDateTimeFromCode(String dayCode) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DAYCODE_PATTERN).withZone(DateTimeZone.getDefault());
        return dateTimeFormatter.parseDateTime(dayCode);
    }

    public static String getTime(int ts) {
        final DateTime dateTime = getDateTimeFromTS(ts);
        return getEventTime(dateTime);
    }

    public static int getDayStartTS(String dayCode) {
        final DateTime dateTime = getLocalDateTimeFromCode(dayCode);
        return (int) (dateTime.getMillis() / 1000);
    }

    public static int getDayEndTS(String dayCode) {
        final DateTime dateTime = getLocalDateTimeFromCode(dayCode);
        return (int) (dateTime.plusDays(1).minusMinutes(1).getMillis() / 1000);
    }

    public static String getDayCodeFromTS(int ts) {
        final DateTime dateTime = getDateTimeFromTS(ts);
        return dateTime.toString(Formatter.DAYCODE_PATTERN);
    }

    public static String getDayCodeFromDateTime(DateTime dateTime) {
        return dateTime.toString(Formatter.DAYCODE_PATTERN);
    }

    public static DateTime getDateTimeFromTS(int ts) {
        return new DateTime(ts * 1000L, DateTimeZone.getDefault());
    }

    // use manually translated month names, as DateFormat and Joda have issues with a lot of languages
    public static String getMonthName(Context context, int id) {
        return context.getResources().getStringArray(R.array.months)[id];
    }
}
