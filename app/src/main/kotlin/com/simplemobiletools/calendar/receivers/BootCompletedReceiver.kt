package com.simplemobiletools.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.extensions.scheduleNextEventReminder
import com.simplemobiletools.calendar.helpers.DBHelper

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, arg1: Intent) {
        val events = DBHelper.newInstance(context).getEventsAtReboot()
        for (event in events) {
            context.scheduleNextEventReminder(event)
        }
    }
}
