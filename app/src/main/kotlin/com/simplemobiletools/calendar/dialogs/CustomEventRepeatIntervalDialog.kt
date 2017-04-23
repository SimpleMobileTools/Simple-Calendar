package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.view.WindowManager
import com.simplemobiletools.calendar.R
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_custom_event_repeat_interval.view.*

class CustomEventRepeatIntervalDialog(val activity: Activity, val callback: (seconds: Int) -> Unit) : AlertDialog.Builder(activity) {
    var dialog: AlertDialog
    var view = activity.layoutInflater.inflate(R.layout.dialog_custom_event_repeat_interval, null) as ViewGroup

    init {
        view.dialog_radio_view.check(R.id.dialog_radio_days)

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialogInterface, i -> confirmRepeatInterval() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            activity.setupDialogStuff(view, this)
        }
    }

    private fun confirmRepeatInterval() {
        val value = view.dialog_custom_repeat_interval_value.value
        val multiplier = getMultiplier(view.dialog_radio_view.checkedRadioButtonId)
        val days = Integer.valueOf(if (value.isEmpty()) "0" else value)
        callback.invoke(days * multiplier * 24 * 60 * 60)
        activity.hideKeyboard()
        dialog.dismiss()
    }

    private fun getMultiplier(id: Int) = when (id) {
        R.id.dialog_radio_weeks -> 7
        R.id.dialog_radio_months -> 30
        R.id.dialog_radio_years -> 365
        else -> 1
    }
}
