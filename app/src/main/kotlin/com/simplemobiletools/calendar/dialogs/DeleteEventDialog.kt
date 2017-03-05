package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.calendar.R
import com.simplemobiletools.commons.extensions.setupDialogStuff

class DeleteEventDialog(val activity: Activity, val eventIds: List<Int>, val callback: (allOccurrences: Boolean) -> Unit) : AlertDialog.Builder(activity) {
    val dialog: AlertDialog?

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_delete_event, null)

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.yes, { dialog, which -> dialogConfirmed() })
                .setNegativeButton(R.string.no, null)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback.invoke(true)
    }
}
