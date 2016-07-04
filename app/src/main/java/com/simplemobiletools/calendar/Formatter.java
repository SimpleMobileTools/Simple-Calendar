package com.simplemobiletools.calendar;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Formatter {
    public static final String DAYCODE_PATTERN = "YYMMdd";
    private static final String EVENT_DATE_PATTERN = "MMMM d YYYY";
    private static final String EVENT_TIME_PATTERN = "HH:mm";

    public static String getEventDate(String dayCode) {
        return getDateTimeFromCode(dayCode).toString(EVENT_DATE_PATTERN);
    }

    public static String getEventDate(DateTime dateTime) {
        return dateTime.toString(EVENT_DATE_PATTERN);
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
        final DateTime dateTime = new DateTime(ts * 1000L, DateTimeZone.getDefault());
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
        final DateTime dateTime = new DateTime(ts * 1000L, DateTimeZone.getDefault());
        return dateTime.toString(Formatter.DAYCODE_PATTERN);
    }

    public static String getDayCodeFromDateTime(DateTime dateTime) {
        return dateTime.toDateTime(DateTimeZone.getDefault()).toString(Formatter.DAYCODE_PATTERN);
    }
}
