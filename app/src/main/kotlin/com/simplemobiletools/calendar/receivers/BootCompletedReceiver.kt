package com.simplemobiletools.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.extensions.notifyRunningEvents
import com.simplemobiletools.calendar.extensions.recheckCalDAVCalendars
import com.simplemobiletools.calendar.extensions.scheduleAllEvents

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, arg1: Intent) {
        context.apply {
            scheduleAllEvents()
            notifyRunningEvents()
            recheckCalDAVCalendars {}
        }
    }
}
