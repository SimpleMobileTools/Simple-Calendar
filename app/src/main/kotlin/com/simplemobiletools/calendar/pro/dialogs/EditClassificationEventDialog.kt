package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.commons.extensions.hideKeyboard
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_edit_classification_event.view.*
import kotlinx.android.synthetic.main.dialog_edit_repeating_event.view.*

class EditClassificationEventDialog(val activity: Activity, classificationValue: String, val callback: (classificationValue: String) -> Unit) {
    var dialog: AlertDialog

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_edit_classification_event, null) as ViewGroup).apply {

        }
        val checkedId = when (classificationValue.toUpperCase()) {
            com.simplemobiletools.calendar.pro.helpers.PUBLIC -> R.id.edit_classification_event_public
            com.simplemobiletools.calendar.pro.helpers.CONFIDENTIAL -> R.id.edit_classification_event_confidential
            else -> R.id.edit_classification_event_private
        }
        view.classification_event_radio_view.check(checkedId)
        view.classification_event_radio_view.edit_classification_event_public.setOnClickListener{ viewClicked(view, R.id.edit_classification_event_public) }
        view.classification_event_radio_view.edit_classification_event_private.setOnClickListener{ viewClicked(view, R.id.edit_classification_event_private) }
        view.classification_event_radio_view.edit_classification_event_confidential.setOnClickListener{ viewClicked(view, R.id.edit_classification_event_confidential) }

        dialog = AlertDialog.Builder(activity)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        hideKeyboard()
                    }
                }
    }

    private fun viewClicked(view: ViewGroup, classificationId: Int) {
        view.classification_event_radio_view.check(classificationId)
        val classificationValue = when (classificationId) {
            R.id.edit_classification_event_private -> com.simplemobiletools.calendar.pro.helpers.PRIVATE
            R.id.edit_classification_event_public -> com.simplemobiletools.calendar.pro.helpers.PUBLIC
            else -> com.simplemobiletools.calendar.pro.helpers.CONFIDENTIAL
        }
            dialog?.dismiss()
            callback(classificationValue)
    }
}
