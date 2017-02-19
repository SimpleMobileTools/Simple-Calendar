package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.IcsParser
import com.simplemobiletools.calendar.helpers.IcsParser.ImportResult.*
import com.simplemobiletools.commons.extensions.humanizePath
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import kotlinx.android.synthetic.main.dialog_import_events.view.*

class ImportEventsDialog(val activity: Activity, val path: String, val callback: (refreshView: Boolean) -> Unit) : AlertDialog.Builder(activity) {

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_import_events, null).apply {
            import_events_filename.text = activity.humanizePath(path)
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.import_events)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                Thread({
                    val result = IcsParser().parseIcs(context, path)
                    handleParseResult(result)
                    dismiss()
                }).start()
            })
        }
    }

    private fun handleParseResult(result: IcsParser.ImportResult) {
        activity.runOnUiThread {
            activity.toast(when (result) {
                IMPORT_OK -> R.string.events_imported_successfully
                IMPORT_PARTIAL -> R.string.importing_some_events_failed
                else -> R.string.importing_events_failed
            })
            callback.invoke(result != IMPORT_FAIL)
        }
    }
}
