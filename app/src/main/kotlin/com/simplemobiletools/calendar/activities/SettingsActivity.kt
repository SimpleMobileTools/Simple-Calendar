package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.TaskStackBuilder
import android.view.View
import android.widget.AdapterView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.beVisibleIf
import com.simplemobiletools.calendar.extensions.hideKeyboard
import com.simplemobiletools.calendar.extensions.showKeyboard
import com.simplemobiletools.calendar.extensions.value
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.filepicker.extensions.toast
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    val GET_RINGTONE_URI = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupDarkTheme()
        setupSundayFirst()
        setupWeekNumbers()
        setupVibrate()
        setupReminderSound()
        setupEventReminder()
    }

    private fun setupDarkTheme() {
        settings_dark_theme.isChecked = mConfig.isDarkTheme
        settings_dark_theme_holder.setOnClickListener {
            settings_dark_theme.toggle()
            mConfig.isDarkTheme = settings_dark_theme.isChecked
            restartActivity()
        }
    }

    private fun setupSundayFirst() {
        settings_sunday_first.isChecked = mConfig.isSundayFirst
        settings_sunday_first_holder.setOnClickListener {
            settings_sunday_first.toggle()
            mConfig.isSundayFirst = settings_sunday_first.isChecked
        }
    }

    private fun setupWeekNumbers() {
        settings_week_numbers.isChecked = mConfig.displayWeekNumbers
        settings_week_numbers_holder.setOnClickListener {
            settings_week_numbers.toggle()
            mConfig.displayWeekNumbers = settings_week_numbers.isChecked
        }
    }

    private fun setupReminderSound() {
        settings_reminder_sound.text = RingtoneManager.getRingtone(this, Uri.parse(mConfig.reminderSound)).getTitle(this)
        settings_reminder_sound_holder.setOnClickListener {
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, resources.getString(R.string.notification_sound))
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(mConfig.reminderSound))

                if (resolveActivity(packageManager) != null)
                    startActivityForResult(this, GET_RINGTONE_URI)
                else {
                    toast(R.string.no_ringtone_picker)
                }
            }
        }
    }

    private fun setupVibrate() {
        settings_vibrate.isChecked = mConfig.vibrateOnReminder
        settings_vibrate_holder.setOnClickListener {
            settings_vibrate.toggle()
            mConfig.vibrateOnReminder = settings_vibrate.isChecked
        }
    }

    private fun setupEventReminder() {
        val reminderType = mConfig.defaultReminderType
        val reminderMinutes = mConfig.defaultReminderMinutes
        settings_default_reminder.setSelection(when (reminderType) {
            REMINDER_OFF -> 0
            REMINDER_AT_START -> 1
            else -> 2
        })
        custom_reminder_save.setTextColor(custom_reminder_other_val.currentTextColor)
        setupReminderPeriod(reminderMinutes)
        settings_custom_reminder_holder.beVisibleIf(reminderType == 2)
        custom_reminder_save.setOnClickListener { saveReminder() }

        settings_default_reminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, itemIndex: Int, p3: Long) {
                if (itemIndex == 2) {
                    settings_custom_reminder_holder.visibility = View.VISIBLE
                    showKeyboard(custom_reminder_value)
                } else {
                    hideKeyboard()
                    settings_custom_reminder_holder.visibility = View.GONE
                }

                mConfig.defaultReminderType = when (itemIndex) {
                    0 -> REMINDER_OFF
                    1 -> REMINDER_AT_START
                    else -> REMINDER_CUSTOM
                }
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

        mConfig.defaultReminderMinutes = Integer.valueOf(if (value.isEmpty()) "0" else value) * multiplier
        mConfig.defaultReminderType = REMINDER_CUSTOM
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
            mConfig.reminderSound = uri.toString()
        }
    }

    private fun restartActivity() {
        TaskStackBuilder.create(applicationContext).addNextIntentWithParentStack(intent).startActivities()
    }
}
