package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.getDefaultReminderTypeIndex
import com.simplemobiletools.calendar.extensions.setupReminderPeriod
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import kotlinx.android.synthetic.main.dialog_import_events.view.*

class ImportEventsDialog(val activity: Activity, val path: String, val callback: () -> Unit) : AlertDialog.Builder(activity) {
    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_import_events, null).apply {
            import_events_filename.text = activity.humanizePath(path)
            import_events_reminder.setSelection(context.getDefaultReminderTypeIndex())
            context.setupReminderPeriod(import_events_custom_reminder_other_period, import_events_custom_reminder_value)

            import_events_reminder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, itemIndex: Int, p3: Long) {
                    if (itemIndex == 2) {
                        import_events_custom_reminder_holder.visibility = View.VISIBLE
                        activity.showKeyboard(import_events_custom_reminder_value)
                    } else {
                        activity.hideKeyboard()
                        import_events_custom_reminder_holder.visibility = View.GONE
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.import_events)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {

            })
        }
    }
}
