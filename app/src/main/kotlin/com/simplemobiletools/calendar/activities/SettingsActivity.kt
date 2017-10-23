package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.content.res.Resources
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.dialogs.CustomEventReminderDialog
import com.simplemobiletools.calendar.dialogs.SelectCalendarsDialog
import com.simplemobiletools.calendar.dialogs.SnoozePickerDialog
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.helpers.CalDAVHandler
import com.simplemobiletools.calendar.helpers.FONT_SIZE_LARGE
import com.simplemobiletools.calendar.helpers.FONT_SIZE_MEDIUM
import com.simplemobiletools.calendar.helpers.FONT_SIZE_SMALL
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.PERMISSION_WRITE_CALENDAR
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    private val GET_RINGTONE_URI = 1

    lateinit var res: Resources
    private var mStoredPrimaryColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        res = resources
        mStoredPrimaryColor = config.primaryColor
        setupCaldavSync()
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupManageEventTypes()
        setupHourFormat()
        setupSundayFirst()
        setupReplaceDescription()
        setupWeekNumbers()
        setupWeeklyStart()
        setupWeeklyEnd()
        setupVibrate()
        setupReminderSound()
        setupSnoozeDelay()
        setupEventReminder()
        setupDisplayPastEvents()
        setupFontSize()
        updateTextColors(settings_holder)
        checkPrimaryColor()
    }

    override fun onPause() {
        super.onPause()
        mStoredPrimaryColor = config.primaryColor
    }

    private fun checkPrimaryColor() {
        if (config.primaryColor != mStoredPrimaryColor) {
            dbHelper.getEventTypes {
                if (it.filter { it.caldavCalendarId == 0 }.size == 1) {
                    val eventType = it.first { it.caldavCalendarId == 0 }
                    eventType.color = config.primaryColor
                    dbHelper.updateEventType(eventType)
                }
            }
        }
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupManageEventTypes() {
        settings_manage_event_types_holder.setOnClickListener {
            startActivity(Intent(this, ManageEventTypesActivity::class.java))
        }
    }

    private fun setupHourFormat() {
        settings_hour_format.isChecked = config.use24hourFormat
        settings_hour_format_holder.setOnClickListener {
            settings_hour_format.toggle()
            config.use24hourFormat = settings_hour_format.isChecked
        }
    }

    private fun setupCaldavSync() {
        settings_caldav_sync.isChecked = config.caldavSync
        settings_caldav_sync_holder.setOnClickListener {
            if (config.caldavSync) {
                toggleCaldavSync(false)
            } else {
                handlePermission(PERMISSION_WRITE_CALENDAR) {
                    if (it) {
                        toggleCaldavSync(true)
                    }
                }
            }
        }

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
            config.getSyncedCalendarIdsAsList().forEach {
                CalDAVHandler(applicationContext).deleteCalDAVCalendarEvents(it.toLong())
            }
            dbHelper.deleteEventTypesWithCalendarId(config.caldavSyncedCalendarIDs)
        }
    }

    private fun showCalendarPicker() {
        val oldCalendarIds = config.getSyncedCalendarIdsAsList()

        SelectCalendarsDialog(this) {
            val newCalendarIds = config.getSyncedCalendarIdsAsList()
            settings_manage_synced_calendars_holder.beVisibleIf(newCalendarIds.isNotEmpty())
            settings_caldav_sync.isChecked = newCalendarIds.isNotEmpty()
            config.caldavSync = newCalendarIds.isNotEmpty()
            toast(R.string.syncing)

            Thread({
                if (newCalendarIds.isNotEmpty()) {
                    val existingEventTypeNames = dbHelper.fetchEventTypes().map { it.getDisplayTitle().toLowerCase() } as ArrayList<String>
                    getSyncedCalDAVCalendars().forEach {
                        val calendarTitle = it.getFullTitle()
                        if (!existingEventTypeNames.contains(calendarTitle.toLowerCase())) {
                            val eventType = EventType(0, it.displayName, it.color, it.id, it.displayName, it.accountName)
                            existingEventTypeNames.add(calendarTitle.toLowerCase())
                            dbHelper.insertEventType(eventType)
                        }
                    }
                    CalDAVHandler(applicationContext).refreshCalendars(this) {}
                }

                val removedCalendarIds = oldCalendarIds.filter { !newCalendarIds.contains(it) }
                removedCalendarIds.forEach {
                    CalDAVHandler(applicationContext).deleteCalDAVCalendarEvents(it.toLong())
                    dbHelper.getEventTypeWithCalDAVCalendarId(it.toInt())?.apply {
                        dbHelper.deleteEventTypes(arrayListOf(this), true) {}
                    }
                }
                dbHelper.deleteEventTypesWithCalendarId(TextUtils.join(",", removedCalendarIds))
                toast(R.string.synchronization_completed)
            }).start()
        }
    }

    private fun setupSundayFirst() {
        settings_sunday_first.isChecked = config.isSundayFirst
        settings_sunday_first_holder.setOnClickListener {
            settings_sunday_first.toggle()
            config.isSundayFirst = settings_sunday_first.isChecked
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
            (0..24).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(this@SettingsActivity, items, config.startWeeklyAt) {
                if (it as Int >= config.endWeeklyAt) {
                    toast(R.string.day_end_before_start)
                } else {
                    config.startWeeklyAt = it
                    settings_start_weekly_at.text = getHoursString(it)
                }
            }
        }
    }

    private fun setupWeeklyEnd() {
        settings_end_weekly_at.text = getHoursString(config.endWeeklyAt)
        settings_end_weekly_at_holder.setOnClickListener {
            val items = ArrayList<RadioItem>()
            (0..24).mapTo(items) { RadioItem(it, getHoursString(it)) }

            RadioGroupDialog(this@SettingsActivity, items, config.endWeeklyAt) {
                if (it as Int <= config.startWeeklyAt) {
                    toast(R.string.day_end_before_start)
                } else {
                    config.endWeeklyAt = it
                    settings_end_weekly_at.text = getHoursString(it)
                }
            }
        }
    }

    private fun setupWeekNumbers() {
        settings_week_numbers.isChecked = config.displayWeekNumbers
        settings_week_numbers_holder.setOnClickListener {
            settings_week_numbers.toggle()
            config.displayWeekNumbers = settings_week_numbers.isChecked
        }
    }

    private fun setupReminderSound() {
        val noRingtone = res.getString(R.string.no_ringtone_selected)
        if (config.reminderSound.isEmpty()) {
            settings_reminder_sound.text = noRingtone
        } else {
            settings_reminder_sound.text = RingtoneManager.getRingtone(this, Uri.parse(config.reminderSound))?.getTitle(this) ?: noRingtone
        }
        settings_reminder_sound_holder.setOnClickListener {
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, res.getString(R.string.reminder_sound))
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(config.reminderSound))

                if (resolveActivity(packageManager) != null)
                    startActivityForResult(this, GET_RINGTONE_URI)
                else {
                    toast(R.string.no_ringtone_picker)
                }
            }
        }
    }

    private fun setupVibrate() {
        settings_vibrate.isChecked = config.vibrateOnReminder
        settings_vibrate_holder.setOnClickListener {
            settings_vibrate.toggle()
            config.vibrateOnReminder = settings_vibrate.isChecked
        }
    }

    private fun setupSnoozeDelay() {
        updateSnoozeText()
        settings_snooze_delay_holder.setOnClickListener {
            SnoozePickerDialog(this, config.snoozeDelay) {
                config.snoozeDelay = it
                updateSnoozeText()
            }
        }
    }

    private fun updateSnoozeText() {
        settings_snooze_delay.text = res.getQuantityString(R.plurals.by_minutes, config.snoozeDelay, config.snoozeDelay)
    }

    private fun setupEventReminder() {
        var reminderMinutes = config.defaultReminderMinutes
        settings_default_reminder.text = getFormattedMinutes(reminderMinutes)
        settings_default_reminder_holder.setOnClickListener {
            showEventReminderDialog(reminderMinutes) {
                config.defaultReminderMinutes = it
                reminderMinutes = it
                settings_default_reminder.text = getFormattedMinutes(it)
            }
        }
    }

    private fun getHoursString(hours: Int): String {
        return if (hours < 10) {
            "0$hours:00"
        } else {
            "$hours:00"
        }
    }

    private fun setupDisplayPastEvents() {
        var displayPastEvents = config.displayPastEvents
        updatePastEventsText(displayPastEvents)
        settings_display_past_events_holder.setOnClickListener {
            CustomEventReminderDialog(this, displayPastEvents) {
                displayPastEvents = it
                config.displayPastEvents = it
                updatePastEventsText(it)
            }
        }
    }

    private fun updatePastEventsText(displayPastEvents: Int) {
        settings_display_past_events.text = getDisplayPastEventsText(displayPastEvents)
    }

    private fun getDisplayPastEventsText(displayPastEvents: Int): String {
        return if (displayPastEvents == 0)
            getString(R.string.never)
        else
            getFormattedMinutes(displayPastEvents, false)
    }

    private fun setupFontSize() {
        settings_font_size.text = getFontSizeText()
        settings_font_size_holder.setOnClickListener {
            val items = arrayListOf(
                    RadioItem(FONT_SIZE_SMALL, res.getString(R.string.small)),
                    RadioItem(FONT_SIZE_MEDIUM, res.getString(R.string.medium)),
                    RadioItem(FONT_SIZE_LARGE, res.getString(R.string.large)))

            RadioGroupDialog(this@SettingsActivity, items, config.fontSize) {
                config.fontSize = it as Int
                settings_font_size.text = getFontSizeText()
                updateWidgets()
                updateListWidget()
            }
        }
    }

    private fun getFontSizeText() = getString(when (config.fontSize) {
        FONT_SIZE_SMALL -> R.string.small
        FONT_SIZE_MEDIUM -> R.string.medium
        else -> R.string.large
    })

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == GET_RINGTONE_URI) {
                val uri = data?.getParcelableExtra<Parcelable>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                if (uri == null) {
                    config.reminderSound = ""
                } else {
                    settings_reminder_sound.text = RingtoneManager.getRingtone(this, uri as Uri)?.getTitle(this)
                    config.reminderSound = uri.toString()
                }
            }
        }
    }
}
