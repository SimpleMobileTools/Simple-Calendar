package com.simplemobiletools.calendar.pro.dialogs

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.ManageEventTypesActivity
import com.simplemobiletools.calendar.pro.databinding.DialogSelectEventTypeBinding
import com.simplemobiletools.calendar.pro.databinding.RadioButtonWithColorBinding
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.extensions.*

class SelectEventTypeDialog(
    val activity: Activity, val currEventType: Long, val showCalDAVCalendars: Boolean, val showNewEventTypeOption: Boolean,
    val addLastUsedOneAsFirstOption: Boolean, val showOnlyWritable: Boolean, var showManageEventTypes: Boolean, val callback: (eventType: EventType) -> Unit
) {
    private val NEW_EVENT_TYPE_ID = -2L
    private val LAST_USED_EVENT_TYPE_ID = -1L

    private var dialog: AlertDialog? = null
    private val radioGroup: RadioGroup
    private var wasInit = false
    private var eventTypes = ArrayList<EventType>()

    private val binding by activity.viewBinding(DialogSelectEventTypeBinding::inflate)

    init {
        radioGroup = binding.dialogRadioGroup
        binding.dialogManageEventTypes.apply {
            beVisibleIf(showManageEventTypes)
            setOnClickListener {
                activity.startActivity(Intent(activity, ManageEventTypesActivity::class.java))
                dialog?.dismiss()
            }
        }

        binding.dialogRadioDivider.beVisibleIf(showManageEventTypes)

        activity.eventsHelper.getEventTypes(activity, showOnlyWritable) { eventTypes ->
            this.eventTypes = eventTypes
            activity.runOnUiThread {
                if (addLastUsedOneAsFirstOption) {
                    val lastUsedEventType = EventType(LAST_USED_EVENT_TYPE_ID, activity.getString(R.string.last_used_one), Color.TRANSPARENT, 0)
                    addRadioButton(lastUsedEventType)
                }
                this.eventTypes.filter { showCalDAVCalendars || it.caldavCalendarId == 0 }.forEach {
                    addRadioButton(it)
                }
                if (showNewEventTypeOption) {
                    val newEventType = EventType(NEW_EVENT_TYPE_ID, activity.getString(R.string.add_new_type), Color.TRANSPARENT, 0)
                    addRadioButton(newEventType)
                }
                wasInit = true
                activity.updateTextColors(binding.dialogRadioHolder)
            }
        }

        activity.getAlertDialogBuilder()
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun addRadioButton(eventType: EventType) {
        val radioBinding = RadioButtonWithColorBinding.inflate(activity.layoutInflater)
        (radioBinding.dialogRadioButton).apply {
            text = eventType.getDisplayTitle()
            isChecked = eventType.id == currEventType
            id = eventType.id!!.toInt()
        }

        if (eventType.color != Color.TRANSPARENT) {
            radioBinding.dialogRadioColor.setFillWithStroke(eventType.color, activity.getProperBackgroundColor())
        }

        radioBinding.root.setOnClickListener { viewClicked(eventType) }
        radioGroup.addView(radioBinding.root, RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun viewClicked(eventType: EventType) {
        if (!wasInit) {
            return
        }

        if (eventType.id == NEW_EVENT_TYPE_ID) {
            EditEventTypeDialog(activity) {
                callback(it)
                activity.hideKeyboard()
                dialog?.dismiss()
            }
        } else {
            callback(eventType)
            dialog?.dismiss()
        }
    }
}
