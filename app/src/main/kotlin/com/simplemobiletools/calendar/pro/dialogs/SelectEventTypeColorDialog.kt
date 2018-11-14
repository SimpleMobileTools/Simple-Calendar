package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.calDAVHelper
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_select_event_type_color.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*

class SelectEventTypeColorDialog(val activity: Activity, val eventType: EventType, val callback: (color: Int) -> Unit) {
    private val dialog: AlertDialog?
    private val radioGroup: RadioGroup
    private var wasInit = false
    private val colors = activity.calDAVHelper.getAvailableCalDAVCalendarColors(eventType)

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_event_type_color, null) as ViewGroup
        radioGroup = view.dialog_select_event_type_color_radio
        view.dialog_select_event_type_other_value.setOnClickListener {
            showCustomColorPicker()
        }

        colors.forEachIndexed { index, value ->
            addRadioButton(index, value)
        }

        wasInit = true
        dialog = AlertDialog.Builder(activity)
                .create().apply {
                    activity.setupDialogStuff(view, this)

                    if (colors.isEmpty()) {
                        showCustomColorPicker()
                    }
                }
    }

    private fun addRadioButton(colorKey: Int, color: Int) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as RadioButton).apply {
            text = if (color == 0) activity.getString(R.string.transparent) else String.format("#%06X", 0xFFFFFF and color)
            isChecked = color == eventType.color
            id = colorKey
        }

        view.dialog_radio_color.setFillWithStroke(color, activity.config.backgroundColor)
        view.setOnClickListener {
            viewClicked(colorKey)
        }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(colorKey: Int) {
        if (!wasInit)
            return

        callback(colors[colorKey])
        dialog?.dismiss()
    }

    private fun showCustomColorPicker() {
        ColorPickerDialog(activity, activity.config.primaryColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                callback(color)
            }
            dialog?.dismiss()
        }
    }
}
