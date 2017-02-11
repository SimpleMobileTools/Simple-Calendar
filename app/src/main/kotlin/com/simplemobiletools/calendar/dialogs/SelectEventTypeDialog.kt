package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_radio_group.view.*
import java.util.*

class SelectEventTypeDialog(val activity: Activity, val currEventType: Int, val callback: (checkedId: Int) -> Unit) : RadioGroup.OnCheckedChangeListener {
    val dialog: AlertDialog?
    var wasInit = false
    var eventTypes = ArrayList<EventType>()
    var radioGroup: RadioGroup

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_radio_group, null)
        radioGroup = view.dialog_radio_group
        radioGroup.setOnCheckedChangeListener(this)

        DBHelper.newInstance(activity).getEventTypes {
            eventTypes = it
            activity.runOnUiThread {
                eventTypes.forEach {
                    addRadioButton(it)
                }
                wasInit = true
            }
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_event_type)
        }
    }

    private fun addRadioButton(type: EventType) {
        val radioButton = (activity.layoutInflater.inflate(R.layout.radio_button, null) as RadioButton).apply {
            text = type.title
            isChecked = type.id == currEventType
            id = type.id
        }
        radioGroup.addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (wasInit) {
            callback.invoke(checkedId)
            dialog?.dismiss()
        }
    }
}
