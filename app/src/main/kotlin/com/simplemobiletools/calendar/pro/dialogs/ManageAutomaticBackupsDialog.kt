package com.simplemobiletools.calendar.pro.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import kotlinx.android.synthetic.main.dialog_manage_automatic_backups.view.*

class ManageAutomaticBackupsDialog(private val activity: SimpleActivity, onSuccess: (() -> Unit)? = null, onCancel: (() -> Unit)? = null) {
    private val view = (activity.layoutInflater.inflate(R.layout.dialog_manage_automatic_backups, null) as ViewGroup)
    private val config = activity.config
    private var realPath = config.autoBackupPath
    private var selectedEventTypes = config.autoBackupEventTypes

    init {
        view.apply {
            backup_events_folder.setText(
                if (realPath.isEmpty()) {
                    activity.getString(R.string.select_folder)
                } else {
                    activity.humanizePath(realPath)
                }
            )

            val filename = config.autoBackupFilename.ifEmpty {
                "${activity.getString(R.string.events)}_%Y%M%D_%h%m%s"
            }
            backup_events_filename.setText(filename)
            backup_events_filename_hint.setEndIconOnClickListener {
                DateTimePatternInfoDialog(activity)
            }
            backup_events_filename_hint.setEndIconOnLongClickListener {
                DateTimePatternInfoDialog(activity)
                true
            }

            backup_events_checkbox.isChecked = config.autoBackupEvents
            backup_events_checkbox_holder.setOnClickListener {
                backup_events_checkbox.toggle()
            }
            backup_tasks_checkbox.isChecked = config.autoBackupTasks
            backup_tasks_checkbox_holder.setOnClickListener {
                backup_tasks_checkbox.toggle()
            }
            backup_past_events_checkbox.isChecked = config.autoBackupPastEntries
            backup_past_events_checkbox_holder.setOnClickListener {
                backup_past_events_checkbox.toggle()
            }

            backup_events_folder.setOnClickListener {
                selectBackupFolder()
            }

            manage_event_types_holder.setOnClickListener {
                SelectEventTypesDialog(activity, selectedEventTypes) {
                    selectedEventTypes = it
                }
            }
        }
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.manage_automatic_backups) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (realPath.isEmpty()) {
                            activity.toast(R.string.select_folder)
                            selectBackupFolder()
                            return@setOnClickListener
                        }

                        val filename = view.backup_events_filename.value
                        when {
                            filename.isEmpty() -> activity.toast(R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val backupEventsChecked = view.backup_events_checkbox.isChecked
                                val backupTasksChecked = view.backup_tasks_checkbox.isChecked
                                if (!backupEventsChecked && !backupTasksChecked || selectedEventTypes.isEmpty()) {
                                    activity.toast(R.string.no_entries_for_exporting)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.apply {
                                        autoBackupPath = realPath
                                        autoBackupFilename = filename
                                        autoBackupEvents = backupEventsChecked
                                        autoBackupTasks = backupTasksChecked
                                        autoBackupPastEntries = view.backup_past_events_checkbox.isChecked
                                        if (autoBackupEventTypes != selectedEventTypes) {
                                            autoBackupEventTypes = selectedEventTypes
                                        }
                                    }

                                    onSuccess?.invoke()
                                    alertDialog.dismiss()
                                }
                            }
                            else -> activity.toast(R.string.invalid_name)
                        }
                    }

                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                        onCancel?.invoke()
                        alertDialog.dismiss()
                    }
                }
            }
    }

    private fun selectBackupFolder() {
        activity.hideKeyboard(view.backup_events_filename)
        FilePickerDialog(activity, realPath, false, showFAB = true) {
            view.backup_events_folder.setText(activity.humanizePath(it))
            realPath = it
        }
    }
}

