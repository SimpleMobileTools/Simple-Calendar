package com.simplemobiletools.calendar;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Formatter {
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
        final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(Constants.DATE_PATTERN).withZone(DateTimeZone.UTC);
        return dateTimeFormatter.parseDateTime(dayCode);
    }

    public static String getTime(int ts) {
        final DateTime dateTime = new DateTime(ts * 1000L, DateTimeZone.UTC);
        return getEventTime(dateTime);
    }

    public static int getDayStartTS(String dayCode) {
        final DateTime dateTime = getDateTimeFromCode(dayCode);
        return (int) (dateTime.getMillis() / 1000);
    }

    public static int getDayEndTS(String dayCode) {
        final DateTime dateTime = getDateTimeFromCode(dayCode);
        return (int) (dateTime.plusDays(1).minusMinutes(1).getMillis() / 1000);
    }
}
