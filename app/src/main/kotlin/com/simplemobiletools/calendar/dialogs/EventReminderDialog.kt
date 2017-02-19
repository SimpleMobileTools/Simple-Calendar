package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.getReminderText
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_radio_group.view.*
import java.util.*

class EventReminderDialog(val activity: Activity, val reminderMinutes: Int, val callback: (minutes: Int) -> Unit) : AlertDialog.Builder(activity),
        RadioGroup.OnCheckedChangeListener {
    val dialog: AlertDialog?
    var wasInit = false
    var minutes = TreeSet<Int>()
    var radioGroup: RadioGroup

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_radio_group, null)
        radioGroup = view.dialog_radio_group
        radioGroup.setOnCheckedChangeListener(this)

        minutes.apply {
            add(-1)
            add(0)
            add(10)
            add(30)
            add(reminderMinutes)
        }

        minutes.forEachIndexed { index, value ->
            addRadioButton(activity.getReminderText(value), value, index)
        }
        addRadioButton(activity.getString(R.string.custom), -2, minutes.size)

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_event_reminder)
        }

        wasInit = true
    }

    private fun addRadioButton(textValue: String, value: Int, index: Int) {
        val radioButton = (activity.layoutInflater.inflate(R.layout.radio_button, null) as RadioButton).apply {
            text = textValue
            isChecked = value == reminderMinutes
            id = index
        }
        radioGroup.addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        if (!wasInit)
            return

        if (checkedId == minutes.size) {
            CustomEventReminderDialog(activity) {
                callback.invoke(it)
                dialog?.dismiss()
            }
        } else {
            callback.invoke(minutes.elementAt(checkedId))
            dialog?.dismiss()
        }
    }
}
