package com.simplemobiletools.calendar.activities

import android.os.Bundle
import android.support.v4.app.TaskStackBuilder
import android.view.View
import android.widget.AdapterView
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.*
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupDarkTheme()
        setupSundayFirst()
        setupWeekNumbers()
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

    private fun setupEventReminder() {
        val reminderType = mConfig.defaultReminderType
        val reminderMinutes = mConfig.defaultReminderMinutes
        settings_default_reminder.setSelection(when (reminderType) {
            Constants.REMINDER_OFF -> 0
            Constants.REMINDER_AT_START -> 1
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
                    0 -> Constants.REMINDER_OFF
                    1 -> Constants.REMINDER_AT_START
                    else -> Constants.REMINDER_CUSTOM
                }
            }
        }
    }

    private fun saveReminder() {
        val value = custom_reminder_value.value
        val multiplier = when (custom_reminder_other_period.selectedItemPosition) {
            1 -> Constants.HOUR_MINS
            2 -> Constants.DAY_MINS
            else -> 1
        }
        mConfig.defaultReminderMinutes = Integer.valueOf(value) * multiplier
        toast(R.string.reminder_saved)
        hideKeyboard()
    }

    private fun setupReminderPeriod(mins: Int) {
        var value = mins
        if (mins == 0) {
            custom_reminder_other_period.setSelection(0)
        } else if (mins % Constants.DAY_MINS == 0) {
            value = mins / Constants.DAY_MINS
            custom_reminder_other_period.setSelection(2)
        } else if (mins % Constants.HOUR_MINS == 0) {
            value = mins / Constants.HOUR_MINS
            custom_reminder_other_period.setSelection(1)
        } else {
            custom_reminder_other_period.setSelection(0)
        }
        custom_reminder_value.setText(value.toString())
    }

    private fun restartActivity() {
        TaskStackBuilder.create(applicationContext).addNextIntentWithParentStack(intent).startActivities()
    }
}
