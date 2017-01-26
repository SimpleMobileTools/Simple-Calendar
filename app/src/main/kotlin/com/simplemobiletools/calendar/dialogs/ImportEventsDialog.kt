package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.getDefaultReminderTypeIndex
import com.simplemobiletools.calendar.extensions.setupReminderPeriod
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showKeyboard
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_import_events.view.*

class ImportEventsDialog(val activity: Activity, val path: String, val callback: (success: Boolean) -> Unit) : AlertDialog.Builder(activity) {
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
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val minutes = when (view.import_events_reminder.selectedItemPosition) {
                    0 -> REMINDER_OFF
                    1 -> REMINDER_AT_START
                    else -> getReminderMinutes(view)
                }

                try {
                    Thread({
                        IcsParser.parseIcs(context, minutes, path)
                        callback.invoke(true)
                    }).start()
                } catch (e: Exception) {
                    callback.invoke(false)
                }
            })
        }
    }

    private fun getReminderMinutes(view: View): Int {
        val multiplier = when (view.import_events_custom_reminder_other_period.selectedItemPosition) {
            1 -> HOUR_MINS
            2 -> DAY_MINS
            else -> 1
        }

        val value = view.import_events_custom_reminder_value.value
        return Integer.valueOf(if (value.isEmpty()) "0" else value) * multiplier
    }
}
