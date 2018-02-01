package com.simplemobiletools.calendar.activities

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.scheduleEventIn
import com.simplemobiletools.calendar.extensions.showEventReminderDialog
import com.simplemobiletools.calendar.helpers.EVENT_ID

class SnoozeReminderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showEventReminderDialog(config.snoozeDelay, true, cancelCallback = { dialogCancelled() }) {
            val eventId = intent.getIntExtra(EVENT_ID, 0)
            val event = dbHelper.getEventWithId(eventId)
            config.snoozeDelay = it

            if (eventId != 0 && event != null) {
                applicationContext.scheduleEventIn(System.currentTimeMillis() + it * 60000, event)
                val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(eventId)
            }
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
