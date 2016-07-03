package com.simplemobiletools.calendar;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Formatter {
    private static final String EVENT_PATTERN = "d MMMM YYYY";

    public static String getEventDate(String dayCode) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(Constants.DATE_PATTERN);
        return dateTimeFormatter.parseDateTime(dayCode).toString(EVENT_PATTERN);
    }
}
