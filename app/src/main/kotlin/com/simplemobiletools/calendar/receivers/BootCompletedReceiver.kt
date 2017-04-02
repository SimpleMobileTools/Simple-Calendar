package com.simplemobiletools.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.scheduleNextEventReminder

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, arg1: Intent) {
        val events = context.dbHelper.getEventsAtReboot()
        for (event in events) {
            context.scheduleNextEventReminder(event)
        }
    }
}
