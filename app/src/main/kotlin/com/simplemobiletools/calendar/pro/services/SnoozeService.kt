package com.simplemobiletools.calendar.pro.services

import android.app.IntentService
import android.content.Intent
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventsDB
import com.simplemobiletools.calendar.pro.extensions.rescheduleReminder
import com.simplemobiletools.calendar.pro.helpers.EVENT_ID

class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val eventId = intent.getLongExtra(EVENT_ID, 0L)
            val event = eventsDB.getEventWithId(eventId)
            rescheduleReminder(event, config.snoozeTime)
        }
    }
}
