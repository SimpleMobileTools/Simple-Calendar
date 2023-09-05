package com.simplemobiletools.calendar.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.databinding.DialogManageAutomaticBackupsBinding
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import java.io.File

class ManageAutomaticBackupsDialog(private val activity: SimpleActivity, onSuccess: () -> Unit) {
    private val binding by activity.viewBinding(DialogManageAutomaticBackupsBinding::inflate)
    private val config = activity.config
    private var backupFolder = config.autoBackupFolder
    private var selectedEventTypes = config.autoBackupEventTypes.ifEmpty { config.displayEventTypes }

    init {
        binding.apply {
            backupEventsFolder.setText(activity.humanizePath(backupFolder))
            val filename = config.autoBackupFilename.ifEmpty {
                "${activity.getString(R.string.events)}_%Y%M%D_%h%m%s"
            }

            backupEventsFilename.setText(filename)
            backupEventsFilenameHint.setEndIconOnClickListener {
                DateTimePatternInfoDialog(activity)
            }

            backupEventsFilenameHint.setEndIconOnLongClickListener {
                DateTimePatternInfoDialog(activity)
                true
            }

            backupEventsCheckbox.isChecked = config.autoBackupEvents
            backupEventsCheckboxHolder.setOnClickListener {
                backupEventsCheckbox.toggle()
            }

            backupTasksCheckbox.isChecked = config.autoBackupTasks
            backupTasksCheckboxHolder.setOnClickListener {
                backupTasksCheckbox.toggle()
            }

            backupPastEventsCheckbox.isChecked = config.autoBackupPastEntries
            backupPastEventsCheckboxHolder.setOnClickListener {
                backupPastEventsCheckbox.toggle()
            }

            backupEventsFolder.setOnClickListener {
                selectBackupFolder()
            }

            manageEventTypesHolder.setOnClickListener {
                SelectEventTypesDialog(activity, selectedEventTypes) {
                    selectedEventTypes = it
                }
            }
        }
        activity.getAlertDialogBuilder()
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok, null)
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, com.simplemobiletools.commons.R.string.manage_automatic_backups) { dialog ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val filename = binding.backupEventsFilename.value
                        when {
                            filename.isEmpty() -> activity.toast(com.simplemobiletools.commons.R.string.empty_name)
                            filename.isAValidFilename() -> {
                                val file = File(backupFolder, "$filename.ics")
                                if (file.exists() && !file.canWrite()) {
                                    activity.toast(com.simplemobiletools.commons.R.string.name_taken)
                                    return@setOnClickListener
                                }

                                val backupEventsChecked = binding.backupEventsCheckbox.isChecked
                                val backupTasksChecked = binding.backupTasksCheckbox.isChecked
                                if (!backupEventsChecked && !backupTasksChecked || selectedEventTypes.isEmpty()) {
                                    activity.toast(com.simplemobiletools.commons.R.string.no_entries_for_exporting)
                                    return@setOnClickListener
                                }

                                ensureBackgroundThread {
                                    config.apply {
                                        autoBackupFolder = backupFolder
                                        autoBackupFilename = filename
                                        autoBackupEvents = backupEventsChecked
                                        autoBackupTasks = backupTasksChecked
                                        autoBackupPastEntries = binding.backupPastEventsCheckbox.isChecked
                                        if (autoBackupEventTypes != selectedEventTypes) {
                                            autoBackupEventTypes = selectedEventTypes
                                        }
                                    }

                                    activity.runOnUiThread {
                                        onSuccess()
                                    }

                                    dialog.dismiss()
                                }
                            }

                            else -> activity.toast(com.simplemobiletools.commons.R.string.invalid_name)
                        }
                    }
                }
            }
    }

    private fun selectBackupFolder() {
        activity.hideKeyboard(binding.backupEventsFilename)
        FilePickerDialog(activity, backupFolder, pickFile = false, showFAB = true) { path ->
            activity.handleSAFDialog(path) { grantedSAF ->
                if (!grantedSAF) {
                    return@handleSAFDialog
                }

                activity.handleSAFDialogSdk30(path) { grantedSAF30 ->
                    if (!grantedSAF30) {
                        return@handleSAFDialogSdk30
                    }

                    backupFolder = path
                    binding.backupEventsFolder.setText(activity.humanizePath(path))
                }
            }
        }
    }
}

