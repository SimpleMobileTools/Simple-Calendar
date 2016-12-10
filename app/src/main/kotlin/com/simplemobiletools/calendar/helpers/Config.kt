package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.content.SharedPreferences
import android.media.RingtoneManager
import java.util.*

class Config(val context: Context) {
    private val mPrefs: SharedPreferences

    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    init {
        mPrefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
    }

    var isFirstRun: Boolean
        get() = mPrefs.getBoolean(IS_FIRST_RUN, true)
        set(firstRun) = mPrefs.edit().putBoolean(IS_FIRST_RUN, firstRun).apply()

    var isDarkTheme: Boolean
        get() = mPrefs.getBoolean(IS_DARK_THEME, false)
        set(isDarkTheme) = mPrefs.edit().putBoolean(IS_DARK_THEME, isDarkTheme).apply()

    var isSundayFirst: Boolean
        get() {
            val isSundayFirst = Calendar.getInstance(Locale.getDefault()).firstDayOfWeek == Calendar.SUNDAY
            return mPrefs.getBoolean(SUNDAY_FIRST, isSundayFirst)
        }
        set(sundayFirst) = mPrefs.edit().putBoolean(SUNDAY_FIRST, sundayFirst).apply()

    var displayWeekNumbers: Boolean
        get() = mPrefs.getBoolean(WEEK_NUMBERS, false)
        set(displayWeekNumbers) = mPrefs.edit().putBoolean(WEEK_NUMBERS, displayWeekNumbers).apply()

    var vibrateOnReminder: Boolean
        get() = mPrefs.getBoolean(VIBRATE, false)
        set(vibrate) = mPrefs.edit().putBoolean(VIBRATE, vibrate).apply()

    var reminderSound: String
        get() = mPrefs.getString(REMINDER_SOUND, RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION).toString())
        set(path) = mPrefs.edit().putString(REMINDER_SOUND, path).apply()

    var storedView: Int
        get() = mPrefs.getInt(VIEW, MONTHLY_VIEW)
        set(view) = mPrefs.edit().putInt(VIEW, view).apply()

    var defaultReminderType: Int
        get() = mPrefs.getInt(REMINDER_TYPE, REMINDER_AT_START)
        set(type) {
            var newType = type
            if (newType == REMINDER_CUSTOM && defaultReminderMinutes == 0)
                newType = REMINDER_AT_START

            mPrefs.edit().putInt(REMINDER_TYPE, newType).apply()
        }

    var defaultReminderMinutes: Int
        get() = mPrefs.getInt(REMINDER_MINUTES, 10)
        set(mins) {
            if (mins == 0)
                defaultReminderType = REMINDER_AT_START
            mPrefs.edit().putInt(REMINDER_MINUTES, mins).apply()
        }
}
