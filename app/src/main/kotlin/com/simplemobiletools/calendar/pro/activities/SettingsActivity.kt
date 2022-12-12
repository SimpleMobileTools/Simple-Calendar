package com.simplemobiletools.calendar.pro.activities

import android.app.Activity
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Bundle
import android.widget.Toast
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.dialogs.SelectCalendarsDialog
import com.simplemobiletools.calendar.pro.dialogs.SelectEventTypeDialog
import com.simplemobiletools.calendar.pro.dialogs.SelectQuickFilterEventTypesDialog
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.calendar.pro.views.MonthView
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.AlarmSound
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.activity_settings.*
import org.joda.time.DateTime
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val GET_RINGTONE_URI = 1
    private val PICK_IMPORT_SOURCE_INTENT = 2

    private var mStoredPrimaryColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        mStoredPrimaryColor = getProperPrimaryColor()
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(settings_toolbar, NavigationIcon.Arrow)
        setupSettingItems()
    }

    private fun setupSettingItems() {
        setupCustomizeColors()
        setupCustomizeNotifications()
        setupUseEnglish()
        setupLanguage()
        setupManageEventTypes()
        setupManageQuickFilterEventTypes()
        setupHourFormat()
        setupAllowCreatingTasks()
        setupSundayFirst()
        setupHighlightWeekends()
        setupHighlightWeekendsColor()
        setupDeleteAllEvents()
        setupDisplayDescription()
        setupReplaceDescription()
        setupWeekNumbers()
        setupShowGrid()
        setupWeeklyStart()
        setupMidnightSpanEvents()
        setupAllowCustomizeDayCount()
        setupStartWeekWithCurrentDay()
        setupVibrate()
        setupReminderSound()
        setupReminderAudioStream()
        setupUseSameSnooze()
        setupLoopReminders()
        setupSnoozeTime()
        setupCaldavSync()
        setupManageSyncedCalendars()
        setupDefaultStartTime()
        setupDefaultDuration()
        setupDefaultEventType()
        setupPullToRefresh()
        setupDefaultReminder()
        setupDefaultReminder1()
        setupDefaultReminder2()
        setupDefaultReminder3()
        setupDisplayPastEvents()
        setupFontSize()
        setupMonthFontSize()
        setupCustomizeWidgetColors()
        setupViewToOpenFromListWidget()
        setupDimEvents()
        setupDimCompletedTasks()
        setupAllowChangingTimeZones()
        updateTextColors(settings_holder)
        checkPrimaryColor()
        setupExportSettings()
        setupImportSettings()

        arrayOf(
            settings_color_customization_label,
            settings_general_settings_label,
            settings_reminders_label,
            settings_caldav_label,
            settings_new_events_label,
            settings_weekly_view_label,
            settings_monthly_view_label,
            settings_event_lists_label,
            settings_widgets_label,
            settings_events_label,
            settings_tasks_label,
            settings_migrating_label
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }

        arrayOf(
            settings_color_customization_holder,
            settings_general_settings_holder,
            settings_reminders_holder,
            settings_caldav_holder,
            settings_new_events_holder,
            settings_weekly_view_holder,
            settings_monthly_view_holder,
            settings_event_lists_holder,
            settings_widgets_holder,
            settings_events_holder,
            settings_tasks_holder,
            settings_migrating_holder
        ).forEach {
            it.background.applyColorFilter(getProperBackgroundColor().getContrastColor())
        }
    }

    override fun onPause() {
        super.onPause()
        mStoredPrimaryColor = getProperPrimaryColor()
    }

    override fun onStop() {
        super.onStop()
        val reminders = sortedSetOf(config.defaultReminder1, config.defaultReminder2, config.defaultReminder3).filter { it != REMINDER_OFF }
        config.defaultReminder1 = reminders.getOrElse(0) { REMINDER_OFF }
        config.defaultReminder2 = reminders.getOrElse(1) { REMINDER_OFF }
        config.defaultReminder3 = reminders.getOrElse(2) { REMINDER_OFF }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == GET_RINGTONE_URI && resultCode == RESULT_OK && resultData != null) {
            val newAlarmSound = storeNewYourAlarmSound(resultData)
            updateReminderSound(newAlarmSound)
        } else if (requestCode == PICK_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            parseFile(inputStream)
        }
    }

    private fun checkPrimaryColor() {
        if (getProperPrimaryColor() != mStoredPrimaryColor) {
            ensureBackgroundThread {
                val eventTypes = eventsHelper.getEventTypesSync()
                if (eventTypes.filter { it.caldavCalendarId == 0 }.size == 1) {
                    val eventType = eventTypes.first { it.caldavCalendarId == 0 }
                    eventType.color = getProperPrimaryColor()
                    eventsHelper.insertOrUpdateEventTypeSync(eventType)
                }
            }
        }
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupCustomizeNotifications() {
        settings_customize_notifications_holder.beVisibleIf(isOreoPlus())

        if (settings_customize_notifications_holder.isGone()) {
            settings_reminder_sound_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        settings_customize_notifications_holder.setOnClickListener {
            launchCustomizeNotificationsIntent()
        }
    }

    private fun setupUseEnglish() {
        settings_use_english_holder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settings_use_english.isChecked = config.useEnglish
        settings_use_english_holder.setOnClickListener {
            settings_use_english.toggle()
            config.useEnglish = settings_use_english.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() {
        settings_language.text = Locale.getDefault().displayLanguage
        settings_language_holder.beVisibleIf(isTiramisuPlus())

        if (settings_use_english_holder.isGone() && settings_language_holder.isGone()) {
            settings_manage_event_types_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
        }

        settings_language_holder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupManageEventTypes() {
        settings_manage_event_types_holder.setOnClickListener {
            startActivity(Intent(this, ManageEventTypesActivity::class.java))
        }
    }

    private fun setupManageQuickFilterEventTypes() {
        settings_manage_quick_filter_event_types_holder.setOnClickListener {
            showQuickFilterPicker()
        }

        eventsHelper.getEventTypes(this, false) {
            settings_manage_quick_filter_event_types_holder.beGoneIf(it.size < 2)
        }
    }

    private fun setupHourFormat() {
        settings_hour_format.isChecked = config.use24HourFormat
        settings_hour_format_holder.setOnClickListener {
            settings_hour_format.toggle()
            config.use24HourFormat = settings_hour_format.isChecked
        }
    }

    private fun setupAllowCreatingTasks() {
        settings_allow_creating_tasks.isChecked = config.allowCreatingTasks
        settings_allow_creating_tasks_holder.setOnClickListener {
            settings_allow_creating_tasks.toggle()
            config.allowCreatingTasks = settings_allow_creating_tasks.isChecked
        }
    }

    private fun setupCaldavSync() {
        settings_caldav_sync.isChecked = config.caldavSync
        checkCalDAVBackgrounds()
        settings_caldav_sync_holder.setOnClickListener {
            if (config.caldavSync) {
                toggleCaldavSync(false)
            } else {
                handlePermission(PERMISSION_WRITE_CALENDAR) {
                    if (it) {
                        handlePermission(PERMISSION_READ_CALENDAR) {
                            if (it) {
                                handleNotificationPermission { granted ->
                                    if (granted) {
                                        toggleCaldavSync(true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupPullToRefresh() {
        settings_caldav_pull_to_refresh_holder.beVisibleIf(config.caldavSync)
        settings_caldav_pull_to_refresh.isChecked = config.pullToRefresh
        checkCalDAVBackgrounds()
        settings_caldav_pull_to_refresh_holder.setOnClickListener {
            settings_caldav_pull_to_refresh.toggle()
            config.pullToRefresh = settings_caldav_pull_to_refresh.isChecked
        }
    }

    private fun checkCalDAVBackgrounds() {
        if (config.caldavSync) {
            settings_caldav_sync_holder.background = resources.getDrawable(R.drawable.ripple_top_corners, theme)
            settings_manage_synced_calendars_holder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        } else {
            settings_caldav_sync_holder.background = resources.getDrawable(R.drawable.ripple_all_corners, theme)
        }
    }

    private fun setupManageSyncedCalendars() {
        settings_manage_synced_calendars_holder.beVisibleIf(config.caldavSync)
        settings_manage_synced_calendars_holder.setOnClickListener {
            showCalendarPicker()
        }
    }

    private fun toggleCaldavSync(enable: Boolean) {
        if (enable) {
            showCalendarPicker()
        } else {
            settings_caldav_sync.isChecked = false
            config.caldavSync = false
            settings_manage_synced_calendars_holder.beGone()
            settings_caldav_pull_to_refresh_holder.beGone()

            ensureBackgroundThread {
                config.getSyncedCalendarIdsAsList().forEach {
                    calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                }
                eventTypesDB.deleteEventTypesWithCalendarId(config.getSyncedCalendarIdsAsList())
                updateDefaultEventTypeText()
            }
        }
        checkCalDAVBackgrounds()
    }

    private fun showCalendarPicker() {
        val oldCalendarIds = config.getSyncedCalendarIdsAsList()

        SelectCalendarsDialog(this) {
            val newCalendarIds = config.getSyncedCalendarIdsAsList()
            if (newCalendarIds.isEmpty() && !config.caldavSync) {
                return@SelectCalendarsDialog
            }

            settings_manage_synced_calendars_holder.beVisibleIf(newCalendarIds.isNotEmpty())
            settings_caldav_pull_to_refresh_holder.beVisibleIf(newCalendarIds.isNotEmpty())
            settings_caldav_sync.isChecked = newCalendarIds.isNotEmpty()
            config.caldavSync = newCalendarIds.isNotEmpty()
            if (settings_caldav_sync.isChecked) {
                toast(R.string.syncing)
            }

            ensureBackgroundThread {
                if (newCalendarIds.isNotEmpty()) {
                    val existingEventTypeNames = eventsHelper.getEventTypesSync().map {
                        it.getDisplayTitle().lowercase(Locale.getDefault())
                    } as ArrayList<String>

                    getSyncedCalDAVCalendars().forEach {
                        val calendarTitle = it.getFullTitle()
                        if (!existingEventTypeNames.contains(calendarTitle.lowercase(Locale.getDefault()))) {
                            val eventType = EventType(null, it.displayName, it.color, it.id, it.displayName, it.accountName)
                            existingEventTypeNames.add(calendarTitle.lowercase(Locale.getDefault()))
                            eventsHelper.insertOrUpdateEventType(this, eventType)
                        }
                    }

                    syncCalDAVCalendars {
                        calDAVHelper.refreshCalendars(showToasts = true, scheduleNextSync = true) {
                            if (settings_caldav_sync.isChecked) {
                                toast(R.string.synchronization_completed)
                            }
                        }
                    }
                }

                val removedCalendarIds = oldCalendarIds.filter { !newCalendarIds.contains(it) }
                removedCalendarIds.forEach {
                    calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                    eventsHelper.getEventTypeWithCalDAVCalendarId(it)?.apply {
                        eventsHelper.deleteEventTypes(arrayListOf(this), true)
                    }
                }

                eventTypesDB.deleteEventTypesWithCalendarId(removedCalendarIds)
                updateDefaultEventTypeText()
            }
        }
    }

    private fun showQuickFilterPicker() {
        SelectQuickFilterEventTypesDialog(this)
    }

    private fun setupSundayFirst() {
        settings_sunday_first.isChecked = config.isSundayFirst
        settings_sunday_first_holder.setOnClickListener {
            settings_sunday_first.toggle()
            config.isSundayFirst = settings_sunday_first.isChecked
        }
    }

    private fun setupHighlightWeekends() {
        settings_highlight_weekends.isChecked = config.highlightWeekends
        settings_highlight_weekends_color_holder.beVisibleIf(config.highlightWeekends)
        setupHighlightWeekendColorBackground()
        settings_highlight_weekends_holder.setOnClickListener {
            settings_highlight_weekends.toggle()
            config.highlightWeekends = settings_highlight_weekends.isChecked
            settings_highlight_weekends_color_holder.beVisibleIf(config.highlightWeekends)
            setupHighlightWeekendColorBackground()
        }
    }

    private fun setupHighlightWeekendsColor() {
        settings_highlight_weekends_color.setFillWithStroke(config.highlightWeekendsColor, getProperBackgroundColor())
        settings_highlight_weekends_color_holder.setOnClickListener {
            ColorPickerDialog(this, config.highlightWeekendsColor) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    config.highlightWeekendsColor = color
                    settings_highlight_weekends_color.setFillWithStroke(color, getProperBackgroundColor())
                }
            }
        }
    }

    private fun setupHighlightWeekendColorBackground() {
        if (settings_highlight_weekends_color_holder.isVisible()) {
            settings_highlight_weekends_holder.background = resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            settings_highlight_weekends_holder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun setupDeleteAllEvents() {
        settings_delete_all_events_holder.setOnClickListener {
            ConfirmationDialog(this, messageId = R.string.delete_all_events_confirmation) {
                eventsHelper.deleteAllEvents()
            }
        }
    }

    private fun setupDisplayDescription() {
        settings_display_description.isChecked = config.displayDescription
        setupDescriptionVisibility()
        settings_display_description_holder.setOnClickListener {
            settings_display_description.toggle()
            config.displayDescription = settings_display_description.isChecked
            setupDescriptionVisibility()
        }
    }

    private fun setupDescriptionVisibility() {
        settings_replace_description_holder.beVisibleIf(config.displayDescription)
        if (settings_replace_description_holder.isVisible()) {
            settings_display_description_holder.background = resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            settings_display_description_holder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun setupReplaceDescription() {
        settings_replace_description.isChecked = config.replaceDescription
        settings_replace_description_holder.setOnClickListener {
            settings_replace_description.toggle()
            config.replaceDescription = settings_replace_description.isChecked
        }
    }

    private fun setupWeeklyStart() {
        settings_start_weekly_at.text = getHoursString(config.startWeeklyAt)
        settings_start_weekly_at_holder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            (0..16).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(this@SettingsActivity, items, config.startWeeklyAt) {
                config.startWeeklyAt = it as Int
                settings_start_weekly_at.text = getHoursString(it)
            }
        }
    }

    private fun setupMidnightSpanEvents() {
        settings_midnight_span_event.isChecked = config.showMidnightSpanningEventsAtTop
        settings_midnight_span_events_holder.setOnClickListener {
            settings_midnight_span_event.toggle()
            config.showMidnightSpanningEventsAtTop = settings_midnight_span_event.isChecked
        }
    }

    private fun setupAllowCustomizeDayCount() {
        settings_allow_customize_day_count.isChecked = config.allowCustomizeDayCount
        settings_allow_customize_day_count_holder.setOnClickListener {
            settings_allow_customize_day_count.toggle()
            config.allowCustomizeDayCount = settings_allow_customize_day_count.isChecked
        }
    }

    private fun setupStartWeekWithCurrentDay() {
        settings_start_week_with_current_day.isChecked = config.startWeekWithCurrentDay
        settings_start_week_with_current_day_holder.setOnClickListener {
            settings_start_week_with_current_day.toggle()
            config.startWeekWithCurrentDay = settings_start_week_with_current_day.isChecked
        }
    }

    private fun setupWeekNumbers() {
        settings_week_numbers.isChecked = config.showWeekNumbers
        settings_week_numbers_holder.setOnClickListener {
            settings_week_numbers.toggle()
            config.showWeekNumbers = settings_week_numbers.isChecked
        }
    }

    private fun setupShowGrid() {
        settings_show_grid.isChecked = config.showGrid
        settings_show_grid_holder.setOnClickListener {
            settings_show_grid.toggle()
            config.showGrid = settings_show_grid.isChecked
        }
    }

    private fun setupReminderSound() {
        settings_reminder_sound_holder.beGoneIf(isOreoPlus())
        settings_reminder_sound.text = config.reminderSoundTitle

        settings_reminder_sound_holder.setOnClickListener {
            SelectAlarmSoundDialog(this, config.reminderSoundUri, config.reminderAudioStream, GET_RINGTONE_URI, RingtoneManager.TYPE_NOTIFICATION, false,
                onAlarmPicked = {
                    if (it != null) {
                        updateReminderSound(it)
                    }
                }, onAlarmSoundDeleted = {
                    if (it.uri == config.reminderSoundUri) {
                        val defaultAlarm = getDefaultAlarmSound(RingtoneManager.TYPE_NOTIFICATION)
                        updateReminderSound(defaultAlarm)
                    }
                })
        }
    }

    private fun updateReminderSound(alarmSound: AlarmSound) {
        config.reminderSoundTitle = alarmSound.title
        config.reminderSoundUri = alarmSound.uri
        settings_reminder_sound.text = alarmSound.title
    }

    private fun setupReminderAudioStream() {
        settings_reminder_audio_stream.text = getAudioStreamText()
        settings_reminder_audio_stream_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(AudioManager.STREAM_ALARM, getString(R.string.alarm_stream)),
                RadioItem(AudioManager.STREAM_SYSTEM, getString(R.string.system_stream)),
                RadioItem(AudioManager.STREAM_NOTIFICATION, getString(R.string.notification_stream)),
                RadioItem(AudioManager.STREAM_RING, getString(R.string.ring_stream))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.reminderAudioStream) {
                config.reminderAudioStream = it as Int
                settings_reminder_audio_stream.text = getAudioStreamText()
            }
        }
    }

    private fun getAudioStreamText() = getString(
        when (config.reminderAudioStream) {
            AudioManager.STREAM_ALARM -> R.string.alarm_stream
            AudioManager.STREAM_SYSTEM -> R.string.system_stream
            AudioManager.STREAM_NOTIFICATION -> R.string.notification_stream
            else -> R.string.ring_stream
        }
    )

    private fun setupVibrate() {
        settings_vibrate.isChecked = config.vibrateOnReminder
        settings_vibrate_holder.setOnClickListener {
            settings_vibrate.toggle()
            config.vibrateOnReminder = settings_vibrate.isChecked
        }
    }

    private fun setupLoopReminders() {
        settings_loop_reminders.isChecked = config.loopReminders
        settings_loop_reminders_holder.setOnClickListener {
            settings_loop_reminders.toggle()
            config.loopReminders = settings_loop_reminders.isChecked
        }
    }

    private fun setupUseSameSnooze() {
        settings_snooze_time_holder.beVisibleIf(config.useSameSnooze)
        settings_use_same_snooze.isChecked = config.useSameSnooze
        setupSnoozeBackgrounds()
        settings_use_same_snooze_holder.setOnClickListener {
            settings_use_same_snooze.toggle()
            config.useSameSnooze = settings_use_same_snooze.isChecked
            settings_snooze_time_holder.beVisibleIf(config.useSameSnooze)
            setupSnoozeBackgrounds()
        }
    }

    private fun setupSnoozeBackgrounds() {
        if (config.useSameSnooze) {
            settings_use_same_snooze_holder.background = resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            settings_use_same_snooze_holder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun setupSnoozeTime() {
        updateSnoozeTime()
        settings_snooze_time_holder.setOnClickListener {
            showPickSecondsDialogHelper(config.snoozeTime, true) {
                config.snoozeTime = it / 60
                updateSnoozeTime()
            }
        }
    }

    private fun updateSnoozeTime() {
        settings_snooze_time.text = formatMinutesToTimeString(config.snoozeTime)
    }

    private fun setupDefaultReminder() {
        settings_use_last_event_reminders.isChecked = config.usePreviousEventReminders
        toggleDefaultRemindersVisibility(!config.usePreviousEventReminders)
        settings_use_last_event_reminders_holder.setOnClickListener {
            settings_use_last_event_reminders.toggle()
            config.usePreviousEventReminders = settings_use_last_event_reminders.isChecked
            toggleDefaultRemindersVisibility(!settings_use_last_event_reminders.isChecked)
        }
    }

    private fun setupDefaultReminder1() {
        settings_default_reminder_1.text = getFormattedMinutes(config.defaultReminder1)
        settings_default_reminder_1_holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder1) {
                config.defaultReminder1 = if (it == -1 || it == 0) it else it / 60
                settings_default_reminder_1.text = getFormattedMinutes(config.defaultReminder1)
            }
        }
    }

    private fun setupDefaultReminder2() {
        settings_default_reminder_2.text = getFormattedMinutes(config.defaultReminder2)
        settings_default_reminder_2_holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder2) {
                config.defaultReminder2 = if (it == -1 || it == 0) it else it / 60
                settings_default_reminder_2.text = getFormattedMinutes(config.defaultReminder2)
            }
        }
    }

    private fun setupDefaultReminder3() {
        settings_default_reminder_3.text = getFormattedMinutes(config.defaultReminder3)
        settings_default_reminder_3_holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder3) {
                config.defaultReminder3 = if (it == -1 || it == 0) it else it / 60
                settings_default_reminder_3.text = getFormattedMinutes(config.defaultReminder3)
            }
        }
    }

    private fun toggleDefaultRemindersVisibility(show: Boolean) {
        arrayOf(settings_default_reminder_1_holder, settings_default_reminder_2_holder, settings_default_reminder_3_holder).forEach {
            it.beVisibleIf(show)
        }

        if (show) {
            settings_use_last_event_reminders_holder.background = resources.getDrawable(R.drawable.ripple_background, theme)
        } else {
            settings_use_last_event_reminders_holder.background = resources.getDrawable(R.drawable.ripple_bottom_corners, theme)
        }
    }

    private fun getHoursString(hours: Int) = String.format("%02d:00", hours)

    private fun setupDisplayPastEvents() {
        var displayPastEvents = config.displayPastEvents
        updatePastEventsText(displayPastEvents)
        settings_display_past_events_holder.setOnClickListener {
            CustomIntervalPickerDialog(this, displayPastEvents * 60) {
                val result = it / 60
                displayPastEvents = result
                config.displayPastEvents = result
                updatePastEventsText(result)
            }
        }
    }

    private fun updatePastEventsText(displayPastEvents: Int) {
        settings_display_past_events.text = getDisplayPastEventsText(displayPastEvents)
    }

    private fun getDisplayPastEventsText(displayPastEvents: Int): String {
        return if (displayPastEvents == 0) {
            getString(R.string.never)
        } else {
            getFormattedMinutes(displayPastEvents, false)
        }
    }

    // TODO:
    private fun setupMonthFontSize() {
        settings_month_font_size.text = getMonthFontSizeText()
        settings_month_font_size_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(0, getString(R.string.small)),
                RadioItem(1, getString(R.string.medium)),
                RadioItem(2, getString(R.string.large)),
                //RadioItem(3, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.monthFontSize) {
                config.monthFontSize = it as Int
                settings_month_font_size.text = getMonthFontSizeText()
            }
        }
    }

    private fun getMonthFontSizeText() = getString(
        when (config.monthFontSize) {
            FONT_SIZE_SMALL -> R.string.small
            FONT_SIZE_MEDIUM -> R.string.medium
            FONT_SIZE_LARGE -> R.string.large
            else -> R.string.large
        }
    )

    private fun setupFontSize() {
        settings_font_size.text = getFontSizeText()
        settings_font_size_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                settings_font_size.text = getFontSizeText()
                updateWidgets()
            }
        }
    }

    private fun setupCustomizeWidgetColors() {
        settings_customize_widget_colors_holder.setOnClickListener {
            Intent(this, WidgetListConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
    }

    private fun setupViewToOpenFromListWidget() {
        settings_list_widget_view_to_open.text = getDefaultViewText()
        settings_list_widget_view_to_open_holder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(DAILY_VIEW, getString(R.string.daily_view)),
                RadioItem(WEEKLY_VIEW, getString(R.string.weekly_view)),
                RadioItem(MONTHLY_VIEW, getString(R.string.monthly_view)),
                RadioItem(MONTHLY_DAILY_VIEW, getString(R.string.monthly_daily_view)),
                RadioItem(YEARLY_VIEW, getString(R.string.yearly_view)),
                RadioItem(EVENTS_LIST_VIEW, getString(R.string.simple_event_list)),
                RadioItem(LAST_VIEW, getString(R.string.last_view))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.listWidgetViewToOpen) {
                config.listWidgetViewToOpen = it as Int
                settings_list_widget_view_to_open.text = getDefaultViewText()
                updateWidgets()
            }
        }
    }

    private fun getDefaultViewText() = getString(
        when (config.listWidgetViewToOpen) {
            DAILY_VIEW -> R.string.daily_view
            WEEKLY_VIEW -> R.string.weekly_view
            MONTHLY_VIEW -> R.string.monthly_view
            MONTHLY_DAILY_VIEW -> R.string.monthly_daily_view
            YEARLY_VIEW -> R.string.yearly_view
            EVENTS_LIST_VIEW -> R.string.simple_event_list
            else -> R.string.last_view
        }
    )

    private fun setupDimEvents() {
        settings_dim_past_events.isChecked = config.dimPastEvents
        settings_dim_past_events_holder.setOnClickListener {
            settings_dim_past_events.toggle()
            config.dimPastEvents = settings_dim_past_events.isChecked
        }
    }

    private fun setupDimCompletedTasks() {
        settings_dim_completed_tasks.isChecked = config.dimCompletedTasks
        settings_dim_completed_tasks_holder.setOnClickListener {
            settings_dim_completed_tasks.toggle()
            config.dimCompletedTasks = settings_dim_completed_tasks.isChecked
        }
    }

    private fun setupAllowChangingTimeZones() {
        settings_allow_changing_time_zones.isChecked = config.allowChangingTimeZones
        settings_allow_changing_time_zones_holder.setOnClickListener {
            settings_allow_changing_time_zones.toggle()
            config.allowChangingTimeZones = settings_allow_changing_time_zones.isChecked
        }
    }

    private fun setupDefaultStartTime() {
        updateDefaultStartTimeText()
        settings_default_start_time_holder.setOnClickListener {
            val currentDefaultTime = when (config.defaultStartTime) {
                DEFAULT_START_TIME_NEXT_FULL_HOUR -> DEFAULT_START_TIME_NEXT_FULL_HOUR
                DEFAULT_START_TIME_CURRENT_TIME -> DEFAULT_START_TIME_CURRENT_TIME
                else -> 0
            }

            val items = ArrayList<RadioItem>()
            items.add(RadioItem(DEFAULT_START_TIME_CURRENT_TIME, getString(R.string.current_time)))
            items.add(RadioItem(DEFAULT_START_TIME_NEXT_FULL_HOUR, getString(R.string.next_full_hour)))
            items.add(RadioItem(0, getString(R.string.other_time)))

            RadioGroupDialog(this@SettingsActivity, items, currentDefaultTime) {
                if (it as Int == DEFAULT_START_TIME_NEXT_FULL_HOUR || it == DEFAULT_START_TIME_CURRENT_TIME) {
                    config.defaultStartTime = it
                    updateDefaultStartTimeText()
                } else {
                    val timeListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                        config.defaultStartTime = hourOfDay * 60 + minute
                        updateDefaultStartTimeText()
                    }

                    val currentDateTime = DateTime.now()
                    TimePickerDialog(
                        this,
                        getTimePickerDialogTheme(),
                        timeListener,
                        currentDateTime.hourOfDay,
                        currentDateTime.minuteOfHour,
                        config.use24HourFormat
                    ).show()
                }
            }
        }
    }

    private fun updateDefaultStartTimeText() {
        when (config.defaultStartTime) {
            DEFAULT_START_TIME_CURRENT_TIME -> settings_default_start_time.text = getString(R.string.current_time)
            DEFAULT_START_TIME_NEXT_FULL_HOUR -> settings_default_start_time.text = getString(R.string.next_full_hour)
            else -> {
                val hours = config.defaultStartTime / 60
                val minutes = config.defaultStartTime % 60
                settings_default_start_time.text = String.format("%02d:%02d", hours, minutes)
            }
        }
    }

    private fun setupDefaultDuration() {
        updateDefaultDurationText()
        settings_default_duration_holder.setOnClickListener {
            CustomIntervalPickerDialog(this, config.defaultDuration * 60) {
                val result = it / 60
                config.defaultDuration = result
                updateDefaultDurationText()
            }
        }
    }

    private fun updateDefaultDurationText() {
        val duration = config.defaultDuration
        settings_default_duration.text = if (duration == 0) {
            "0 ${getString(R.string.minutes_raw)}"
        } else {
            getFormattedMinutes(duration, false)
        }
    }

    private fun setupDefaultEventType() {
        updateDefaultEventTypeText()
        settings_default_event_type.text = getString(R.string.last_used_one)
        settings_default_event_type_holder.setOnClickListener {
            SelectEventTypeDialog(this, config.defaultEventTypeId, true, false, true, true, false) {
                config.defaultEventTypeId = it.id!!
                updateDefaultEventTypeText()
            }
        }
    }

    private fun updateDefaultEventTypeText() {
        if (config.defaultEventTypeId == -1L) {
            runOnUiThread {
                settings_default_event_type.text = getString(R.string.last_used_one)
            }
        } else {
            ensureBackgroundThread {
                val eventType = eventTypesDB.getEventTypeWithId(config.defaultEventTypeId)
                if (eventType != null) {
                    config.lastUsedCaldavCalendarId = eventType.caldavCalendarId
                    runOnUiThread {
                        settings_default_event_type.text = eventType.title
                    }
                } else {
                    config.defaultEventTypeId = -1
                    updateDefaultEventTypeText()
                }
            }
        }
    }

    private fun setupExportSettings() {
        settings_export_holder.setOnClickListener {
            val configItems = LinkedHashMap<String, Any>().apply {
                put(IS_USING_SHARED_THEME, config.isUsingSharedTheme)
                put(TEXT_COLOR, config.textColor)
                put(BACKGROUND_COLOR, config.backgroundColor)
                put(PRIMARY_COLOR, config.primaryColor)
                put(ACCENT_COLOR, config.accentColor)
                put(APP_ICON_COLOR, config.appIconColor)
                put(USE_ENGLISH, config.useEnglish)
                put(WAS_USE_ENGLISH_TOGGLED, config.wasUseEnglishToggled)
                put(WIDGET_BG_COLOR, config.widgetBgColor)
                put(WIDGET_TEXT_COLOR, config.widgetTextColor)
                put(WEEK_NUMBERS, config.showWeekNumbers)
                put(START_WEEKLY_AT, config.startWeeklyAt)
                put(VIBRATE, config.vibrateOnReminder)
                put(LAST_EVENT_REMINDER_MINUTES, config.lastEventReminderMinutes1)
                put(LAST_EVENT_REMINDER_MINUTES_2, config.lastEventReminderMinutes2)
                put(LAST_EVENT_REMINDER_MINUTES_3, config.lastEventReminderMinutes3)
                put(DISPLAY_PAST_EVENTS, config.displayPastEvents)
                put(FONT_SIZE, config.fontSize)
                put(MONTH_FONT_SIZE, config.monthFontSize)
                put(LIST_WIDGET_VIEW_TO_OPEN, config.listWidgetViewToOpen)
                put(REMINDER_AUDIO_STREAM, config.reminderAudioStream)
                put(DISPLAY_DESCRIPTION, config.displayDescription)
                put(REPLACE_DESCRIPTION, config.replaceDescription)
                put(SHOW_GRID, config.showGrid)
                put(LOOP_REMINDERS, config.loopReminders)
                put(DIM_PAST_EVENTS, config.dimPastEvents)
                put(DIM_COMPLETED_TASKS, config.dimCompletedTasks)
                put(ALLOW_CHANGING_TIME_ZONES, config.allowChangingTimeZones)
                put(USE_PREVIOUS_EVENT_REMINDERS, config.usePreviousEventReminders)
                put(DEFAULT_REMINDER_1, config.defaultReminder1)
                put(DEFAULT_REMINDER_2, config.defaultReminder2)
                put(DEFAULT_REMINDER_3, config.defaultReminder3)
                put(PULL_TO_REFRESH, config.pullToRefresh)
                put(DEFAULT_START_TIME, config.defaultStartTime)
                put(DEFAULT_DURATION, config.defaultDuration)
                put(USE_SAME_SNOOZE, config.useSameSnooze)
                put(SNOOZE_TIME, config.snoozeTime)
                put(USE_24_HOUR_FORMAT, config.use24HourFormat)
                put(SUNDAY_FIRST, config.isSundayFirst)
                put(HIGHLIGHT_WEEKENDS, config.highlightWeekends)
                put(HIGHLIGHT_WEEKENDS_COLOR, config.highlightWeekendsColor)
                put(ALLOW_CREATING_TASKS, config.allowCreatingTasks)
            }

            exportSettings(configItems)
        }
    }

    private fun setupImportSettings() {
        settings_import_holder.setOnClickListener {
            if (isQPlus()) {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"

                    try {
                        startActivityForResult(this, PICK_IMPORT_SOURCE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            } else {
                handlePermission(PERMISSION_READ_STORAGE) {
                    if (it) {
                        FilePickerDialog(this) {
                            ensureBackgroundThread {
                                parseFile(File(it).inputStream())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseFile(inputStream: InputStream?) {
        if (inputStream == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        var importedItems = 0
        val configValues = LinkedHashMap<String, Any>()
        inputStream.bufferedReader().use {
            while (true) {
                try {
                    val line = it.readLine() ?: break
                    val split = line.split("=".toRegex(), 2)
                    if (split.size == 2) {
                        configValues[split[0]] = split[1]
                    }
                    importedItems++
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }

        for ((key, value) in configValues) {
            when (key) {
                IS_USING_SHARED_THEME -> config.isUsingSharedTheme = value.toBoolean()
                TEXT_COLOR -> config.textColor = value.toInt()
                BACKGROUND_COLOR -> config.backgroundColor = value.toInt()
                PRIMARY_COLOR -> config.primaryColor = value.toInt()
                ACCENT_COLOR -> config.accentColor = value.toInt()
                APP_ICON_COLOR -> {
                    if (getAppIconColors().contains(value.toInt())) {
                        config.appIconColor = value.toInt()
                        checkAppIconColor()
                    }
                }
                USE_ENGLISH -> config.useEnglish = value.toBoolean()
                WAS_USE_ENGLISH_TOGGLED -> config.wasUseEnglishToggled = value.toBoolean()
                WIDGET_BG_COLOR -> config.widgetBgColor = value.toInt()
                WIDGET_TEXT_COLOR -> config.widgetTextColor = value.toInt()
                WEEK_NUMBERS -> config.showWeekNumbers = value.toBoolean()
                START_WEEKLY_AT -> config.startWeeklyAt = value.toInt()
                VIBRATE -> config.vibrateOnReminder = value.toBoolean()
                LAST_EVENT_REMINDER_MINUTES -> config.lastEventReminderMinutes1 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_2 -> config.lastEventReminderMinutes2 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_3 -> config.lastEventReminderMinutes3 = value.toInt()
                DISPLAY_PAST_EVENTS -> config.displayPastEvents = value.toInt()
                FONT_SIZE -> config.fontSize = value.toInt()
                MONTH_FONT_SIZE -> config.monthFontSize = value.toInt()
                LIST_WIDGET_VIEW_TO_OPEN -> config.listWidgetViewToOpen = value.toInt()
                REMINDER_AUDIO_STREAM -> config.reminderAudioStream = value.toInt()
                DISPLAY_DESCRIPTION -> config.displayDescription = value.toBoolean()
                REPLACE_DESCRIPTION -> config.replaceDescription = value.toBoolean()
                SHOW_GRID -> config.showGrid = value.toBoolean()
                LOOP_REMINDERS -> config.loopReminders = value.toBoolean()
                DIM_PAST_EVENTS -> config.dimPastEvents = value.toBoolean()
                DIM_COMPLETED_TASKS -> config.dimCompletedTasks = value.toBoolean()
                ALLOW_CHANGING_TIME_ZONES -> config.allowChangingTimeZones = value.toBoolean()
                USE_PREVIOUS_EVENT_REMINDERS -> config.usePreviousEventReminders = value.toBoolean()
                DEFAULT_REMINDER_1 -> config.defaultReminder1 = value.toInt()
                DEFAULT_REMINDER_2 -> config.defaultReminder2 = value.toInt()
                DEFAULT_REMINDER_3 -> config.defaultReminder3 = value.toInt()
                PULL_TO_REFRESH -> config.pullToRefresh = value.toBoolean()
                DEFAULT_START_TIME -> config.defaultStartTime = value.toInt()
                DEFAULT_DURATION -> config.defaultDuration = value.toInt()
                USE_SAME_SNOOZE -> config.useSameSnooze = value.toBoolean()
                SNOOZE_TIME -> config.snoozeTime = value.toInt()
                USE_24_HOUR_FORMAT -> config.use24HourFormat = value.toBoolean()
                SUNDAY_FIRST -> config.isSundayFirst = value.toBoolean()
                HIGHLIGHT_WEEKENDS -> config.highlightWeekends = value.toBoolean()
                HIGHLIGHT_WEEKENDS_COLOR -> config.highlightWeekendsColor = value.toInt()
                ALLOW_CREATING_TASKS -> config.allowCreatingTasks = value.toBoolean()
            }
        }

        runOnUiThread {
            val msg = if (configValues.size > 0) R.string.settings_imported_successfully else R.string.no_entries_for_importing
            toast(msg)

            setupSettingItems()
            updateWidgets()
        }
    }
}
