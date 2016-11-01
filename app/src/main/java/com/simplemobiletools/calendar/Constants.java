package com.simplemobiletools.calendar;

public class Constants {
    public static final float LOW_ALPHA = .2f;
    public static final float MEDIUM_ALPHA = .4f;
    public static final float HIGH_ALPHA = .8f;

    public static final String DAY_CODE = "day_code";
    public static final String YEAR_LABEL = "year";
    public static final String EVENT_ID = "event_id";

    public static final int MONTHLY_VIEW = 1;
    public static final int YEARLY_VIEW = 2;
    public static final int EVENTS_LIST_VIEW = 3;

    public static final int REMINDER_OFF = -1;
    public static final int REMINDER_AT_START = 0;
    public static final int REMINDER_CUSTOM = 1;

    public static final int DAY = 86400;
    public static final int WEEK = 604800;
    public static final int BIWEEK = 1209600;
    public static final int MONTH = 2592000;    // exact value not taken into account, Joda is used for adding months and years
    public static final int YEAR = 31536000;

    public static final int HOUR_MINS = 60;
    public static final int DAY_MINS = 1440;

    // Shared Preferences
    public static final String PREFS_KEY = "Calendar";
    public static final String IS_FIRST_RUN = "is_first_run";
    public static final String IS_DARK_THEME = "is_dark_theme";
    public static final String SUNDAY_FIRST = "sunday_first";
    public static final String WEEK_NUMBERS = "week_numbers";
    public static final String VIEW = "view";
    public static final String REMINDER_TYPE = "reminder_type";
    public static final String REMINDER_MINUTES = "reminder_minutes";
    public static final String WIDGET_BG_COLOR = "widget_bg_color";
    public static final String WIDGET_TEXT_COLOR = "widget_text_color";
    public static final String LAST_OTHER_REMINDER_MINS = "last_other_reminder_mins";
}
