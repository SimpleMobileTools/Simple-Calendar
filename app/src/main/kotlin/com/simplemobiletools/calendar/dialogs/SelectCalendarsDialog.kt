package com.simplemobiletools.calendar.dialogs

import android.app.Activity
import android.support.v7.app.AlertDialog
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.getCalDAVCalendars
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.calendar_item_account.view.*
import kotlinx.android.synthetic.main.calendar_item_calendar.view.*
import kotlinx.android.synthetic.main.dialog_select_calendars.view.*

class SelectCalendarsDialog(val activity: Activity, val callback: (calendars: Int) -> Unit) : AlertDialog.Builder(activity) {
    var prevAccount = ""
    var dialog: AlertDialog
    var view = (activity.layoutInflater.inflate(R.layout.dialog_select_calendars, null) as ViewGroup)

    init {
        val calendars = activity.getCalDAVCalendars()
        val sorted = calendars.sortedWith(compareBy({ it.accountName }, { it.displayName }))
        sorted.forEach {
            if (prevAccount != it.accountName) {
                prevAccount = it.accountName
                addCalendarItem(false, it.accountName)
            }

            addCalendarItem(true, it.displayName)
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, { dialogInterface, i -> confirmSelection() })
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.select_caldav_calendars)
        }
    }

    private fun addCalendarItem(isEvent: Boolean, text: String) {
        val calendarItem = activity.layoutInflater.inflate(if (isEvent) R.layout.calendar_item_calendar else R.layout.calendar_item_account,
                view.dialog_select_calendars_holder, false)

        if (isEvent) {
            calendarItem.calendar_item_calendar_switch.text = text
            calendarItem.setOnClickListener {
                calendarItem.calendar_item_calendar_switch.toggle()
            }
        } else {
            calendarItem.calendar_item_account.text = text
        }

        view.dialog_select_calendars_holder.addView(calendarItem)
    }

    private fun confirmSelection() {

    }
}
