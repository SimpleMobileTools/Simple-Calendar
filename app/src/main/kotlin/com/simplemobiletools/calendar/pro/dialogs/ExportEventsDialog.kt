package com.simplemobiletools.calendar.pro.dialogs

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.adapters.FilterEventTypeAdapter
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.dialog_export_events.view.*
import java.io.File
import java.util.*

class ExportEventsDialog(val activity: SimpleActivity, val path: String, val hidePath: Boolean,
                         val callback: (file: File, eventTypes: ArrayList<Long>) -> Unit) {
    private var realPath = if (path.isEmpty()) activity.internalStoragePath else path
    val config = activity.config

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_export_events, null) as ViewGroup).apply {
            export_events_folder.text = activity.humanizePath(realPath)
            export_events_filename.setText("${activity.getString(R.string.events)}_${activity.getCurrentFormattedDateTime()}")
            export_events_checkbox.isChecked = config.exportPastEvents

            if (hidePath) {
                export_events_folder_label.beGone()
                export_events_folder.beGone()
            } else {
                export_events_folder.setOnClickListener {
                    activity.hideKeyboard(export_events_filename)
                    FilePickerDialog(activity, realPath, false, showFAB = true) {
                        export_events_folder.text = activity.humanizePath(it)
                        realPath = it
                    }
                }
            }

            activity.eventsHelper.getEventTypes(activity, false) {
                val eventTypes = HashSet<String>()
                it.mapTo(eventTypes) { it.id.toString() }

                export_events_types_list.adapter = FilterEventTypeAdapter(activity, it, eventTypes)
                if (it.size > 1) {
                    export_events_pick_types.beVisible()

                    val margin = activity.resources.getDimension(R.dimen.normal_margin).toInt()
                    (export_events_checkbox.layoutParams as LinearLayout.LayoutParams).leftMargin = margin
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.export_events) {
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val filename = view.export_events_filename.value
                            when {
                                filename.isEmpty() -> activity.toast(R.string.empty_name)
                                filename.isAValidFilename() -> {
                                    val file = File(realPath, "$filename.ics")
                                    if (!hidePath && file.exists()) {
                                        activity.toast(R.string.name_taken)
                                        return@setOnClickListener
                                    }

                                    ensureBackgroundThread {
                                        config.lastExportPath = file.absolutePath.getParentPath()
                                        config.exportPastEvents = view.export_events_checkbox.isChecked

                                        val eventTypes = (view.export_events_types_list.adapter as FilterEventTypeAdapter).getSelectedItemsList()
                                        callback(file, eventTypes)
                                        dismiss()
                                    }
                                }
                                else -> activity.toast(R.string.invalid_name)
                            }
                        }
                    }
                }
    }
}
