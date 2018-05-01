package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.widget.ImageView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_event_type.view.*

class UpdateEventTypeDialog(val activity: Activity, var eventType: EventType? = null, val callback: (eventType: EventType) -> Unit) {
    var isNewEvent = eventType == null

    init {
        if (eventType == null)
            eventType = EventType(0, "", activity.config.primaryColor)

        val view = activity.layoutInflater.inflate(R.layout.dialog_event_type, null).apply {
            setupColor(type_color)
            type_title.setText(eventType!!.title)
            type_color.setOnClickListener {
                if (eventType?.caldavCalendarId == 0) {
                    ColorPickerDialog(activity, eventType!!.color) { wasPositivePressed, color ->
                        if (wasPositivePressed) {
                            eventType!!.color = color
                            setupColor(type_color)
                        }
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
                    activity.setupDialogStuff(view, this, if (isNewEvent) R.string.add_new_type else R.string.edit_type) {
                        showKeyboard(view.type_title)
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

                            eventType!!.id = if (isNewEvent) {
                                activity.dbHelper.insertEventType(eventType!!)
                            } else {
                                activity.dbHelper.updateEventType(eventType!!)
                            }

                            if (eventType!!.id != -1) {
                                dismiss()
                                callback(eventType!!)
                            } else {
                                activity.toast(R.string.editing_calendar_failed)
                            }
                        }
                    }
                }
    }

    private fun setupColor(view: ImageView) {
        view.setFillWithStroke(eventType!!.color, activity.config.backgroundColor)
    }
}
