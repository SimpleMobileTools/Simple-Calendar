package com.simplemobiletools.calendar.activities

import android.os.Bundle
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.EventTypeAdapter
import com.simplemobiletools.calendar.dialogs.EventTypeDialog
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.views.RecyclerViewDivider
import kotlinx.android.synthetic.main.activity_manage_event_types.*

class ManageEventTypesActivity : SimpleActivity(), EventTypeAdapter.DeleteEventTypeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event_types)

        getEventTypes()
        manage_event_types_fab.setOnClickListener {
            showEventTypeDialog()
        }

        updateTextColors(manage_event_types_coordinator)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        EventTypeDialog(this, eventType) {
            getEventTypes()
        }
    }

    private fun getEventTypes() {
        DBHelper.newInstance(applicationContext).getEventTypes {
            runOnUiThread {
                gotEventTypes(it)
            }
        }
    }

    private fun gotEventTypes(eventTypes: List<EventType>) {
        val eventTypesAdapter = EventTypeAdapter(this, eventTypes, this) {
            showEventTypeDialog(it)
        }

        manage_event_types_list.apply {
            this@apply.adapter = eventTypesAdapter
            addItemDecoration(RecyclerViewDivider(context))
        }
    }

    override fun eventTypeDeleted() {
        getEventTypes()
    }
}
