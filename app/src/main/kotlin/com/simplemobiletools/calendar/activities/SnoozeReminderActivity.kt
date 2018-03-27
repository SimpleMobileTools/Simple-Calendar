package com.simplemobiletools.calendar.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.rescheduleReminder
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.commons.extensions.showPickSecondsDialogHelper

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showPickSecondsDialogHelper(config.snoozeTime, true, cancelCallback = { dialogCancelled() }) {
            val eventId = intent.getIntExtra(EVENT_ID, 0)
            val event = dbHelper.getEventWithId(eventId)
            config.snoozeTime = it / 60
            rescheduleReminder(event, it / 60)
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
