package com.simplemobiletools.calendar.helpers

import com.simplemobiletools.calendar.R

val LOW_ALPHA = .3f
val MEDIUM_ALPHA = .6f
val HIGH_ALPHA = .8f

val DAY_CODE = "day_code"
val YEAR_LABEL = "year"
val EVENT_ID = "event_id"

val MONTHLY_VIEW = 1
val YEARLY_VIEW = 2
val EVENTS_LIST_VIEW = 3

val REMINDER_OFF = -1
val REMINDER_AT_START = 0
val REMINDER_CUSTOM = 1

val DAY = 86400
val WEEK = 604800
val BIWEEK = 1209600
val MONTH = 2592000    // exact value not taken into account, Joda is used for adding months and years
val YEAR = 31536000

val HOUR_MINS = 60
val DAY_MINS = 1440

// Shared Preferences
val SUNDAY_FIRST = "sunday_first"
val WEEK_NUMBERS = "week_numbers"
val VIBRATE = "vibrate"
val REMINDER_SOUND = "reminder_sound"
val VIEW = "view"
val REMINDER_TYPE = "reminder_type"
val REMINDER_MINUTES = "reminder_minutes"

val letterIDs = intArrayOf(R.string.sunday_letter, R.string.monday_letter, R.string.tuesday_letter, R.string.wednesday_letter,
        R.string.thursday_letter, R.string.friday_letter, R.string.saturday_letter)
