package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.scheduleCalDAVSync
import com.simplemobiletools.commons.extensions.getDefaultAlarmTitle
import com.simplemobiletools.commons.helpers.BaseConfig
import com.simplemobiletools.commons.helpers.DAY_MINUTES
import com.simplemobiletools.commons.helpers.YEAR_SECONDS
import java.util.*

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var showWeekNumbers: Boolean
        get() = prefs.getBoolean(WEEK_NUMBERS, false)
        set(showWeekNumbers) = prefs.edit().putBoolean(WEEK_NUMBERS, showWeekNumbers).apply()

    var startWeeklyAt: Int
        get() = prefs.getInt(START_WEEKLY_AT, 7)
        set(startWeeklyAt) = prefs.edit().putInt(START_WEEKLY_AT, startWeeklyAt).apply()

    var startWeekWithCurrentDay: Boolean
        get() = prefs.getBoolean(START_WEEK_WITH_CURRENT_DAY, false)
        set(startWeekWithCurrentDay) = prefs.edit().putBoolean(START_WEEK_WITH_CURRENT_DAY, startWeekWithCurrentDay).apply()

    var firstDayOfWeek: Int
        get() {
            val defaultFirstDayOfWeek = Calendar.getInstance(Locale.getDefault()).firstDayOfWeek
            return prefs.getInt(FIRST_DAY_OF_WEEK, getJodaDayOfWeekFromJava(defaultFirstDayOfWeek))
        }
        set(firstDayOfWeek) = prefs.edit().putInt(FIRST_DAY_OF_WEEK, firstDayOfWeek).apply()

    var showMidnightSpanningEventsAtTop: Boolean
        get() = prefs.getBoolean(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, true)
        set(midnightSpanning) = prefs.edit().putBoolean(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, midnightSpanning).apply()

    var allowCustomizeDayCount: Boolean
        get() = prefs.getBoolean(ALLOW_CUSTOMIZE_DAY_COUNT, true)
        set(allow) = prefs.edit().putBoolean(ALLOW_CUSTOMIZE_DAY_COUNT, allow).apply()

    var vibrateOnReminder: Boolean
        get() = prefs.getBoolean(VIBRATE, false)
        set(vibrate) = prefs.edit().putBoolean(VIBRATE, vibrate).apply()

    var reminderSoundUri: String
        get() = prefs.getString(REMINDER_SOUND_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())!!
        set(reminderSoundUri) = prefs.edit().putString(REMINDER_SOUND_URI, reminderSoundUri).apply()

    var reminderSoundTitle: String
        get() = prefs.getString(REMINDER_SOUND_TITLE, context.getDefaultAlarmTitle(RingtoneManager.TYPE_NOTIFICATION))!!
        set(reminderSoundTitle) = prefs.edit().putString(REMINDER_SOUND_TITLE, reminderSoundTitle).apply()

    var lastSoundUri: String
        get() = prefs.getString(LAST_SOUND_URI, "")!!
        set(lastSoundUri) = prefs.edit().putString(LAST_SOUND_URI, lastSoundUri).apply()

    var lastReminderChannel: Long
        get() = prefs.getLong(LAST_REMINDER_CHANNEL_ID, 0L)
        set(lastReminderChannel) = prefs.edit().putLong(LAST_REMINDER_CHANNEL_ID, lastReminderChannel).apply()

    var storedView: Int
        get() = prefs.getInt(VIEW, MONTHLY_VIEW)
        set(view) = prefs.edit().putInt(VIEW, view).apply()

    var lastEventReminderMinutes1: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES, 10)
        set(lastEventReminderMinutes) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES, lastEventReminderMinutes).apply()

    var lastEventReminderMinutes2: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_2, REMINDER_OFF)
        set(lastEventReminderMinutes2) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES_2, lastEventReminderMinutes2).apply()

    var lastEventReminderMinutes3: Int
        get() = prefs.getInt(LAST_EVENT_REMINDER_MINUTES_3, REMINDER_OFF)
        set(lastEventReminderMinutes3) = prefs.edit().putInt(LAST_EVENT_REMINDER_MINUTES_3, lastEventReminderMinutes3).apply()

    var displayPastEvents: Int
        get() = prefs.getInt(DISPLAY_PAST_EVENTS, DAY_MINUTES)
        set(displayPastEvents) = prefs.edit().putInt(DISPLAY_PAST_EVENTS, displayPastEvents).apply()

    var displayEventTypes: Set<String>
        get() = prefs.getStringSet(DISPLAY_EVENT_TYPES, HashSet())!!
        set(displayEventTypes) = prefs.edit().remove(DISPLAY_EVENT_TYPES).putStringSet(DISPLAY_EVENT_TYPES, displayEventTypes).apply()

    var quickFilterEventTypes: Set<String>
        get() = prefs.getStringSet(QUICK_FILTER_EVENT_TYPES, HashSet())!!
        set(quickFilterEventTypes) = prefs.edit().remove(QUICK_FILTER_EVENT_TYPES).putStringSet(QUICK_FILTER_EVENT_TYPES, quickFilterEventTypes).apply()

    fun addQuickFilterEventType(type: String) {
        val currQuickFilterEventTypes = HashSet(quickFilterEventTypes)
        currQuickFilterEventTypes.add(type)
        quickFilterEventTypes = currQuickFilterEventTypes
    }

    var listWidgetViewToOpen: Int
        get() = prefs.getInt(LIST_WIDGET_VIEW_TO_OPEN, DAILY_VIEW)
        set(viewToOpenFromListWidget) = prefs.edit().putInt(LIST_WIDGET_VIEW_TO_OPEN, viewToOpenFromListWidget).apply()

    var caldavSync: Boolean
        get() = prefs.getBoolean(CALDAV_SYNC, false)
        set(caldavSync) {
            context.scheduleCalDAVSync(caldavSync)
            prefs.edit().putBoolean(CALDAV_SYNC, caldavSync).apply()
        }

    var caldavSyncedCalendarIds: String
        get() = prefs.getString(CALDAV_SYNCED_CALENDAR_IDS, "")!!
        set(calendarIDs) = prefs.edit().putString(CALDAV_SYNCED_CALENDAR_IDS, calendarIDs).apply()

    var lastUsedCaldavCalendarId: Int
        get() = prefs.getInt(LAST_USED_CALDAV_CALENDAR, getSyncedCalendarIdsAsList().first().toInt())
        set(calendarId) = prefs.edit().putInt(LAST_USED_CALDAV_CALENDAR, calendarId).apply()

    var lastUsedLocalEventTypeId: Long
        get() = prefs.getLong(LAST_USED_LOCAL_EVENT_TYPE_ID, REGULAR_EVENT_TYPE_ID)
        set(lastUsedLocalEventTypeId) = prefs.edit().putLong(LAST_USED_LOCAL_EVENT_TYPE_ID, lastUsedLocalEventTypeId).apply()

    var lastUsedIgnoreEventTypesState: Boolean
        get() = prefs.getBoolean(LAST_USED_IGNORE_EVENT_TYPES_STATE, false)
        set(lastUsedIgnoreEventTypesState) = prefs.edit().putBoolean(LAST_USED_IGNORE_EVENT_TYPES_STATE, lastUsedIgnoreEventTypesState).apply()

    var reminderAudioStream: Int
        get() = prefs.getInt(REMINDER_AUDIO_STREAM, AudioManager.STREAM_NOTIFICATION)
        set(reminderAudioStream) = prefs.edit().putInt(REMINDER_AUDIO_STREAM, reminderAudioStream).apply()

    var replaceDescription: Boolean
        get() = prefs.getBoolean(REPLACE_DESCRIPTION, false)
        set(replaceDescription) = prefs.edit().putBoolean(REPLACE_DESCRIPTION, replaceDescription).apply()

    var displayDescription: Boolean
        get() = prefs.getBoolean(DISPLAY_DESCRIPTION, true)
        set(displayDescription) = prefs.edit().putBoolean(DISPLAY_DESCRIPTION, displayDescription).apply()

    var showGrid: Boolean
        get() = prefs.getBoolean(SHOW_GRID, false)
        set(showGrid) = prefs.edit().putBoolean(SHOW_GRID, showGrid).apply()

    var loopReminders: Boolean
        get() = prefs.getBoolean(LOOP_REMINDERS, false)
        set(loopReminders) = prefs.edit().putBoolean(LOOP_REMINDERS, loopReminders).apply()

    var dimPastEvents: Boolean
        get() = prefs.getBoolean(DIM_PAST_EVENTS, true)
        set(dimPastEvents) = prefs.edit().putBoolean(DIM_PAST_EVENTS, dimPastEvents).apply()

    var dimCompletedTasks: Boolean
        get() = prefs.getBoolean(DIM_COMPLETED_TASKS, true)
        set(dimCompletedTasks) = prefs.edit().putBoolean(DIM_COMPLETED_TASKS, dimCompletedTasks).apply()

    fun getSyncedCalendarIdsAsList() =
        caldavSyncedCalendarIds.split(",").filter { it.trim().isNotEmpty() }.map { Integer.parseInt(it) }.toMutableList() as ArrayList<Int>

    fun getDisplayEventTypessAsList() = displayEventTypes.map { it.toLong() }.toMutableList() as ArrayList<Long>

    fun addDisplayEventType(type: String) {
        addDisplayEventTypes(HashSet(Arrays.asList(type)))
    }

    private fun addDisplayEventTypes(types: Set<String>) {
        val currDisplayEventTypes = HashSet(displayEventTypes)
        currDisplayEventTypes.addAll(types)
        displayEventTypes = currDisplayEventTypes
    }

    fun removeDisplayEventTypes(types: Set<String>) {
        val currDisplayEventTypes = HashSet(displayEventTypes)
        currDisplayEventTypes.removeAll(types)
        displayEventTypes = currDisplayEventTypes
    }

    var usePreviousEventReminders: Boolean
        get() = prefs.getBoolean(USE_PREVIOUS_EVENT_REMINDERS, true)
        set(usePreviousEventReminders) = prefs.edit().putBoolean(USE_PREVIOUS_EVENT_REMINDERS, usePreviousEventReminders).apply()

    var defaultReminder1: Int
        get() = prefs.getInt(DEFAULT_REMINDER_1, 10)
        set(defaultReminder1) = prefs.edit().putInt(DEFAULT_REMINDER_1, defaultReminder1).apply()

    var defaultReminder2: Int
        get() = prefs.getInt(DEFAULT_REMINDER_2, REMINDER_OFF)
        set(defaultReminder2) = prefs.edit().putInt(DEFAULT_REMINDER_2, defaultReminder2).apply()

    var defaultReminder3: Int
        get() = prefs.getInt(DEFAULT_REMINDER_3, REMINDER_OFF)
        set(defaultReminder3) = prefs.edit().putInt(DEFAULT_REMINDER_3, defaultReminder3).apply()

    var pullToRefresh: Boolean
        get() = prefs.getBoolean(PULL_TO_REFRESH, false)
        set(pullToRefresh) = prefs.edit().putBoolean(PULL_TO_REFRESH, pullToRefresh).apply()

    var lastVibrateOnReminder: Boolean
        get() = prefs.getBoolean(LAST_VIBRATE_ON_REMINDER, context.config.vibrateOnReminder)
        set(lastVibrateOnReminder) = prefs.edit().putBoolean(LAST_VIBRATE_ON_REMINDER, lastVibrateOnReminder).apply()

    var defaultStartTime: Int
        get() = prefs.getInt(DEFAULT_START_TIME, DEFAULT_START_TIME_NEXT_FULL_HOUR)
        set(defaultStartTime) = prefs.edit().putInt(DEFAULT_START_TIME, defaultStartTime).apply()

    var defaultDuration: Int
        get() = prefs.getInt(DEFAULT_DURATION, 0)
        set(defaultDuration) = prefs.edit().putInt(DEFAULT_DURATION, defaultDuration).apply()

    var defaultEventTypeId: Long
        get() = prefs.getLong(DEFAULT_EVENT_TYPE_ID, -1L)
        set(defaultEventTypeId) = prefs.edit().putLong(DEFAULT_EVENT_TYPE_ID, defaultEventTypeId).apply()

    var allowChangingTimeZones: Boolean
        get() = prefs.getBoolean(ALLOW_CHANGING_TIME_ZONES, false)
        set(allowChangingTimeZones) = prefs.edit().putBoolean(ALLOW_CHANGING_TIME_ZONES, allowChangingTimeZones).apply()

    var addBirthdaysAutomatically: Boolean
        get() = prefs.getBoolean(ADD_BIRTHDAYS_AUTOMATICALLY, false)
        set(addBirthdaysAutomatically) = prefs.edit().putBoolean(ADD_BIRTHDAYS_AUTOMATICALLY, addBirthdaysAutomatically).apply()

    var addAnniversariesAutomatically: Boolean
        get() = prefs.getBoolean(ADD_ANNIVERSARIES_AUTOMATICALLY, false)
        set(addAnniversariesAutomatically) = prefs.edit().putBoolean(ADD_ANNIVERSARIES_AUTOMATICALLY, addAnniversariesAutomatically).apply()

    var birthdayReminders: ArrayList<Int>
        get() = prefs.getString(BIRTHDAY_REMINDERS, REMINDER_DEFAULT_VALUE)!!.split(",").map { it.toInt() }.toMutableList() as ArrayList<Int>
        set(birthdayReminders) = prefs.edit().putString(BIRTHDAY_REMINDERS, birthdayReminders.joinToString(",")).apply()

    var anniversaryReminders: ArrayList<Int>
        get() = prefs.getString(ANNIVERSARY_REMINDERS, REMINDER_DEFAULT_VALUE)!!.split(",").map { it.toInt() }.toMutableList() as ArrayList<Int>
        set(anniversaryReminders) = prefs.edit().putString(ANNIVERSARY_REMINDERS, anniversaryReminders.joinToString(",")).apply()

    var exportEvents: Boolean
        get() = prefs.getBoolean(EXPORT_EVENTS, true)
        set(exportEvents) = prefs.edit().putBoolean(EXPORT_EVENTS, exportEvents).apply()

    var exportTasks: Boolean
        get() = prefs.getBoolean(EXPORT_TASKS, true)
        set(exportTasks) = prefs.edit().putBoolean(EXPORT_TASKS, exportTasks).apply()

    var exportPastEntries: Boolean
        get() = prefs.getBoolean(EXPORT_PAST_EVENTS, true)
        set(exportPastEvents) = prefs.edit().putBoolean(EXPORT_PAST_EVENTS, exportPastEvents).apply()

    var weeklyViewItemHeightMultiplier: Float
        get() = prefs.getFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, 1f)
        set(weeklyViewItemHeightMultiplier) = prefs.edit().putFloat(WEEKLY_VIEW_ITEM_HEIGHT_MULTIPLIER, weeklyViewItemHeightMultiplier).apply()

    var weeklyViewDays: Int
        get() = prefs.getInt(WEEKLY_VIEW_DAYS, 7)
        set(weeklyViewDays) = prefs.edit().putInt(WEEKLY_VIEW_DAYS, weeklyViewDays).apply()

    var highlightWeekends: Boolean
        get() = prefs.getBoolean(HIGHLIGHT_WEEKENDS, false)
        set(highlightWeekends) = prefs.edit().putBoolean(HIGHLIGHT_WEEKENDS, highlightWeekends).apply()

    var highlightWeekendsColor: Int
        get() = prefs.getInt(HIGHLIGHT_WEEKENDS_COLOR, context.resources.getColor(R.color.red_text))
        set(highlightWeekendsColor) = prefs.edit().putInt(HIGHLIGHT_WEEKENDS_COLOR, highlightWeekendsColor).apply()

    var lastUsedEventSpan: Int
        get() = prefs.getInt(LAST_USED_EVENT_SPAN, YEAR_SECONDS)
        set(lastUsedEventSpan) = prefs.edit().putInt(LAST_USED_EVENT_SPAN, lastUsedEventSpan).apply()

    var allowCreatingTasks: Boolean
        get() = prefs.getBoolean(ALLOW_CREATING_TASKS, true)
        set(allowCreatingTasks) = prefs.edit().putBoolean(ALLOW_CREATING_TASKS, allowCreatingTasks).apply()

    var wasFilteredOutWarningShown: Boolean
        get() = prefs.getBoolean(WAS_FILTERED_OUT_WARNING_SHOWN, false)
        set(wasFilteredOutWarningShown) = prefs.edit().putBoolean(WAS_FILTERED_OUT_WARNING_SHOWN, wasFilteredOutWarningShown).apply()

    var autoBackupEventTypes: Set<String>
        get() = prefs.getStringSet(AUTO_BACKUP_EVENT_TYPES, HashSet())!!
        set(autoBackupEventTypes) = prefs.edit().remove(AUTO_BACKUP_EVENT_TYPES).putStringSet(AUTO_BACKUP_EVENT_TYPES, autoBackupEventTypes).apply()

    var autoBackupEvents: Boolean
        get() = prefs.getBoolean(AUTO_BACKUP_EVENTS, true)
        set(autoBackupEvents) = prefs.edit().putBoolean(AUTO_BACKUP_EVENTS, autoBackupEvents).apply()

    var autoBackupTasks: Boolean
        get() = prefs.getBoolean(AUTO_BACKUP_TASKS, true)
        set(autoBackupTasks) = prefs.edit().putBoolean(AUTO_BACKUP_TASKS, autoBackupTasks).apply()

    var autoBackupPastEntries: Boolean
        get() = prefs.getBoolean(AUTO_BACKUP_PAST_ENTRIES, true)
        set(autoBackupPastEntries) = prefs.edit().putBoolean(AUTO_BACKUP_PAST_ENTRIES, autoBackupPastEntries).apply()

}
