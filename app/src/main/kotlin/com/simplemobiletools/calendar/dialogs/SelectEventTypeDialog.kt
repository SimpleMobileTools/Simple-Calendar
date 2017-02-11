package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.dialog_radio_group.view.*
import java.util.*

class SelectEventTypeDialog(val activity: Activity, val currEventType: Int, val callback: (checkedId: Int) -> Unit) : RadioGroup.OnCheckedChangeListener {
    val NEW_TYPE_ID = -2

    val dialog: AlertDialog?
    var wasInit = false
    var eventTypes = ArrayList<EventType>()
    var radioGroup: RadioGroup

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_radio_group, null) as ViewGroup
        radioGroup = view.dialog_radio_group
        radioGroup.setOnCheckedChangeListener(this)

        DBHelper.newInstance(activity).getEventTypes {
            eventTypes = it
            activity.runOnUiThread {
                eventTypes.forEach {
                    addRadioButton(it.title, it.id)
                }
                addRadioButton(activity.getString(R.string.add_new_type), NEW_TYPE_ID)
                wasInit = true
                activity.updateTextColors(view.dialog_radio_holder)
            }
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_event_type)
        }
    }

    private fun addRadioButton(title: String, typeId: Int) {
        val radioButton = (activity.layoutInflater.inflate(R.layout.radio_button, null) as RadioButton).apply {
            text = title
            isChecked = typeId == currEventType
            id = typeId
        }
        radioGroup.addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (!wasInit)
            return

        if (checkedId == NEW_TYPE_ID) {
            NewEventTypeDialog(activity) {
                callback.invoke(it)
                activity.hideKeyboard()
                dialog?.dismiss()
            }
        } else {
            callback.invoke(checkedId)
            dialog?.dismiss()
        }
    }
}
