package com.simplemobiletools.calendar.pro.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_edit_repeating_event.view.*

class EditRepeatingEventDialog(val activity: SimpleActivity, val isTask: Boolean = false, val callback: (allOccurrences: Int) -> Unit) {
    private var dialog: AlertDialog? = null

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_edit_repeating_event, null) as ViewGroup).apply {
            edit_repeating_event_one_only.setOnClickListener { sendResult(0) }
            edit_repeating_event_this_and_future_occurences.setOnClickListener { sendResult(1) }
            edit_repeating_event_all_occurrences.setOnClickListener { sendResult(2) }

            if (isTask) {
                edit_repeating_event_title.setText(R.string.task_is_repeatable)
            } else {
                edit_repeating_event_title.setText(R.string.event_is_repeatable)
            }
        }

        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.hideKeyboard()
                }
            }
    }

    private fun sendResult(allOccurrences: Int) {
        callback(allOccurrences)
        dialog?.dismiss()
    }
}
