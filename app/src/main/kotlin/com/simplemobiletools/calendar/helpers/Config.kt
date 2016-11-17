package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.content.SharedPreferences
import com.simplemobiletools.calendar.Constants
import java.util.*

class Config(context: Context) {
    private val mPrefs: SharedPreferences

    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    init {
        mPrefs = context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE)
    }

    var isFirstRun: Boolean
        get() = mPrefs.getBoolean(Constants.IS_FIRST_RUN, true)
        set(firstRun) = mPrefs.edit().putBoolean(Constants.IS_FIRST_RUN, firstRun).apply()

    var isDarkTheme: Boolean
        get() = mPrefs.getBoolean(Constants.IS_DARK_THEME, false)
        set(isDarkTheme) = mPrefs.edit().putBoolean(Constants.IS_DARK_THEME, isDarkTheme).apply()

    var isSundayFirst: Boolean
        get() {
            val isSundayFirst = Calendar.getInstance(Locale.getDefault()).firstDayOfWeek == Calendar.SUNDAY
            return mPrefs.getBoolean(Constants.SUNDAY_FIRST, isSundayFirst)
        }
        set(sundayFirst) = mPrefs.edit().putBoolean(Constants.SUNDAY_FIRST, sundayFirst).apply()

    var displayWeekNumbers: Boolean
        get() = mPrefs.getBoolean(Constants.WEEK_NUMBERS, false)
        set(displayWeekNumbers) = mPrefs.edit().putBoolean(Constants.WEEK_NUMBERS, displayWeekNumbers).apply()

    var storedView: Int
        get() = mPrefs.getInt(Constants.VIEW, Constants.MONTHLY_VIEW)
        set(view) = mPrefs.edit().putInt(Constants.VIEW, view).apply()

    var defaultReminderType: Int
        get() = mPrefs.getInt(Constants.REMINDER_TYPE, Constants.REMINDER_AT_START)
        set(type) {
            var newType = type
            if (newType == Constants.REMINDER_CUSTOM && defaultReminderMinutes == 0)
                newType = Constants.REMINDER_AT_START

            mPrefs.edit().putInt(Constants.REMINDER_TYPE, newType).apply()
        }

    var defaultReminderMinutes: Int
        get() = mPrefs.getInt(Constants.REMINDER_MINUTES, 10)
        set(mins) {
            if (mins == 0)
                defaultReminderType = Constants.REMINDER_AT_START
            mPrefs.edit().putInt(Constants.REMINDER_MINUTES, mins).apply()
        }
}
