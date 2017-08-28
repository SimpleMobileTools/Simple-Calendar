package com.simplemobiletools.calendar.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.EventTypeAdapter
import com.simplemobiletools.calendar.dialogs.UpdateEventTypeDialog
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.interfaces.DeleteEventTypesListener
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.activity_manage_event_types.*
import java.util.*

class ManageEventTypesActivity : SimpleActivity(), DeleteEventTypesListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event_types)

        getEventTypes()
        updateTextColors(manage_event_types_list)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        UpdateEventTypeDialog(this, eventType?.copy()) {
            getEventTypes()
        }
    }

    private fun getEventTypes() {
        dbHelper.getEventTypes {
            runOnUiThread {
                manage_event_types_list.adapter = EventTypeAdapter(this, it, this) {
                    showEventTypeDialog(it)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_event_types, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_event_type -> showEventTypeDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean) {
        if (eventTypes.map { it.id }.contains(DBHelper.REGULAR_EVENT_TYPE_ID)) {
            toast(R.string.cannot_delete_default_type)
        }

        if (eventTypes.any { it.caldavCalendarId != 0 }) {
            toast(R.string.unsync_caldav_calendar)
        }

        dbHelper.deleteEventTypes(eventTypes, deleteEvents) {
            if (it > 0) {
                getEventTypes()
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    }
}
