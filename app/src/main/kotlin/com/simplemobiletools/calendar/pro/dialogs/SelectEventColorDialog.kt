package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.provider.CalendarContract.Colors
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.calDAVHelper
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.getProperBackgroundColor
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_select_event_type_color.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*

class SelectEventColorDialog(val activity: Activity, val eventType: EventType, val selectedColor: Int, val callback: (color: Int) -> Unit) {
    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false
    private val colors = activity.calDAVHelper.getAvailableCalDAVCalendarColors(eventType, Colors.TYPE_EVENT)

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_event_color, null) as ViewGroup
        radioGroup = view.dialog_select_event_type_color_radio

        addRadioButton(colorKey = colors.values.size.inc(), color = eventType.color)
        colors.forEach { (color, key) ->
            addRadioButton(key.toInt(), color)
        }

        wasInit = true
        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun addRadioButton(colorKey: Int, color: Int) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as RadioButton).apply {
            text = if (color == eventType.color) activity.getString(R.string.default_color) else String.format("#%06X", 0xFFFFFF and color)
            isChecked = color == selectedColor
            id = colorKey
        }

        view.dialog_radio_color.setFillWithStroke(color, activity.getProperBackgroundColor())
        view.setOnClickListener {
            viewClicked(color)
        }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(color: Int) {
        if (!wasInit) {
            return
        }

        callback(color)
        dialog?.dismiss()
    }
}
