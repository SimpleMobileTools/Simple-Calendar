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

class SelectClassificationDialog(val activity: Activity, currClassification: String, val callback: (classification: String) -> Unit) {
    init {
        EditClassificationEventDialog(activity, currClassification, callback)
    }
}
