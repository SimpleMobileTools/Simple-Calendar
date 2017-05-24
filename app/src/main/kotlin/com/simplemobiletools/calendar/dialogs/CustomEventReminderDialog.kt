package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.simplemobiletools.calendar.R
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_custom_event_reminder.view.*

class CustomEventReminderDialog(val activity: Activity, val selectedMinutes: Int = 0, val callback: (minutes: Int) -> Unit) : AlertDialog.Builder(activity) {
    var dialog: AlertDialog
    var view: View = (activity.layoutInflater.inflate(R.layout.dialog_custom_event_reminder, null) as ViewGroup).apply {
        if (selectedMinutes == 0) {
            dialog_radio_view.check(R.id.dialog_radio_minutes)
        } else if (selectedMinutes % 1440 == 0) {
            dialog_radio_view.check(R.id.dialog_radio_days)
            dialog_custom_reminder_value.setText((selectedMinutes / 1440).toString())
        } else if (selectedMinutes % 60 == 0) {
            dialog_radio_view.check(R.id.dialog_radio_hours)
            dialog_custom_reminder_value.setText((selectedMinutes / 60).toString())
        } else {
            dialog_radio_view.check(R.id.dialog_radio_minutes)
            dialog_custom_reminder_value.setText(selectedMinutes.toString())
        }
    }

    init {
        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialogInterface, i -> confirmReminder() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            activity.setupDialogStuff(view, this)
        }
    }

    private fun confirmReminder() {
        val value = view.dialog_custom_reminder_value.value
        val multiplier = getMultiplier(view.dialog_radio_view.checkedRadioButtonId)
        val minutes = Integer.valueOf(if (value.isEmpty()) "0" else value)
        callback.invoke(minutes * multiplier)
        activity.hideKeyboard()
        dialog.dismiss()
    }

    private fun getMultiplier(id: Int) = when (id) {
        R.id.dialog_radio_hours -> 60
        R.id.dialog_radio_days -> 1440
        else -> 1
    }
}
