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

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_radio_group, null)
        val radioGroup = view.dialog_radio_group
        radioGroup.setOnCheckedChangeListener(this)

        DBHelper.newInstance(activity).getEventTypes {
            activity.runOnUiThread {
                eventTypes = it
                eventTypes.forEach {
                    val radioButton = activity.layoutInflater.inflate(R.layout.radio_button, null) as RadioButton
                    radioButton.apply {
                        text = it.title
                        isChecked = it.id == currEventType
                        id = it.id
                    }
                    radioGroup.addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                }
                wasInit = true
            }
        }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_event_type)
        }
    }

    override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
        if (wasInit) {
            callback.invoke(checkedId)
            dialog?.dismiss()
        }
    }
}
