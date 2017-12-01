package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.WindowManager
import android.widget.ImageView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.setBackgroundWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.value
import kotlinx.android.synthetic.main.dialog_event_type.view.*

class UpdateEventTypeDialog(val activity: Activity, var eventType: EventType? = null, val callback: (eventTypeId: Int) -> Unit) {
    var isNewEvent = eventType == null

    init {
        if (eventType == null)
            eventType = EventType(0, "", activity.config.primaryColor)

        val view = activity.layoutInflater.inflate(R.layout.dialog_event_type, null).apply {
            setupColor(type_color)
            type_title.setText(eventType!!.title)
            type_color.setOnClickListener {
                if (eventType?.caldavCalendarId == 0) {
                    ColorPickerDialog(activity, eventType!!.color) {
                        eventType!!.color = it
                        setupColor(type_color)
                    }
                } else {
                    SelectEventTypeColorDialog(activity, eventType!!) {
                        eventType!!.color = it
                        setupColor(type_color)
                    }
                }
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            activity.setupDialogStuff(view, this, if (isNewEvent) R.string.add_new_type else R.string.edit_type) {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val title = view.type_title.value
                    val eventIdWithTitle = activity.dbHelper.getEventTypeIdWithTitle(title)
                    var isEventTypeTitleTaken = isNewEvent && eventIdWithTitle != -1
                    if (!isEventTypeTitleTaken)
                        isEventTypeTitleTaken = !isNewEvent && eventType!!.id != eventIdWithTitle && eventIdWithTitle != -1

                    if (title.isEmpty()) {
                        activity.toast(R.string.title_empty)
                        return@setOnClickListener
                    } else if (isEventTypeTitleTaken) {
                        activity.toast(R.string.type_already_exists)
                        return@setOnClickListener
                    }

                    eventType!!.title = title
                    if (eventType!!.caldavCalendarId != 0)
                        eventType!!.caldavDisplayName = title

                    val eventTypeId = if (isNewEvent) {
                        activity.dbHelper.insertEventType(eventType!!)
                    } else {
                        activity.dbHelper.updateEventType(eventType!!)
                    }

                    if (eventTypeId != -1) {
                        dismiss()
                        callback(eventTypeId)
                    } else {
                        activity.toast(R.string.editing_calendar_failed)
                    }
                }
            }
        }
    }

    private fun setupColor(view: ImageView) {
        view.setBackgroundWithStroke(eventType!!.color, activity.config.backgroundColor)
    }
}
