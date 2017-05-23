package com.simplemobiletools.calendar.helpers

import com.simplemobiletools.calendar.R

val LOW_ALPHA = .3f
val MEDIUM_ALPHA = .6f

val DAY_CODE = "day_code"
val YEAR_LABEL = "year"
val EVENT_ID = "event_id"
val EVENT_OCCURRENCE_TS = "event_occurrence_ts"
val NEW_EVENT_START_TS = "new_event_start_ts"
val WEEK_START_TIMESTAMP = "week_start_timestamp"
val NEW_EVENT_SET_HOUR_DURATION = "new_event_set_hour_duration"

val MONTHLY_VIEW = 1
val YEARLY_VIEW = 2
val EVENTS_LIST_VIEW = 3
val WEEKLY_VIEW = 4

val REMINDER_OFF = -1

val DAY = 86400
val WEEK = 604800
val MONTH = 2592000    // exact value not taken into account, Joda is used for adding months and years
val YEAR = 31536000

val DAY_MINUTES = 24 * 60
val DAY_SECONDS = 24 * 60 * 60
val WEEK_SECONDS = 7 * DAY_SECONDS

// Shared Preferences
val USE_24_HOUR_FORMAT = "use_24_hour_format"
val SUNDAY_FIRST = "sunday_first"
val WEEK_NUMBERS = "week_numbers"
val START_WEEKLY_AT = "start_weekly_at"
val END_WEEKLY_AT = "end_weekly_at"
val VIBRATE = "vibrate"
val REMINDER_SOUND = "reminder_sound"
val VIEW = "view"
val REMINDER_MINUTES = "reminder_minutes"
val DISPLAY_EVENT_TYPES = "display_event_types"
val GOOGLE_SYNC = "google_sync"
val SYNC_ACCOUNT_NAME = "sync_account_name"
val SNOOZE_DELAY = "snooze_delay"
val DISPLAY_PAST_EVENTS = "display_past_events"

val letterIDs = intArrayOf(R.string.sunday_letter, R.string.monday_letter, R.string.tuesday_letter, R.string.wednesday_letter,
        R.string.thursday_letter, R.string.friday_letter, R.string.saturday_letter)

// repeat_rule for weekly repetition
val MONDAY = 1
val TUESDAY = 2
val WEDNESDAY = 4
val THURSDAY = 8
val FRIDAY = 16
val SATURDAY = 32
val SUNDAY = 64
val EVERY_DAY = 127

// repeat_rule for monthly repetition
val REPEAT_MONTH_SAME_DAY = 1
val REPEAT_MONTH_EVERY_XTH_DAY = 2
val REPEAT_MONTH_LAST_DAY = 3

// special event flags
val FLAG_ALL_DAY = 1

// constants related to ICS file exporting / importing
val BEGIN_CALENDAR = "BEGIN:VCALENDAR"
val END_CALENDAR = "END:VCALENDAR"
val BEGIN_EVENT = "BEGIN:VEVENT"
val END_EVENT = "END:VEVENT"
val BEGIN_ALARM = "BEGIN:VALARM"
val END_ALARM = "END:VALARM"
val DTSTART = "DTSTART"
val DTEND = "DTEND"
val DURATION = "DURATION:"
val SUMMARY = "SUMMARY"
val DESCRIPTION = "DESCRIPTION:"
val UID = "UID:"
val ACTION = "ACTION:"
val TRIGGER = "TRIGGER:"
val RRULE = "RRULE:"
val CATEGORIES = "CATEGORIES:"
val STATUS = "STATUS:"
val EXDATE = "EXDATE"
val BYDAY = "BYDAY"
val BYMONTHDAY = "BYMONTHDAY"

val DISPLAY = "DISPLAY"
val FREQ = "FREQ"
val UNTIL = "UNTIL"
val COUNT = "COUNT"
val INTERVAL = "INTERVAL"
val CONFIRMED = "CONFIRMED"
val VALUE = "VALUE"
val DATE = "DATE"

val DAILY = "DAILY"
val WEEKLY = "WEEKLY"
val MONTHLY = "MONTHLY"
val YEARLY = "YEARLY"

val MO = "MO"
val TU = "TU"
val WE = "WE"
val TH = "TH"
val FR = "FR"
val SA = "SA"
val SU = "SU"
