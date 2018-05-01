package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.graphics.Color
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.views.MyCompatRadioButton
import kotlinx.android.synthetic.main.dialog_select_radio_group.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*
import java.util.*

class SelectEventTypeDialog(val activity: Activity, val currEventType: Int, val showCalDAVCalendars: Boolean,
                            val callback: (eventType: EventType) -> Unit) {
    private val NEW_TYPE_ID = -2

    private val dialog: AlertDialog?
    private val radioGroup: RadioGroup
    private var wasInit = false
    private var eventTypes = ArrayList<EventType>()

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_radio_group, null) as ViewGroup
        radioGroup = view.dialog_radio_group

        activity.dbHelper.getEventTypes {
            eventTypes = it
            activity.runOnUiThread {
                eventTypes.filter { showCalDAVCalendars || it.caldavCalendarId == 0 }.forEach {
                    addRadioButton(it)
                }
                val newEventType = EventType(NEW_TYPE_ID, activity.getString(R.string.add_new_type), Color.TRANSPARENT, 0)
                addRadioButton(newEventType)
                wasInit = true
                activity.updateTextColors(view.dialog_radio_holder)
            }
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun addRadioButton(eventType: EventType) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as MyCompatRadioButton).apply {
            text = eventType.getDisplayTitle()
            isChecked = eventType.id == currEventType
            id = eventType.id
        }

        if (eventType.color != Color.TRANSPARENT) {
            view.dialog_radio_color.setFillWithStroke(eventType.color, activity.config.backgroundColor)
        }

        view.setOnClickListener { viewClicked(eventType) }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(eventType: EventType) {
        if (!wasInit) {
            return
        }

        if (eventType.id == NEW_TYPE_ID) {
            UpdateEventTypeDialog(activity) {
                callback(it)
                activity.hideKeyboard()
                dialog?.dismiss()
            }
        } else {
            callback(eventType)
            dialog?.dismiss()
        }
    }
}
