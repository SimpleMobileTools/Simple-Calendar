package com.simplemobiletools.calendar.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.rescheduleReminder
import com.simplemobiletools.calendar.extensions.showEventReminderDialog
import com.simplemobiletools.calendar.helpers.EVENT_ID

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showEventReminderDialog(config.snoozeDelay, true, cancelCallback = { dialogCancelled() }) {
            val eventId = intent.getIntExtra(EVENT_ID, 0)
            val event = dbHelper.getEventWithId(eventId)
            config.snoozeDelay = it
            rescheduleReminder(event, it)
            finishActivity()
        }
    }

    private fun dialogCancelled() {
        finishActivity()
    }

    private fun finishActivity() {
        finish()
        overridePendingTransition(0, 0)
    }
}
