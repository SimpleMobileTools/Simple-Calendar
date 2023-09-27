package com.simplemobiletools.calendar.pro.activities

import android.app.Activity
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.databinding.ActivitySettingsBinding
import com.simplemobiletools.calendar.pro.dialogs.*
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.AlarmSound
import com.simplemobiletools.commons.models.RadioItem
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleActivity() {
    private val GET_RINGTONE_URI = 1
    private val PICK_SETTINGS_IMPORT_SOURCE_INTENT = 2
    private val PICK_EVENTS_IMPORT_SOURCE_INTENT = 3
    private val PICK_EVENTS_EXPORT_FILE_INTENT = 4

    private var mStoredPrimaryColor = 0

    private var eventTypesToExport = listOf<Long>()

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        mStoredPrimaryColor = getProperPrimaryColor()

        updateMaterialActivityViews(binding.settingsCoordinator, binding.settingsHolder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsToolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)
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
        setupStartWeekOn()
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
        setupCustomizeWidgetColors()
        setupViewToOpenFromListWidget()
        setupDimEvents()
        setupDimCompletedTasks()
        setupAllowChangingTimeZones()
        updateTextColors(binding.settingsHolder)
        checkPrimaryColor()
        setupEnableAutomaticBackups()
        setupManageAutomaticBackups()
        setupExportEvents()
        setupImportEvents()
        setupExportSettings()
        setupImportSettings()

        arrayOf(
            binding.settingsColorCustomizationSectionLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsRemindersLabel,
            binding.settingsCaldavLabel,
            binding.settingsNewEventsLabel,
            binding.settingsWeeklyViewLabel,
            binding.settingsMonthlyViewLabel,
            binding.settingsEventListsLabel,
            binding.settingsWidgetsLabel,
            binding.settingsEventsLabel,
            binding.settingsTasksLabel,
            binding.settingsBackupsLabel,
            binding.settingsMigratingLabel
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
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
        } else if (requestCode == PICK_SETTINGS_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val inputStream = contentResolver.openInputStream(resultData.data!!)
            parseFile(inputStream)
        } else if (requestCode == PICK_EVENTS_IMPORT_SOURCE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            tryImportEventsFromFile(resultData.data!!)
        } else if (requestCode == PICK_EVENTS_EXPORT_FILE_INTENT && resultCode == Activity.RESULT_OK && resultData != null && resultData.data != null) {
            val outputStream = contentResolver.openOutputStream(resultData.data!!)
            exportEventsTo(eventTypesToExport, outputStream)
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
        binding.settingsColorCustomizationHolder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupCustomizeNotifications() {
        binding.settingsCustomizeNotificationsHolder.beVisibleIf(isOreoPlus())
        binding.settingsCustomizeNotificationsHolder.setOnClickListener {
            launchCustomizeNotificationsIntent()
        }
    }

    private fun setupUseEnglish() = binding.apply {
        settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settingsUseEnglish.isChecked = config.useEnglish
        settingsUseEnglishHolder.setOnClickListener {
            settingsUseEnglish.toggle()
            config.useEnglish = settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    private fun setupLanguage() = binding.apply {
        settingsLanguage.text = Locale.getDefault().displayLanguage
        settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupManageEventTypes() {
        binding.settingsManageEventTypesHolder.setOnClickListener {
            startActivity(Intent(this, ManageEventTypesActivity::class.java))
        }
    }

    private fun setupManageQuickFilterEventTypes() = binding.apply {
        settingsManageQuickFilterEventTypesHolder.setOnClickListener {
            showQuickFilterPicker()
        }

        eventsHelper.getEventTypes(this@SettingsActivity, false) {
            settingsManageQuickFilterEventTypesHolder.beGoneIf(it.size < 2)
        }
    }

    private fun setupHourFormat() = binding.apply {
        settingsHourFormat.isChecked = config.use24HourFormat
        settingsHourFormatHolder.setOnClickListener {
            settingsHourFormat.toggle()
            config.use24HourFormat = settingsHourFormat.isChecked
        }
    }

    private fun setupAllowCreatingTasks() = binding.apply {
        settingsAllowCreatingTasks.isChecked = config.allowCreatingTasks
        settingsAllowCreatingTasksHolder.setOnClickListener {
            settingsAllowCreatingTasks.toggle()
            config.allowCreatingTasks = settingsAllowCreatingTasks.isChecked
        }
    }

    private fun setupCaldavSync() = binding.apply {
        settingsCaldavSync.isChecked = config.caldavSync
        settingsCaldavSyncHolder.setOnClickListener {
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

    private fun setupPullToRefresh() = binding.apply {
        settingsCaldavPullToRefreshHolder.beVisibleIf(config.caldavSync)
        settingsCaldavPullToRefresh.isChecked = config.pullToRefresh
        settingsCaldavPullToRefreshHolder.setOnClickListener {
            settingsCaldavPullToRefresh.toggle()
            config.pullToRefresh = settingsCaldavPullToRefresh.isChecked
        }
    }

    private fun setupManageSyncedCalendars() = binding.apply {
        settingsManageSyncedCalendarsHolder.beVisibleIf(config.caldavSync)
        settingsManageSyncedCalendarsHolder.setOnClickListener {
            showCalendarPicker()
        }
    }

    private fun toggleCaldavSync(enable: Boolean) = binding.apply {
        if (enable) {
            showCalendarPicker()
        } else {
            settingsCaldavSync.isChecked = false
            config.caldavSync = false
            settingsManageSyncedCalendarsHolder.beGone()
            settingsCaldavPullToRefreshHolder.beGone()

            ensureBackgroundThread {
                config.getSyncedCalendarIdsAsList().forEach {
                    calDAVHelper.deleteCalDAVCalendarEvents(it.toLong())
                }
                eventTypesDB.deleteEventTypesWithCalendarId(config.getSyncedCalendarIdsAsList())
                updateDefaultEventTypeText()
            }
        }
    }

    private fun showCalendarPicker() = binding.apply {
        val oldCalendarIds = config.getSyncedCalendarIdsAsList()

        SelectCalendarsDialog(this@SettingsActivity) {
            val newCalendarIds = config.getSyncedCalendarIdsAsList()
            if (newCalendarIds.isEmpty() && !config.caldavSync) {
                return@SelectCalendarsDialog
            }

            settingsManageSyncedCalendarsHolder.beVisibleIf(newCalendarIds.isNotEmpty())
            settingsCaldavPullToRefreshHolder.beVisibleIf(newCalendarIds.isNotEmpty())
            settingsCaldavSync.isChecked = newCalendarIds.isNotEmpty()
            config.caldavSync = newCalendarIds.isNotEmpty()
            if (settingsCaldavSync.isChecked) {
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
                            eventsHelper.insertOrUpdateEventType(this@SettingsActivity, eventType)
                        }
                    }

                    syncCalDAVCalendars {
                        calDAVHelper.refreshCalendars(showToasts = true, scheduleNextSync = true) {
                            if (settingsCaldavSync.isChecked) {
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
        SelectEventTypesDialog(this, config.quickFilterEventTypes) {
            config.quickFilterEventTypes = it
        }
    }

    private fun setupStartWeekOn() = binding.apply {
        val items = arrayListOf(
            RadioItem(DateTimeConstants.SUNDAY, getString(com.simplemobiletools.commons.R.string.sunday)),
            RadioItem(DateTimeConstants.MONDAY, getString(com.simplemobiletools.commons.R.string.monday)),
            RadioItem(DateTimeConstants.TUESDAY, getString(com.simplemobiletools.commons.R.string.tuesday)),
            RadioItem(DateTimeConstants.WEDNESDAY, getString(com.simplemobiletools.commons.R.string.wednesday)),
            RadioItem(DateTimeConstants.THURSDAY, getString(com.simplemobiletools.commons.R.string.thursday)),
            RadioItem(DateTimeConstants.FRIDAY, getString(com.simplemobiletools.commons.R.string.friday)),
            RadioItem(DateTimeConstants.SATURDAY, getString(com.simplemobiletools.commons.R.string.saturday)),
        )

        settingsStartWeekOn.text = getDayOfWeekString(config.firstDayOfWeek)
        settingsStartWeekOnHolder.setOnClickListener {
            RadioGroupDialog(this@SettingsActivity, items, config.firstDayOfWeek) { any ->
                val firstDayOfWeek = any as Int
                config.firstDayOfWeek = firstDayOfWeek
                settingsStartWeekOn.text = getDayOfWeekString(firstDayOfWeek)
            }
        }
    }

    private fun setupHighlightWeekends() = binding.apply {
        settingsHighlightWeekends.isChecked = config.highlightWeekends
        settingsHighlightWeekendsColorHolder.beVisibleIf(config.highlightWeekends)
        settingsHighlightWeekendsHolder.setOnClickListener {
            settingsHighlightWeekends.toggle()
            config.highlightWeekends = settingsHighlightWeekends.isChecked
            settingsHighlightWeekendsColorHolder.beVisibleIf(config.highlightWeekends)
        }
    }

    private fun setupHighlightWeekendsColor() = binding.apply {
        settingsHighlightWeekendsColor.setFillWithStroke(config.highlightWeekendsColor, getProperBackgroundColor())
        settingsHighlightWeekendsColorHolder.setOnClickListener {
            ColorPickerDialog(this@SettingsActivity, config.highlightWeekendsColor) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    config.highlightWeekendsColor = color
                    settingsHighlightWeekendsColor.setFillWithStroke(color, getProperBackgroundColor())
                }
            }
        }
    }

    private fun setupDeleteAllEvents() = binding.apply {
        settingsDeleteAllEventsHolder.setOnClickListener {
            ConfirmationDialog(this@SettingsActivity, messageId = R.string.delete_all_events_confirmation) {
                eventsHelper.deleteAllEvents()
            }
        }
    }

    private fun setupDisplayDescription() = binding.apply {
        settingsDisplayDescription.isChecked = config.displayDescription
        settingsReplaceDescriptionHolder.beVisibleIf(config.displayDescription)
        settingsDisplayDescriptionHolder.setOnClickListener {
            settingsDisplayDescription.toggle()
            config.displayDescription = settingsDisplayDescription.isChecked
            settingsReplaceDescriptionHolder.beVisibleIf(config.displayDescription)
        }
    }

    private fun setupReplaceDescription() = binding.apply {
        settingsReplaceDescription.isChecked = config.replaceDescription
        settingsReplaceDescriptionHolder.setOnClickListener {
            settingsReplaceDescription.toggle()
            config.replaceDescription = settingsReplaceDescription.isChecked
        }
    }

    private fun setupWeeklyStart() = binding.apply {
        settingsStartWeeklyAt.text = getHoursString(config.startWeeklyAt)
        settingsStartWeeklyAtHolder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            (0..16).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(this@SettingsActivity, items, config.startWeeklyAt) {
                config.startWeeklyAt = it as Int
                settingsStartWeeklyAt.text = getHoursString(it)
            }
        }
    }

    private fun setupMidnightSpanEvents() = binding.apply {
        settingsMidnightSpanEvent.isChecked = config.showMidnightSpanningEventsAtTop
        settingsMidnightSpanEventsHolder.setOnClickListener {
            settingsMidnightSpanEvent.toggle()
            config.showMidnightSpanningEventsAtTop = settingsMidnightSpanEvent.isChecked
        }
    }

    private fun setupAllowCustomizeDayCount() = binding.apply {
        settingsAllowCustomizeDayCount.isChecked = config.allowCustomizeDayCount
        settingsAllowCustomizeDayCountHolder.setOnClickListener {
            settingsAllowCustomizeDayCount.toggle()
            config.allowCustomizeDayCount = settingsAllowCustomizeDayCount.isChecked
        }
    }

    private fun setupStartWeekWithCurrentDay() = binding.apply {
        settingsStartWeekWithCurrentDay.isChecked = config.startWeekWithCurrentDay
        settingsStartWeekWithCurrentDayHolder.setOnClickListener {
            settingsStartWeekWithCurrentDay.toggle()
            config.startWeekWithCurrentDay = settingsStartWeekWithCurrentDay.isChecked
        }
    }

    private fun setupWeekNumbers() = binding.apply {
        settingsWeekNumbers.isChecked = config.showWeekNumbers
        settingsWeekNumbersHolder.setOnClickListener {
            settingsWeekNumbers.toggle()
            config.showWeekNumbers = settingsWeekNumbers.isChecked
        }
    }

    private fun setupShowGrid() = binding.apply {
        settingsShowGrid.isChecked = config.showGrid
        settingsShowGridHolder.setOnClickListener {
            settingsShowGrid.toggle()
            config.showGrid = settingsShowGrid.isChecked
        }
    }

    private fun setupReminderSound() = binding.apply {
        settingsReminderSoundHolder.beGoneIf(isOreoPlus())
        settingsReminderSound.text = config.reminderSoundTitle

        settingsReminderSoundHolder.setOnClickListener {
            SelectAlarmSoundDialog(this@SettingsActivity,
                config.reminderSoundUri,
                config.reminderAudioStream,
                GET_RINGTONE_URI,
                RingtoneManager.TYPE_NOTIFICATION,
                false,
                onAlarmPicked = {
                    if (it != null) {
                        updateReminderSound(it)
                    }
                },
                onAlarmSoundDeleted = {
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
        binding.settingsReminderSound.text = alarmSound.title
    }

    private fun setupReminderAudioStream() = binding.apply {
        settingsReminderAudioStream.text = getAudioStreamText()
        settingsReminderAudioStreamHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(AudioManager.STREAM_ALARM, getString(R.string.alarm_stream)),
                RadioItem(AudioManager.STREAM_SYSTEM, getString(R.string.system_stream)),
                RadioItem(AudioManager.STREAM_NOTIFICATION, getString(R.string.notification_stream)),
                RadioItem(AudioManager.STREAM_RING, getString(R.string.ring_stream))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.reminderAudioStream) {
                config.reminderAudioStream = it as Int
                settingsReminderAudioStream.text = getAudioStreamText()
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

    private fun setupVibrate() = binding.apply {
        settingsVibrate.isChecked = config.vibrateOnReminder
        settingsVibrateHolder.setOnClickListener {
            settingsVibrate.toggle()
            config.vibrateOnReminder = settingsVibrate.isChecked
        }
    }

    private fun setupLoopReminders() = binding.apply {
        settingsLoopReminders.isChecked = config.loopReminders
        settingsLoopRemindersHolder.setOnClickListener {
            settingsLoopReminders.toggle()
            config.loopReminders = settingsLoopReminders.isChecked
        }
    }

    private fun setupUseSameSnooze() = binding.apply {
        settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        settingsUseSameSnooze.isChecked = config.useSameSnooze
        settingsUseSameSnoozeHolder.setOnClickListener {
            settingsUseSameSnooze.toggle()
            config.useSameSnooze = settingsUseSameSnooze.isChecked
            settingsSnoozeTimeHolder.beVisibleIf(config.useSameSnooze)
        }
    }

    private fun setupSnoozeTime() {
        updateSnoozeTime()
        binding.settingsSnoozeTimeHolder.setOnClickListener {
            showPickSecondsDialogHelper(config.snoozeTime, true) {
                config.snoozeTime = it / 60
                updateSnoozeTime()
            }
        }
    }

    private fun updateSnoozeTime() {
        binding.settingsSnoozeTime.text = formatMinutesToTimeString(config.snoozeTime)
    }

    private fun setupDefaultReminder() = binding.apply {
        settingsUseLastEventReminders.isChecked = config.usePreviousEventReminders
        toggleDefaultRemindersVisibility(!config.usePreviousEventReminders)
        settingsUseLastEventRemindersHolder.setOnClickListener {
            settingsUseLastEventReminders.toggle()
            config.usePreviousEventReminders = settingsUseLastEventReminders.isChecked
            toggleDefaultRemindersVisibility(!settingsUseLastEventReminders.isChecked)
        }
    }

    private fun setupDefaultReminder1() = binding.apply {
        settingsDefaultReminder1.text = getFormattedMinutes(config.defaultReminder1)
        settingsDefaultReminder1Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder1) {
                config.defaultReminder1 = if (it == -1 || it == 0) it else it / 60
                settingsDefaultReminder1.text = getFormattedMinutes(config.defaultReminder1)
            }
        }
    }

    private fun setupDefaultReminder2() = binding.apply {
        settingsDefaultReminder2.text = getFormattedMinutes(config.defaultReminder2)
        settingsDefaultReminder2Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder2) {
                config.defaultReminder2 = if (it == -1 || it == 0) it else it / 60
                settingsDefaultReminder2.text = getFormattedMinutes(config.defaultReminder2)
            }
        }
    }

    private fun setupDefaultReminder3() = binding.apply {
        settingsDefaultReminder3.text = getFormattedMinutes(config.defaultReminder3)
        settingsDefaultReminder3Holder.setOnClickListener {
            showPickSecondsDialogHelper(config.defaultReminder3) {
                config.defaultReminder3 = if (it == -1 || it == 0) it else it / 60
                settingsDefaultReminder3.text = getFormattedMinutes(config.defaultReminder3)
            }
        }
    }

    private fun toggleDefaultRemindersVisibility(show: Boolean) = binding.apply {
        arrayOf(settingsDefaultReminder1Holder, settingsDefaultReminder2Holder, settingsDefaultReminder3Holder).forEach {
            it.beVisibleIf(show)
        }
    }

    private fun getHoursString(hours: Int): String {
        return if (config.use24HourFormat) {
            String.format("%02d:00", hours)
        } else {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hours)
            calendar.set(Calendar.MINUTE, 0)
            val format = SimpleDateFormat("hh.mm aa")
            format.format(calendar.time)
        }
    }

    private fun setupDisplayPastEvents() {
        var displayPastEvents = config.displayPastEvents
        updatePastEventsText(displayPastEvents)
        binding.settingsDisplayPastEventsHolder.setOnClickListener {
            CustomIntervalPickerDialog(this, displayPastEvents * 60) {
                val result = it / 60
                displayPastEvents = result
                config.displayPastEvents = result
                updatePastEventsText(result)
            }
        }
    }

    private fun updatePastEventsText(displayPastEvents: Int) {
        binding.settingsDisplayPastEvents.text = getDisplayPastEventsText(displayPastEvents)
    }

    private fun getDisplayPastEventsText(displayPastEvents: Int): String {
        return if (displayPastEvents == 0) {
            getString(com.simplemobiletools.commons.R.string.never)
        } else {
            getFormattedMinutes(displayPastEvents, false)
        }
    }

    private fun setupFontSize() = binding.apply {
        settingsFontSize.text = getFontSizeText()
        settingsFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, getString(com.simplemobiletools.commons.R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, getString(com.simplemobiletools.commons.R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, getString(com.simplemobiletools.commons.R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, getString(com.simplemobiletools.commons.R.string.extra_large))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                settingsFontSize.text = getFontSizeText()
                updateWidgets()
            }
        }
    }

    private fun setupCustomizeWidgetColors() {
        binding.settingsWidgetColorCustomizationHolder.setOnClickListener {
            Intent(this, WidgetListConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
    }

    private fun setupViewToOpenFromListWidget() = binding.apply {
        settingsListWidgetViewToOpen.text = getDefaultViewText()
        settingsListWidgetViewToOpenHolder.setOnClickListener {
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
                settingsListWidgetViewToOpen.text = getDefaultViewText()
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

    private fun setupDimEvents() = binding.apply {
        settingsDimPastEvents.isChecked = config.dimPastEvents
        settingsDimPastEventsHolder.setOnClickListener {
            settingsDimPastEvents.toggle()
            config.dimPastEvents = settingsDimPastEvents.isChecked
        }
    }

    private fun setupDimCompletedTasks() = binding.apply {
        settingsDimCompletedTasks.isChecked = config.dimCompletedTasks
        settingsDimCompletedTasksHolder.setOnClickListener {
            settingsDimCompletedTasks.toggle()
            config.dimCompletedTasks = settingsDimCompletedTasks.isChecked
        }
    }

    private fun setupAllowChangingTimeZones() = binding.apply {
        settingsAllowChangingTimeZones.isChecked = config.allowChangingTimeZones
        settingsAllowChangingTimeZonesHolder.setOnClickListener {
            settingsAllowChangingTimeZones.toggle()
            config.allowChangingTimeZones = settingsAllowChangingTimeZones.isChecked
        }
    }

    private fun setupDefaultStartTime() {
        updateDefaultStartTimeText()
        binding.settingsDefaultStartTimeHolder.setOnClickListener {
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

                    if (config.isUsingSystemTheme) {
                        val timeFormat = if (config.use24HourFormat) {
                            TimeFormat.CLOCK_24H
                        } else {
                            TimeFormat.CLOCK_12H
                        }

                        val timePicker = MaterialTimePicker.Builder()
                            .setTimeFormat(timeFormat)
                            .setHour(currentDateTime.hourOfDay)
                            .setMinute(currentDateTime.minuteOfHour)
                            .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                            .build()

                        timePicker.addOnPositiveButtonClickListener {
                            config.defaultStartTime = timePicker.hour * 60 + timePicker.minute
                            updateDefaultStartTimeText()
                        }

                        timePicker.show(supportFragmentManager, "")
                    } else {
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
    }

    private fun updateDefaultStartTimeText() {
        binding.settingsDefaultStartTime.text = when (config.defaultStartTime) {
            DEFAULT_START_TIME_CURRENT_TIME -> getString(R.string.current_time)
            DEFAULT_START_TIME_NEXT_FULL_HOUR -> getString(R.string.next_full_hour)
            else -> {
                val hours = config.defaultStartTime / 60
                val minutes = config.defaultStartTime % 60
                val dateTime = DateTime.now().withHourOfDay(hours).withMinuteOfHour(minutes)
                Formatter.getTime(this, dateTime)
            }
        }
    }

    private fun setupDefaultDuration() {
        updateDefaultDurationText()
        binding.settingsDefaultDurationHolder.setOnClickListener {
            CustomIntervalPickerDialog(this, config.defaultDuration * 60) {
                val result = it / 60
                config.defaultDuration = result
                updateDefaultDurationText()
            }
        }
    }

    private fun updateDefaultDurationText() {
        val duration = config.defaultDuration
        binding.settingsDefaultDuration.text = if (duration == 0) {
            "0 ${getString(com.simplemobiletools.commons.R.string.minutes_raw)}"
        } else {
            getFormattedMinutes(duration, false)
        }
    }

    private fun setupDefaultEventType() = binding.apply {
        updateDefaultEventTypeText()
        settingsDefaultEventType.text = getString(R.string.last_used_one)
        settingsDefaultEventTypeHolder.setOnClickListener {
            SelectEventTypeDialog(this@SettingsActivity, config.defaultEventTypeId, true, false, true, true, false) {
                config.defaultEventTypeId = it.id!!
                updateDefaultEventTypeText()
            }
        }
    }

    private fun updateDefaultEventTypeText() {
        if (config.defaultEventTypeId == -1L) {
            runOnUiThread {
                binding.settingsDefaultEventType.text = getString(R.string.last_used_one)
            }
        } else {
            ensureBackgroundThread {
                val eventType = eventTypesDB.getEventTypeWithId(config.defaultEventTypeId)
                if (eventType != null) {
                    config.lastUsedCaldavCalendarId = eventType.caldavCalendarId
                    runOnUiThread {
                        binding.settingsDefaultEventType.text = eventType.title
                    }
                } else {
                    config.defaultEventTypeId = -1
                    updateDefaultEventTypeText()
                }
            }
        }
    }

    private fun setupEnableAutomaticBackups() = binding.apply {
        settingsBackupsLabel.beVisibleIf(isRPlus())
        settingsBackupsDivider.beVisibleIf(isRPlus())
        settingsEnableAutomaticBackupsHolder.beVisibleIf(isRPlus())
        settingsEnableAutomaticBackups.isChecked = config.autoBackup
        settingsEnableAutomaticBackupsHolder.setOnClickListener {
            val wasBackupDisabled = !config.autoBackup
            if (wasBackupDisabled) {
                ManageAutomaticBackupsDialog(
                    activity = this@SettingsActivity,
                    onSuccess = {
                        enableOrDisableAutomaticBackups(true)
                        scheduleNextAutomaticBackup()
                    }
                )
            } else {
                cancelScheduledAutomaticBackup()
                enableOrDisableAutomaticBackups(false)
            }
        }
    }

    private fun setupManageAutomaticBackups() = binding.apply {
        settingsManageAutomaticBackupsHolder.beVisibleIf(isRPlus() && config.autoBackup)
        settingsManageAutomaticBackupsHolder.setOnClickListener {
            ManageAutomaticBackupsDialog(
                activity = this@SettingsActivity,
                onSuccess = {
                    scheduleNextAutomaticBackup()
                }
            )
        }
    }

    private fun enableOrDisableAutomaticBackups(enable: Boolean) = binding.apply {
        config.autoBackup = enable
        settingsEnableAutomaticBackups.isChecked = enable
        settingsManageAutomaticBackupsHolder.beVisibleIf(enable)
    }

    private fun setupExportEvents() {
        binding.eventsExportHolder.setOnClickListener {
            tryExportEvents()
        }
    }

    private fun setupImportEvents() {
        binding.eventsImportHolder.setOnClickListener {
            tryImportEvents()
        }
    }

    private fun setupExportSettings() {
        binding.settingsExportHolder.setOnClickListener {
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
                put(SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP, config.showMidnightSpanningEventsAtTop)
                put(ALLOW_CUSTOMIZE_DAY_COUNT, config.allowCustomizeDayCount)
                put(START_WEEK_WITH_CURRENT_DAY, config.startWeekWithCurrentDay)
                put(VIBRATE, config.vibrateOnReminder)
                put(LAST_EVENT_REMINDER_MINUTES, config.lastEventReminderMinutes1)
                put(LAST_EVENT_REMINDER_MINUTES_2, config.lastEventReminderMinutes2)
                put(LAST_EVENT_REMINDER_MINUTES_3, config.lastEventReminderMinutes3)
                put(DISPLAY_PAST_EVENTS, config.displayPastEvents)
                put(FONT_SIZE, config.fontSize)
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
                put(FIRST_DAY_OF_WEEK, config.firstDayOfWeek)
                put(HIGHLIGHT_WEEKENDS, config.highlightWeekends)
                put(HIGHLIGHT_WEEKENDS_COLOR, config.highlightWeekendsColor)
                put(ALLOW_CREATING_TASKS, config.allowCreatingTasks)
            }

            exportSettings(configItems)
        }
    }

    private fun setupImportSettings() {
        binding.settingsImportHolder.setOnClickListener {
            if (isQPlus()) {
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"

                    try {
                        startActivityForResult(this, PICK_SETTINGS_IMPORT_SOURCE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(com.simplemobiletools.commons.R.string.system_service_disabled, Toast.LENGTH_LONG)
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
            toast(com.simplemobiletools.commons.R.string.unknown_error_occurred)
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
                SHOW_MIDNIGHT_SPANNING_EVENTS_AT_TOP -> config.showMidnightSpanningEventsAtTop = value.toBoolean()
                ALLOW_CUSTOMIZE_DAY_COUNT -> config.allowCustomizeDayCount = value.toBoolean()
                START_WEEK_WITH_CURRENT_DAY -> config.startWeekWithCurrentDay = value.toBoolean()
                VIBRATE -> config.vibrateOnReminder = value.toBoolean()
                LAST_EVENT_REMINDER_MINUTES -> config.lastEventReminderMinutes1 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_2 -> config.lastEventReminderMinutes2 = value.toInt()
                LAST_EVENT_REMINDER_MINUTES_3 -> config.lastEventReminderMinutes3 = value.toInt()
                DISPLAY_PAST_EVENTS -> config.displayPastEvents = value.toInt()
                FONT_SIZE -> config.fontSize = value.toInt()
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
                SUNDAY_FIRST -> config.firstDayOfWeek = DateTimeConstants.SUNDAY
                FIRST_DAY_OF_WEEK -> config.firstDayOfWeek = value.toInt()
                HIGHLIGHT_WEEKENDS -> config.highlightWeekends = value.toBoolean()
                HIGHLIGHT_WEEKENDS_COLOR -> config.highlightWeekendsColor = value.toInt()
                ALLOW_CREATING_TASKS -> config.allowCreatingTasks = value.toBoolean()
            }
        }

        runOnUiThread {
            val msg = if (configValues.size > 0) {
                com.simplemobiletools.commons.R.string.settings_imported_successfully
            } else {
                com.simplemobiletools.commons.R.string.no_entries_for_importing
            }

            toast(msg)

            setupSettingItems()
            updateWidgets()
        }
    }

    private fun tryImportEvents() {
        if (isQPlus()) {
            handleNotificationPermission { granted ->
                if (granted) {
                    hideKeyboard()
                    Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/calendar"

                        try {
                            startActivityForResult(this, PICK_EVENTS_IMPORT_SOURCE_INTENT)
                        } catch (e: ActivityNotFoundException) {
                            toast(com.simplemobiletools.commons.R.string.system_service_disabled, Toast.LENGTH_LONG)
                        } catch (e: Exception) {
                            showErrorToast(e)
                        }
                    }
                } else {
                    PermissionRequiredDialog(this, com.simplemobiletools.commons.R.string.allow_notifications_reminders, { openNotificationSettings() })
                }
            }
        } else {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    importEvents()
                }
            }
        }
    }

    private fun importEvents() {
        FilePickerDialog(this) {
            showImportEventsDialog(it) {}
        }
    }


    private fun tryExportEvents() {
        if (isQPlus()) {
            ExportEventsDialog(this, config.lastExportPath, true) { file, eventTypes ->
                eventTypesToExport = eventTypes
                hideKeyboard()

                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/calendar"
                    putExtra(Intent.EXTRA_TITLE, file.name)
                    addCategory(Intent.CATEGORY_OPENABLE)

                    try {
                        startActivityForResult(this, PICK_EVENTS_EXPORT_FILE_INTENT)
                    } catch (e: ActivityNotFoundException) {
                        toast(com.simplemobiletools.commons.R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        showErrorToast(e)
                    }
                }
            }
        } else {
            handlePermission(PERMISSION_WRITE_STORAGE) { granted ->
                if (granted) {
                    ExportEventsDialog(this, config.lastExportPath, false) { file, eventTypes ->
                        getFileOutputStream(file.toFileDirItem(this), true) {
                            exportEventsTo(eventTypes, it)
                        }
                    }
                }
            }
        }
    }

    private fun exportEventsTo(eventTypes: List<Long>, outputStream: OutputStream?) {
        ensureBackgroundThread {
            val events = eventsHelper.getEventsToExport(eventTypes, config.exportEvents, config.exportTasks, config.exportPastEntries)
            if (events.isEmpty()) {
                toast(com.simplemobiletools.commons.R.string.no_entries_for_exporting)
            } else {
                IcsExporter(this).exportEvents(outputStream, events, true) { result ->
                    toast(
                        when (result) {
                            IcsExporter.ExportResult.EXPORT_OK -> com.simplemobiletools.commons.R.string.exporting_successful
                            IcsExporter.ExportResult.EXPORT_PARTIAL -> com.simplemobiletools.commons.R.string.exporting_some_entries_failed
                            else -> com.simplemobiletools.commons.R.string.exporting_failed
                        }
                    )
                }
            }
        }
    }
}
