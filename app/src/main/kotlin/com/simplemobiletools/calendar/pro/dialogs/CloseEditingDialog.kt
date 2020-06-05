package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_return_without_saving.view.*

class CloseEditingDialog(val activity: Activity, val callback: (closeRule: Int) -> Unit) {
    val dialog: AlertDialog?

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_return_without_saving, null)

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.yes) { _, _ -> dialogConfirmed(view as ViewGroup) }
                .setNegativeButton(R.string.no, null)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun dialogConfirmed(view: ViewGroup) {
        val closeRule = when (view.close_event_radio_view.checkedRadioButtonId) {
            R.id.close_event_without_saving -> CLOSE_WITHOUT_SAVING
            else -> SAVE_AND_CLOSE
        }
        dialog?.dismiss()
        callback(closeRule)
    }
}
