package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.helpers.DELETE_ALL_OCCURRENCES
import com.simplemobiletools.calendar.pro.helpers.DELETE_FUTURE_OCCURRENCES
import com.simplemobiletools.calendar.pro.helpers.DELETE_SELECTED_OCCURRENCE
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_delete_event.view.*

class DeleteEventDialog(val activity: Activity, eventIds: List<Long>, hasRepeatableEvent: Boolean, val callback: (deleteRule: Int) -> Unit) {
    val dialog: AlertDialog?

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_delete_event, null).apply {
            delete_event_repeat_description.beVisibleIf(hasRepeatableEvent)
            delete_event_radio_view.beVisibleIf(hasRepeatableEvent)
            if (!hasRepeatableEvent) {
                delete_event_radio_view.check(R.id.delete_event_all)
            }

            if (eventIds.size > 1) {
                delete_event_repeat_description.text = resources.getString(R.string.selection_contains_repetition)
            }
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.yes) { dialog, which -> dialogConfirmed(view as ViewGroup) }
                .setNegativeButton(R.string.no, null)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun dialogConfirmed(view: ViewGroup) {
        val deleteRule = when (view.delete_event_radio_view.checkedRadioButtonId) {
            R.id.delete_event_all -> DELETE_ALL_OCCURRENCES
            R.id.delete_event_future -> DELETE_FUTURE_OCCURRENCES
            else -> DELETE_SELECTED_OCCURRENCE
        }
        dialog?.dismiss()
        callback(deleteRule)
    }
}
