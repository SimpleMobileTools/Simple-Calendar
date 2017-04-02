package com.simplemobiletools.calendar.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.EventTypeAdapter
import com.simplemobiletools.calendar.dialogs.NewEventTypeDialog
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.interfaces.DeleteItemsListener
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.activity_manage_event_types.*
import java.util.*

class ManageEventTypesActivity : SimpleActivity(), DeleteItemsListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event_types)

        getEventTypes()
        updateTextColors(manage_event_types_list)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        NewEventTypeDialog(this, eventType) {
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

    override fun deleteItems(ids: ArrayList<Int>) {
        if (ids.contains(DBHelper.REGULAR_EVENT_TYPE_ID)) {
            toast(R.string.cannot_delete_default_type)
        }

        dbHelper.deleteEventTypes(ids) {
            if (it > 0) {
                getEventTypes()
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    }
}
