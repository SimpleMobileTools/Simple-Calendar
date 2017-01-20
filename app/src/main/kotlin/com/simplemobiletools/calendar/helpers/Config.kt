package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.media.RingtoneManager
import com.simplemobiletools.commons.helpers.BaseConfig
import java.util.*

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var isSundayFirst: Boolean
        get() {
            val isSundayFirst = Calendar.getInstance(Locale.getDefault()).firstDayOfWeek == Calendar.SUNDAY
            return prefs.getBoolean(SUNDAY_FIRST, isSundayFirst)
        }
        set(sundayFirst) = prefs.edit().putBoolean(SUNDAY_FIRST, sundayFirst).apply()

    var displayWeekNumbers: Boolean
        get() = prefs.getBoolean(WEEK_NUMBERS, false)
        set(displayWeekNumbers) = prefs.edit().putBoolean(WEEK_NUMBERS, displayWeekNumbers).apply()

    var startWeeklyAt: Int
        get() = prefs.getInt(START_WEEKLY_AT, 7)
        set(startWeeklyAt) = prefs.edit().putInt(START_WEEKLY_AT, startWeeklyAt).apply()

    var endWeeklyAt: Int
        get() = prefs.getInt(END_WEEKLY_AT, 24)
        set(endWeeklyAt) = prefs.edit().putInt(END_WEEKLY_AT, endWeeklyAt).apply()

    var vibrateOnReminder: Boolean
        get() = prefs.getBoolean(VIBRATE, false)
        set(vibrate) = prefs.edit().putBoolean(VIBRATE, vibrate).apply()

    var reminderSound: String
        get() = prefs.getString(REMINDER_SOUND, getDefaultNotificationSound())
        set(path) = prefs.edit().putString(REMINDER_SOUND, path).apply()

    var storedView: Int
        get() = prefs.getInt(VIEW, MONTHLY_VIEW)
        set(view) = prefs.edit().putInt(VIEW, view).apply()

    var defaultReminderType: Int
        get() = prefs.getInt(REMINDER_TYPE, REMINDER_AT_START)
        set(type) {
            var newType = type
            if (newType == REMINDER_CUSTOM && defaultReminderMinutes == 0)
                newType = REMINDER_AT_START

            prefs.edit().putInt(REMINDER_TYPE, newType).apply()
        }

    var defaultReminderMinutes: Int
        get() = prefs.getInt(REMINDER_MINUTES, 10)
        set(mins) {
            if (mins == 0)
                defaultReminderType = REMINDER_AT_START
            prefs.edit().putInt(REMINDER_MINUTES, mins).apply()
        }

    fun getDefaultNotificationSound(): String {
        try {
            return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)?.toString() ?: ""
        } catch (e: Exception) {
            return ""
        }
    }
}
