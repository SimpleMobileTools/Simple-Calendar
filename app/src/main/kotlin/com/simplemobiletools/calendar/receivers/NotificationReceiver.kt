package com.simplemobiletools.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.notifyEvent
import com.simplemobiletools.calendar.extensions.scheduleNextEventReminder
import com.simplemobiletools.calendar.extensions.updateListWidget
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.calendar.helpers.Formatter

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Simple Calendar")
        wakelock.acquire(3000)

        Thread {
            handleIntent(context, intent)
        }.start()
    }

    private fun handleIntent(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EVENT_ID, -1)
        if (id == -1) {
            return
        }

        context.updateListWidget()
        val event = context.dbHelper.getEventWithId(id)
        if (event == null || event.getReminders().isEmpty()) {
            return
        }

        if (!event.ignoreEventOccurrences.contains(Formatter.getDayCodeFromTS(event.startTS).toInt())) {
            context.notifyEvent(event)
        }
        context.scheduleNextEventReminder(event, context.dbHelper)
    }
}
