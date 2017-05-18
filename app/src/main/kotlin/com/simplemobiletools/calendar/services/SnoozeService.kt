package com.simplemobiletools.calendar.services

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.scheduleEventIn
import com.simplemobiletools.calendar.helpers.EVENT_ID

class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent) {
        val eventId = intent.getIntExtra(EVENT_ID, 0)
        val event = dbHelper.getEventWithId(eventId)

        if (eventId != 0 && event != null) {
            applicationContext.scheduleEventIn(System.currentTimeMillis() + applicationContext.config.snoozeDelay * 60000, event)
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(eventId)
        }
    }
}
