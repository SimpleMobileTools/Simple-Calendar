package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.CONFIDENTIAL
import com.simplemobiletools.calendar.pro.helpers.PRIVATE
import com.simplemobiletools.calendar.pro.helpers.PUBLIC
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyCompatRadioButton
import kotlinx.android.synthetic.main.dialog_select_radio_group.view.*
import kotlinx.android.synthetic.main.radio_button_with_color.view.*

class SelectClassificationDialog(val activity: Activity, val currClassification: String, val callback: (classification: String) -> Unit) {
    private val dialog: AlertDialog?
    private val radioGroup: RadioGroup

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_select_radio_group, null) as ViewGroup
        radioGroup = view.dialog_radio_group
        addRadioButton(PUBLIC, R.id.edit_classification_event_public)
        addRadioButton(PRIVATE, R.id.edit_classification_event_private)
        addRadioButton(CONFIDENTIAL, R.id.edit_classification_event_confidential)
        dialog = AlertDialog.Builder(activity)
                .create().apply {
                    activity.setupDialogStuff(view, this)
                }
    }

    private fun addRadioButton(classification: String, classificationId: Int) {
        val view = activity.layoutInflater.inflate(R.layout.radio_button_with_color, null)
        (view.dialog_radio_button as MyCompatRadioButton).apply {
            text = classification
            isChecked = classification.compareTo(currClassification) == 0
            id = classificationId
        }

        view.setOnClickListener { viewClicked(classificationId) }
        radioGroup.addView(view, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(classificationId: Int) {
        radioGroup.check(classificationId)
        val classificationValue = when (classificationId) {
            R.id.edit_classification_event_private -> PRIVATE
            R.id.edit_classification_event_public -> PUBLIC
            else -> CONFIDENTIAL
        }
        dialog?.dismiss()
        callback(classificationValue)
    }
}
