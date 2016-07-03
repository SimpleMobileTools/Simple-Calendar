package com.simplemobiletools.calendar;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Formatter {
    private static final String EVENT_DATE_PATTERN = "MMMM d YYYY";
    private static final String EVENT_TIME_PATTERN = "HH:mm";

    public static String getEventDate(String dayCode) {
        return getDateTime(dayCode).toString(EVENT_DATE_PATTERN);
    }

    public static String getEventDate(DateTime dateTime) {
        return dateTime.toString(EVENT_DATE_PATTERN);
    }

    public static String getEventTime(DateTime dateTime) {
        return dateTime.toString(EVENT_TIME_PATTERN);
    }

    public static DateTime getDateTime(String dayCode) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(Constants.DATE_PATTERN);
        return dateTimeFormatter.parseDateTime(dayCode);
    }
}
