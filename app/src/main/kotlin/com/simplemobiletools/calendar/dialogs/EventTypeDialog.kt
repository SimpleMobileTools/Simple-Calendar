package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.commons.extensions.setBackgroundWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_event_type.view.*

class EventTypeDialog(val activity: Activity, val callback: () -> Unit) : AlertDialog.Builder(activity) {

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_event_type, null).apply {
            type_color.setBackgroundWithStroke(activity.config.primaryColor, activity.config.backgroundColor)
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            activity.setupDialogStuff(view, this, R.string.add_new_type)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({

            })
        }
    }
}
