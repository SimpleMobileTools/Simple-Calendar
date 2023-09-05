package com.simplemobiletools.calendar.pro.dialogs

import android.view.View
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.databinding.DialogSetRemindersBinding
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.ANNIVERSARY_EVENT
import com.simplemobiletools.calendar.pro.helpers.BIRTHDAY_EVENT
import com.simplemobiletools.calendar.pro.helpers.OTHER_EVENT
import com.simplemobiletools.calendar.pro.helpers.REMINDER_OFF
import com.simplemobiletools.commons.dialogs.PermissionRequiredDialog
import com.simplemobiletools.commons.extensions.*

class SetRemindersDialog(val activity: SimpleActivity, val eventType: Int, val callback: (reminders: ArrayList<Int>) -> Unit) {
    private var mReminder1Minutes = REMINDER_OFF
    private var mReminder2Minutes = REMINDER_OFF
    private var mReminder3Minutes = REMINDER_OFF
    private var isAutomatic = false

    private val binding by activity.viewBinding(DialogSetRemindersBinding::inflate)

    init {
        binding.apply {
            setRemindersImage.applyColorFilter(activity.getProperTextColor())
            setReminders1.text = activity.getFormattedMinutes(mReminder1Minutes)
            setReminders2.text = activity.getFormattedMinutes(mReminder1Minutes)
            setReminders3.text = activity.getFormattedMinutes(mReminder1Minutes)

            setReminders1.setOnClickListener {
                activity.handleNotificationPermission { granted ->
                    if (granted) {
                        activity.showPickSecondsDialogHelper(mReminder1Minutes, showDuringDayOption = true) {
                            mReminder1Minutes = if (it == -1 || it == 0) it else it / 60
                            setReminders1.text = activity.getFormattedMinutes(mReminder1Minutes)
                            if (mReminder1Minutes != REMINDER_OFF) {
                                setReminders2.beVisible()
                            }
                        }
                    } else {
                        PermissionRequiredDialog(
                            activity = activity,
                            textId = com.simplemobiletools.commons.R.string.allow_notifications_reminders,
                            positiveActionCallback = { activity.openNotificationSettings() }
                        )
                    }
                }
            }

            setReminders2.setOnClickListener {
                activity.showPickSecondsDialogHelper(mReminder2Minutes, showDuringDayOption = true) {
                    mReminder2Minutes = if (it == -1 || it == 0) it else it / 60
                    setReminders2.text = activity.getFormattedMinutes(mReminder2Minutes)
                    if (mReminder2Minutes != REMINDER_OFF) {
                        setReminders3.beVisible()
                    }
                }
            }

            setReminders3.setOnClickListener {
                activity.showPickSecondsDialogHelper(mReminder3Minutes, showDuringDayOption = true) {
                    mReminder3Minutes = if (it == -1 || it == 0) it else it / 60
                    setReminders3.text = activity.getFormattedMinutes(mReminder3Minutes)
                }
            }

            addEventAutomaticallyCheckbox.apply {
                visibility = if (eventType == OTHER_EVENT) View.GONE else View.VISIBLE
                text = when (eventType) {
                    BIRTHDAY_EVENT -> activity.getString(R.string.add_birthdays_automatically)
                    ANNIVERSARY_EVENT -> activity.getString(R.string.add_anniversaries_automatically)
                    else -> ""
                }
                isChecked = when (eventType) {
                    BIRTHDAY_EVENT -> activity.config.addBirthdaysAutomatically
                    ANNIVERSARY_EVENT -> activity.config.addAnniversariesAutomatically
                    else -> false
                }
                isAutomatic = isChecked
                setOnCheckedChangeListener { _, isChecked -> isAutomatic = isChecked }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok) { _, _ -> dialogConfirmed() }
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.event_reminders)
            }
    }

    private fun dialogConfirmed() {
        val tempReminders = arrayListOf(mReminder1Minutes, mReminder2Minutes, mReminder3Minutes).filter { it != REMINDER_OFF }.sorted()
        val reminders = arrayListOf(
            tempReminders.getOrNull(0) ?: REMINDER_OFF,
            tempReminders.getOrNull(1) ?: REMINDER_OFF,
            tempReminders.getOrNull(2) ?: REMINDER_OFF
        )

        if (eventType == BIRTHDAY_EVENT) {
            activity.config.addBirthdaysAutomatically = isAutomatic
            activity.config.birthdayReminders = reminders
        }

        if (eventType == ANNIVERSARY_EVENT) {
            activity.config.addAnniversariesAutomatically = isAutomatic
            activity.config.anniversaryReminders = reminders
        }

        callback(reminders)
    }
}
