package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.helpers.DAY
import com.simplemobiletools.calendar.pro.helpers.MONTH
import com.simplemobiletools.calendar.pro.helpers.WEEK
import com.simplemobiletools.calendar.pro.helpers.YEAR
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_custom_event_repeat_interval.view.*

class CustomEventRepeatIntervalDialog(val activity: Activity, val callback: (seconds: Int) -> Unit) {
    var dialog: AlertDialog
    var view = activity.layoutInflater.inflate(R.layout.dialog_custom_event_repeat_interval, null) as ViewGroup

    init {
        view.dialog_radio_view.check(R.id.dialog_radio_days)

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmRepeatInterval() }
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        showKeyboard(view.dialog_custom_repeat_interval_value)
                    }
                }
    }

    private fun confirmRepeatInterval() {
        val value = view.dialog_custom_repeat_interval_value.value
        val multiplier = getMultiplier(view.dialog_radio_view.checkedRadioButtonId)
        val days = Integer.valueOf(if (value.isEmpty()) "0" else value)
        callback(days * multiplier)
        activity.hideKeyboard()
        dialog.dismiss()
    }

    private fun getMultiplier(id: Int) = when (id) {
        R.id.dialog_radio_weeks -> WEEK
        R.id.dialog_radio_months -> MONTH
        R.id.dialog_radio_years -> YEAR
        else -> DAY
    }
}
