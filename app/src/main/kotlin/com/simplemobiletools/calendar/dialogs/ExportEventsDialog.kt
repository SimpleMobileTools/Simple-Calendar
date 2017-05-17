package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_export_events.view.*
import java.io.File

class ExportEventsDialog(val activity: Activity, val path: String, val callback: (exportPastEvents: Boolean, file: File) -> Unit) : AlertDialog.Builder(activity) {
    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_export_events, null) as ViewGroup).apply {
            export_events_folder.text = activity.humanizePath(path)
            export_events_filename.setText("events_${System.currentTimeMillis() / 1000}")
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.export_events)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val filename = view.export_events_filename.value
                if (filename.isEmpty()) {
                    context.toast(R.string.empty_name)
                } else if (filename.isAValidFilename()) {
                    val file = File(path, "$filename.ics")
                    if (file.exists()) {
                        context.toast(R.string.name_taken)
                        return@setOnClickListener
                    }

                    callback(view.export_events_checkbox.isChecked, file)
                    dismiss()
                } else {
                    context.toast(R.string.invalid_name)
                }
            })
        }
    }
}
