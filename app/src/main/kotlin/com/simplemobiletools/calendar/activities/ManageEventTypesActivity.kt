package com.simplemobiletools.calendar.activities

import android.os.Bundle
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.dialogs.EventTypeDialog
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.activity_manage_event_types.*

class ManageEventTypesActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_event_types)

        DBHelper.newInstance(applicationContext).getEventTypes {

        }

        manage_event_types_fab.setOnClickListener {
            showEventTypeDialog()
        }

        updateTextColors(manage_event_types_coordinator)
    }

    private fun showEventTypeDialog() {
        EventTypeDialog(this) {

        }
    }
}
