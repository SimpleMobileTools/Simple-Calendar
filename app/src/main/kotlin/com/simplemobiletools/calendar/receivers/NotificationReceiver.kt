package com.simplemobiletools.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.notifyEvent
import com.simplemobiletools.calendar.extensions.scheduleAllEvents
import com.simplemobiletools.calendar.extensions.updateListWidget
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.calendar.helpers.Formatter

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.updateListWidget()
        val id = intent.getIntExtra(EVENT_ID, -1)
        if (id == -1)
            return

        val event = context.dbHelper.getEventWithId(id)
        if (event == null || event.getReminders().isEmpty())
            return

        if (!event.ignoreEventOccurrences.contains(Formatter.getDayCodeFromTS(event.startTS).toInt())) {
            context.notifyEvent(event)
        }
        context.scheduleAllEvents()
    }
}
