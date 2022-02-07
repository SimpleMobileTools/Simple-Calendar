package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.showErrorToast

class ReminderWarningDialog(val activity: Activity, val callback: () -> Unit) {
    var dialog: AlertDialog

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_reminder_warning, null)

        dialog = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNeutralButton(R.string.settings, null)
            .create().apply {
                activity.setupDialogStuff(view, this, cancelOnTouchOutside = false)
                getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    redirectToSettings()
                }
            }
    }

    private fun dialogConfirmed() {
        dialog.dismiss()
        callback()
    }

    private fun redirectToSettings() {
        activity.hideKeyboard()
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)

            try {
                activity.startActivity(this)
            } catch (e: Exception) {
                activity.showErrorToast(e)
            }
        }
    }
}
