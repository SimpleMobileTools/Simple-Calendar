package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_change_views.view.*

class EventReminderDialog(val activity: Activity, val defaultMinutes: Int, val callback: (mins: Int) -> Unit) : AlertDialog.Builder(activity), RadioGroup.OnCheckedChangeListener {
    val dialog: AlertDialog?

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_event_reminder, null).dialog_radio_view.apply {
            check(getCheckedItem())
            setOnCheckedChangeListener(this@EventReminderDialog)
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_event_reminder)
        }
    }

    private fun getCheckedItem() = when (defaultMinutes) {
        -1 -> R.id.dialog_radio_no_reminder
        0 -> R.id.dialog_radio_at_start
        10 -> R.id.dialog_radio_mins_before_10
        30 -> R.id.dialog_radio_mins_before_30
        else -> R.id.dialog_radio_custom
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        if (checkedId == R.id.dialog_radio_custom) {
            CustomEventReminderDialog(activity) {
                callback.invoke(it)
                dialog?.dismiss()
            }
        } else {
            callback.invoke(getSelectionValue(checkedId))
            dialog?.dismiss()
        }
    }

    private fun getSelectionValue(id: Int) = when (id) {
        R.id.dialog_radio_at_start -> 0
        R.id.dialog_radio_mins_before_10 -> 10
        R.id.dialog_radio_mins_before_30 -> 30
        else -> -1
    }
}
