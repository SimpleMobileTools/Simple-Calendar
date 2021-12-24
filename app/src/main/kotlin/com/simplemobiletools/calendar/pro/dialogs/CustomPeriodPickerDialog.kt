package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_custom_period_picker.view.*

class CustomPeriodPickerDialog(val activity: Activity, val initialValue: Int?, val initialType: Int?, val callback: (value: Int, selectedType: Int) -> Unit) {
    var dialog: AlertDialog
    var view = (activity.layoutInflater.inflate(R.layout.dialog_custom_period_picker, null) as ViewGroup)

    init {
        view.dialog_custom_period_value.setText(initialValue?.toString() ?: "")
        view.dialog_radio_view.check(initialType ?: R.id.dialog_radio_days)
        dialog = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { dialogInterface, i -> confirmReminder() }
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this) {
                    showKeyboard(view.dialog_custom_period_value)
                }
            }
    }

    private fun confirmReminder() {
        val value = view.dialog_custom_period_value.value
        val type = view.dialog_radio_view.checkedRadioButtonId
        val period = Integer.valueOf(if (value.isEmpty()) "0" else value)
        callback(period, type)
        activity.hideKeyboard()
        dialog.dismiss()
    }
}
