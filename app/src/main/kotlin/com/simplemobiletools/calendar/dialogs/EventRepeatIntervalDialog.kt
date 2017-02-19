package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.getRepetitionText
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_radio_group.view.*
import java.util.*

class EventRepeatIntervalDialog(val activity: Activity, val repeatSeconds: Int, val callback: (mins: Int) -> Unit) : AlertDialog.Builder(activity),
        RadioGroup.OnCheckedChangeListener {
    val dialog: AlertDialog?
    var wasInit = false
    var seconds = TreeSet<Int>()
    var radioGroup: RadioGroup

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_radio_group, null)
        radioGroup = view.dialog_radio_group
        radioGroup.setOnCheckedChangeListener(this)

        seconds.apply {
            add(0)
            add(DAY)
            add(WEEK)
            add(BIWEEK)
            add(MONTH)
            add(YEAR)
            add(repeatSeconds)
        }

        seconds.forEachIndexed { index, value ->
            addRadioButton(activity.getRepetitionText(value), value, index)
        }
        addRadioButton(activity.getString(R.string.custom), -1, seconds.size)

        dialog = AlertDialog.Builder(activity)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_repeat_interval)
        }

        wasInit = true
    }

    private fun addRadioButton(textValue: String, value: Int, index: Int) {
        val radioButton = (activity.layoutInflater.inflate(R.layout.radio_button, null) as RadioButton).apply {
            text = textValue
            isChecked = value == repeatSeconds
            id = index
        }
        radioGroup.addView(radioButton, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        if (!wasInit)
            return

        if (checkedId == seconds.size) {
            CustomEventRepeatIntervalDialog(activity) {
                callback.invoke(it)
                dialog?.dismiss()
            }
        } else {
            callback.invoke(seconds.elementAt(checkedId))
            dialog?.dismiss()
        }
    }
}
