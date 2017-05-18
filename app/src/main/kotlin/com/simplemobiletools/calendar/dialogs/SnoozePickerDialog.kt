package com.simplemobiletools.calendar.dialogs

import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.view.WindowManager
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_snooze_picker.view.*

class SnoozePickerDialog(val activity: SimpleActivity, val minutes: Int, val callback: (newMinutes: Int) -> Unit)
    : AlertDialog.Builder(activity) {
    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_snooze_picker, null) as ViewGroup).apply {
            snooze_picker_label.text = snooze_picker_label.text.toString().capitalize()
            snooze_picker.setText(minutes.toString())
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val value = view.snooze_picker.value
                val minutes = Integer.valueOf(if (value.isEmpty() || value == "0") "1" else value)
                callback(minutes)
                dismiss()
            })
        }
    }
}
