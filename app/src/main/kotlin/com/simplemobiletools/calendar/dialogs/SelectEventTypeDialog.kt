package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.graphics.Color
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setBackgroundWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.dialog_select_event_type.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*
import java.util.*

class SelectEventTypeDialog(val activity: Activity, val currEventType: Int, val callback: (checkedId: Int) -> Unit) {
    val NEW_TYPE_ID = -2

    val dialog: AlertDialog?
    val radioGroup: RadioGroup
    var wasInit = false
    var eventTypes = ArrayList<EventType>()

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_event_type, null) as ViewGroup
        radioGroup = view.dialog_radio_group

        activity.dbHelper.getEventTypes {
            eventTypes = it
            activity.runOnUiThread {
                eventTypes.forEach {
                    addRadioButton(it.title, it.id, it.color)
                }
                addRadioButton(activity.getString(R.string.add_new_type), NEW_TYPE_ID, Color.TRANSPARENT)
                wasInit = true
                activity.updateTextColors(view.dialog_radio_holder)
            }
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this)
        }
    }

    private fun addRadioButton(title: String, typeId: Int, color: Int) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as RadioButton).apply {
            text = title
            isChecked = typeId == currEventType
            id = typeId
        }

        if (color != Color.TRANSPARENT)
            view.dialog_radio_color.setBackgroundWithStroke(color, activity.config.backgroundColor)

        view.setOnClickListener { viewClicked(typeId) }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    fun viewClicked(typeId: Int) {
        if (!wasInit)
            return

        if (typeId == NEW_TYPE_ID) {
            NewEventTypeDialog(activity) {
                callback.invoke(it)
                activity.hideKeyboard()
                dialog?.dismiss()
            }
        } else {
            callback.invoke(typeId)
            dialog?.dismiss()
        }
    }
}
