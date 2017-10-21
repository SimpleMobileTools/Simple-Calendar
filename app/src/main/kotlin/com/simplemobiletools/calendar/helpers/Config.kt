package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.media.RingtoneManager
import android.text.format.DateFormat
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.scheduleCalDAVSync
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

    var use24hourFormat: Boolean
        get() {
            val use24hourFormat = DateFormat.is24HourFormat(context)
            return prefs.getBoolean(USE_24_HOUR_FORMAT, use24hourFormat)
        }
        set(use24hourFormat) = prefs.edit().putBoolean(USE_24_HOUR_FORMAT, use24hourFormat).apply()

    var displayWeekNumbers: Boolean
        get() = prefs.getBoolean(WEEK_NUMBERS, false)
        set(displayWeekNumbers) = prefs.edit().putBoolean(WEEK_NUMBERS, displayWeekNumbers).apply()

    var startWeeklyAt: Int
        get() = prefs.getInt(START_WEEKLY_AT, 7)
        set(startWeeklyAt) = prefs.edit().putInt(START_WEEKLY_AT, startWeeklyAt).apply()

    var endWeeklyAt: Int
        get() = prefs.getInt(END_WEEKLY_AT, 23)
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

    var defaultReminderMinutes: Int
        get() = prefs.getInt(REMINDER_MINUTES, 10)
        set(mins) = prefs.edit().putInt(REMINDER_MINUTES, mins).apply()

    var snoozeDelay: Int
        get() = prefs.getInt(SNOOZE_DELAY, 10)
        set(snoozeDelay) = prefs.edit().putInt(SNOOZE_DELAY, snoozeDelay).apply()

    var displayPastEvents: Int
        get() = prefs.getInt(DISPLAY_PAST_EVENTS, 0)
        set(displayPastEvents) = prefs.edit().putInt(DISPLAY_PAST_EVENTS, displayPastEvents).apply()

    var displayEventTypes: Set<String>
        get() = prefs.getStringSet(DISPLAY_EVENT_TYPES, HashSet<String>())
        set(displayEventTypes) = prefs.edit().remove(DISPLAY_EVENT_TYPES).putStringSet(DISPLAY_EVENT_TYPES, displayEventTypes).apply()

    var fontSize: Int
        get() = prefs.getInt(FONT_SIZE, FONT_SIZE_MEDIUM)
        set(size) = prefs.edit().putInt(FONT_SIZE, size).apply()

    var googleSync: Boolean
        get() = prefs.getBoolean(GOOGLE_SYNC, false)
        set(googleSync) = prefs.edit().putBoolean(GOOGLE_SYNC, googleSync).apply()

    var caldavSync: Boolean
        get() = prefs.getBoolean(CALDAV_SYNC, false)
        set(caldavSync) {
            context.scheduleCalDAVSync(caldavSync)
            prefs.edit().putBoolean(CALDAV_SYNC, caldavSync).apply()
        }

    var caldavSyncedCalendarIDs: String
        get() = prefs.getString(CALDAV_SYNCED_CALENDAR_IDS, "")
        set(calendarIDs) = prefs.edit().putString(CALDAV_SYNCED_CALENDAR_IDS, calendarIDs).apply()

    var lastUsedCaldavCalendar: Int
        get() = prefs.getInt(LAST_USED_CALDAV_CALENDAR, getSyncedCalendarIdsAsList().first().toInt())
        set(calendarId) = prefs.edit().putInt(LAST_USED_CALDAV_CALENDAR, calendarId).apply()

    var replaceDescription: Boolean
        get() = prefs.getBoolean(REPLACE_DESCRIPTION, false)
        set(replaceDescription) = prefs.edit().putBoolean(REPLACE_DESCRIPTION, replaceDescription).apply()

    fun getSyncedCalendarIdsAsList() = caldavSyncedCalendarIDs.split(",").filter { it.trim().isNotEmpty() } as ArrayList<String>

    fun addDisplayEventType(type: String) {
        addDisplayEventTypes(HashSet<String>(Arrays.asList(type)))
    }

    fun addDisplayEventTypes(types: Set<String>) {
        val currDisplayEventTypes = HashSet<String>(displayEventTypes)
        currDisplayEventTypes.addAll(types)
        displayEventTypes = currDisplayEventTypes
    }

    fun removeDisplayEventTypes(types: Set<String>) {
        val currDisplayEventTypes = HashSet<String>(displayEventTypes)
        currDisplayEventTypes.removeAll(types)
        displayEventTypes = currDisplayEventTypes
    }

    fun getDefaultNotificationSound(): String {
        try {
            return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)?.toString() ?: ""
        } catch (e: Exception) {
            return ""
        }
    }

    fun getFontSize() = when (fontSize) {
        FONT_SIZE_SMALL -> getSmallFontSize()
        FONT_SIZE_MEDIUM -> getMediumFontSize()
        else -> getLargeFontSize()
    }

    fun getSmallFontSize() = getMediumFontSize() - 3f
    fun getMediumFontSize() = context.resources.getDimension(R.dimen.day_text_size) / context.resources.displayMetrics.density
    fun getLargeFontSize() = getMediumFontSize() + 3f
}
