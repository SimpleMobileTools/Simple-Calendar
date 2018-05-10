package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.graphics.Color
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.CalDAVHandler
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_select_radio_group.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*

class SelectEventTypeColorDialog(val activity: Activity, val eventType: EventType, val callback: (color: Int) -> Unit) {
    private val dialog: AlertDialog?
    private val radioGroup: RadioGroup
    private var wasInit = false
    private val colors = CalDAVHandler(activity.applicationContext).getAvailableCalDAVCalendarColors(eventType)

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_radio_group, null) as ViewGroup
        radioGroup = view.dialog_radio_group

        colors.forEachIndexed { index, value ->
            addRadioButton(index, value)
        }

        wasInit = true
        dialog = AlertDialog.Builder(activity)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun addRadioButton(colorKey: Int, color: Int) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as RadioButton).apply {
            text = String.format("#%06X", 0xFFFFFF and color)
            isChecked = color == eventType.color
            id = colorKey
        }

        if (color != Color.TRANSPARENT)
            view.dialog_radio_color.setFillWithStroke(color, activity.config.backgroundColor)

        view.setOnClickListener { viewClicked(colorKey) }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(colorKey: Int) {
        if (!wasInit)
            return

        callback(colors[colorKey])
        dialog?.dismiss()
    }
}
