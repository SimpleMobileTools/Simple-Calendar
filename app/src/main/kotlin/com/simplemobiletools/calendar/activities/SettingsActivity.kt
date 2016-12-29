package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.AdapterView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.beVisibleIf
import com.simplemobiletools.calendar.helpers.DAY_MINS
import com.simplemobiletools.calendar.helpers.HOUR_MINS
import com.simplemobiletools.calendar.helpers.REMINDER_CUSTOM
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    val GET_RINGTONE_URI = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onResume() {
        super.onResume()

        setupCustomizeColors()
        setupSundayFirst()
        setupWeekNumbers()
        setupVibrate()
        setupReminderSound()
        setupEventReminder()
        updateTextColors(settings_holder)
    }

    private fun setupCustomizeColors() {
        settings_customize_colors_holder.setOnClickListener {
            startCustomizationActivity()
        }
    }

    private fun setupSundayFirst() {
        settings_sunday_first.isChecked = config.isSundayFirst
        settings_sunday_first_holder.setOnClickListener {
            settings_sunday_first.toggle()
            config.isSundayFirst = settings_sunday_first.isChecked
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
        settings_reminder_sound.text = RingtoneManager.getRingtone(this, Uri.parse(config.reminderSound)).getTitle(this)
        settings_reminder_sound_holder.setOnClickListener {
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, resources.getString(R.string.notification_sound))
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

    private fun setupEventReminder() {
        var isInitialSetup = true
        val reminderType = config.defaultReminderType
        val reminderMinutes = config.defaultReminderMinutes
        settings_default_reminder.setSelection(reminderType)
        custom_reminder_save.setTextColor(custom_reminder_other_val.currentTextColor)
        setupReminderPeriod(reminderMinutes)

        settings_custom_reminder_holder.beVisibleIf(reminderType == 2)
        custom_reminder_save.setOnClickListener { saveReminder() }

        settings_default_reminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, itemIndex: Int, p3: Long) {
                if (isInitialSetup)
                    settings_default_reminder.setSelection(reminderType)
                else {
                    if (itemIndex == 2) {
                        settings_custom_reminder_holder.visibility = View.VISIBLE
                        showKeyboard(custom_reminder_value)
                    } else {
                        hideKeyboard()
                        settings_custom_reminder_holder.visibility = View.GONE
                    }

                    config.defaultReminderType = itemIndex
                }
                isInitialSetup = false
            }
        }
    }

    private fun saveReminder() {
        val value = custom_reminder_value.value
        val multiplier = when (custom_reminder_other_period.selectedItemPosition) {
            1 -> HOUR_MINS
            2 -> DAY_MINS
            else -> 1
        }

        config.defaultReminderMinutes = Integer.valueOf(if (value.isEmpty()) "0" else value) * multiplier
        config.defaultReminderType = REMINDER_CUSTOM
        toast(R.string.reminder_saved)
        hideKeyboard()
    }

    private fun setupReminderPeriod(mins: Int) {
        var value = mins
        if (mins == 0) {
            custom_reminder_other_period.setSelection(0)
        } else if (mins % DAY_MINS == 0) {
            value = mins / DAY_MINS
            custom_reminder_other_period.setSelection(2)
        } else if (mins % HOUR_MINS == 0) {
            value = mins / HOUR_MINS
            custom_reminder_other_period.setSelection(1)
        } else {
            custom_reminder_other_period.setSelection(0)
        }
        custom_reminder_value.setText(value.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == GET_RINGTONE_URI) {
            val uri = intent?.getParcelableExtra<Parcelable>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) ?: return
            settings_reminder_sound.text = RingtoneManager.getRingtone(this, uri as Uri).getTitle(this)
            config.reminderSound = uri.toString()
        }
    }
}
