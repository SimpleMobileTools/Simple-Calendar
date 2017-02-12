package com.simplemobiletools.calendar.dialogs

import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.adapters.FilterEventTypeAdapter
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.dialog_filter_events.view.*
import java.util.*

class FilterEventsDialog(val activity: SimpleActivity, val callback: () -> Unit) : AlertDialog.Builder(activity) {
    val dialog: AlertDialog?
    var eventTypes = ArrayList<EventType>()

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_filter_events, null)
        DBHelper.newInstance(activity).getEventTypes {
            eventTypes = it
            view.filter_events_list.adapter = FilterEventTypeAdapter(activity, it)

            activity.runOnUiThread {
                activity.updateTextColors(view.filter_events_list)
            }
        }

        dialog = AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.filter_events_by_type)
        }
    }
}
