package com.simplemobiletools.calendar.helpers

const val LOW_ALPHA = .3f
const val MEDIUM_ALPHA = .6f
const val STORED_LOCALLY_ONLY = 0

const val DAY_CODE = "day_code"
const val YEAR_LABEL = "year"
const val EVENT_ID = "event_id"
const val IS_DUPLICATE_INTENT = "is_duplicate_intent"
const val EVENT_OCCURRENCE_TS = "event_occurrence_ts"
const val NEW_EVENT_START_TS = "new_event_start_ts"
const val WEEK_START_TIMESTAMP = "week_start_timestamp"
const val NEW_EVENT_SET_HOUR_DURATION = "new_event_set_hour_duration"
const val WEEK_START_DATE_TIME = "week_start_date_time"
const val CALDAV = "Caldav"
const val OPEN_MONTH = "open_month"

const val MONTHLY_VIEW = 1
const val YEARLY_VIEW = 2
const val EVENTS_LIST_VIEW = 3
const val WEEKLY_VIEW = 4
const val DAILY_VIEW = 5

const val REMINDER_OFF = -1

const val DAY = 86400
const val WEEK = 604800
const val MONTH = 2592001    // exact value not taken into account, Joda is used for adding months and years
const val YEAR = 31536000

// Shared Preferences
const val WEEK_NUMBERS = "week_numbers"
const val START_WEEKLY_AT = "start_weekly_at"
const val END_WEEKLY_AT = "end_weekly_at"
const val VIBRATE = "vibrate"
const val REMINDER_SOUND_URI = "reminder_sound_uri"
const val REMINDER_SOUND_TITLE = "reminder_sound_title"
const val VIEW = "view"
const val REMINDER_MINUTES = "reminder_minutes"
const val REMINDER_MINUTES_2 = "reminder_minutes_2"
const val REMINDER_MINUTES_3 = "reminder_minutes_3"
const val DISPLAY_EVENT_TYPES = "display_event_types"
const val FONT_SIZE = "font_size"
const val CALDAV_SYNC = "caldav_sync"
const val CALDAV_SYNCED_CALENDAR_IDS = "caldav_synced_calendar_ids"
const val LAST_USED_CALDAV_CALENDAR = "last_used_caldav_calendar"
const val LAST_USED_LOCAL_EVENT_TYPE_ID = "last_used_local_event_type_id"
const val DISPLAY_PAST_EVENTS = "display_past_events"
const val REPLACE_DESCRIPTION = "replace_description"
const val SHOW_GRID = "show_grid"
const val LOOP_REMINDERS = "loop_reminders"
const val DIM_PAST_EVENTS = "dim_past_events"
const val LAST_SOUND_URI = "last_sound_uri"
const val LAST_REMINDER_CHANNEL_ID = "last_reminder_channel_ID"
const val REMINDER_AUDIO_STREAM = "reminder_audio_stream"

// repeat_rule for monthly and yearly repetition
const val REPEAT_SAME_DAY = 1                           // i.e. 25th every month, or 3rd june (if yearly repetition)
const val REPEAT_ORDER_WEEKDAY_USE_LAST = 2             // i.e. every last sunday. 4th if a month has 4 sundays, 5th if 5 (or last sunday in june, if yearly)
const val REPEAT_LAST_DAY = 3                           // i.e. every last day of the month
const val REPEAT_ORDER_WEEKDAY = 4                      // i.e. every 4th sunday, even if a month has 4 sundays only (will stay 4th even at months with 5)

// special event flags
const val FLAG_ALL_DAY = 1

// constants related to ICS file exporting / importing
const val BEGIN_CALENDAR = "BEGIN:VCALENDAR"
const val END_CALENDAR = "END:VCALENDAR"
const val CALENDAR_PRODID = "PRODID:-//Simple Mobile Tools//NONSGML Event Calendar//EN"
const val CALENDAR_VERSION = "VERSION:2.0"
const val BEGIN_EVENT = "BEGIN:VEVENT"
const val END_EVENT = "END:VEVENT"
const val BEGIN_ALARM = "BEGIN:VALARM"
const val END_ALARM = "END:VALARM"
const val DTSTART = "DTSTART"
const val DTEND = "DTEND"
const val LAST_MODIFIED = "LAST-MODIFIED"
const val DURATION = "DURATION:"
const val SUMMARY = "SUMMARY"
const val DESCRIPTION = "DESCRIPTION:"
const val UID = "UID:"
const val ACTION = "ACTION:"
const val TRIGGER = "TRIGGER:"
const val RRULE = "RRULE:"
const val CATEGORIES = "CATEGORIES:"
const val STATUS = "STATUS:"
const val EXDATE = "EXDATE"
const val BYDAY = "BYDAY"
const val BYMONTHDAY = "BYMONTHDAY"
const val BYMONTH = "BYMONTH"
const val LOCATION = "LOCATION"

// this tag isn't a standard ICS tag, but there's no official way of adding a category color in an ics file
const val CATEGORY_COLOR = "CATEGORY_COLOR:"

const val DISPLAY = "DISPLAY"
const val FREQ = "FREQ"
const val UNTIL = "UNTIL"
const val COUNT = "COUNT"
const val INTERVAL = "INTERVAL"
const val CONFIRMED = "CONFIRMED"
const val VALUE = "VALUE"
const val DATE = "DATE"

const val DAILY = "DAILY"
const val WEEKLY = "WEEKLY"
const val MONTHLY = "MONTHLY"
const val YEARLY = "YEARLY"

const val MO = "MO"
const val TU = "TU"
const val WE = "WE"
const val TH = "TH"
const val FR = "FR"
const val SA = "SA"
const val SU = "SU"

// font sizes
const val FONT_SIZE_SMALL = 0
const val FONT_SIZE_MEDIUM = 1
const val FONT_SIZE_LARGE = 2

const val SOURCE_SIMPLE_CALENDAR = "simple-calendar"
const val SOURCE_IMPORTED_ICS = "imported-ics"
const val SOURCE_CONTACT_BIRTHDAY = "contact-birthday"
const val SOURCE_CONTACT_ANNIVERSARY = "contact-anniversary"

const val DELETE_SELECTED_OCCURRENCE = 0
const val DELETE_FUTURE_OCCURRENCES = 1
const val DELETE_ALL_OCCURRENCES = 2

fun getNowSeconds() = (System.currentTimeMillis() / 1000).toInt()
