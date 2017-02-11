package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.setBackgroundWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_event_type.view.*

class EventTypeDialog(val activity: Activity, val callback: () -> Unit) : AlertDialog.Builder(activity) {
    var currentColor = 0

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_event_type, null).apply {
            currentColor = activity.config.primaryColor
            setupColor(type_color)
            type_color.setOnClickListener {
                ColorPickerDialog(activity, currentColor) {
                    currentColor = it
                    setupColor(type_color)
                }
            }
        }

        val db = DBHelper.newInstance(activity)
        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            activity.setupDialogStuff(view, this, R.string.add_new_type)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                val title = view.type_title.value
                if (title.isEmpty()) {
                    activity.toast(R.string.title_empty)
                    return@setOnClickListener
                } else if (db.doesEventTypeExist(title)) {
                    activity.toast(R.string.type_already_exists)
                    return@setOnClickListener
                }

                val eventType = EventType(0, title, currentColor)
                if (db.addEventType(eventType)) {
                    dismiss()
                    callback.invoke()
                } else {
                    activity.toast(R.string.unknown_error_occurred)
                }
            })
        }
    }

    private fun setupColor(view: ImageView) {
        view.setBackgroundWithStroke(currentColor, activity.config.backgroundColor)
    }
}
