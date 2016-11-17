package com.simplemobiletools.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.Utils

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, arg1: Intent) {
        val events = DBHelper(context).getEventsAtReboot()
        for (event in events) {
            Utils.scheduleNextEvent(context, event)
        }
    }
}
