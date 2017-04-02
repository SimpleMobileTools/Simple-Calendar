package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_export_events.view.*

class ExportEventsDialog(val activity: Activity, val path: String, val callback: (refreshView: Boolean) -> Unit) : AlertDialog.Builder(activity) {
    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_export_events, null) as ViewGroup).apply {
            export_events_folder.text = activity.humanizePath(path)
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.export_events)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({

            })
        }
    }
}
