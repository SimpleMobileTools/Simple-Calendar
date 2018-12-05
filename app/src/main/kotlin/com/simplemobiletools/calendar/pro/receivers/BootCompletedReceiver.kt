package com.simplemobiletools.calendar.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.pro.extensions.notifyRunningEvents
import com.simplemobiletools.calendar.pro.extensions.recheckCalDAVCalendars
import com.simplemobiletools.calendar.pro.extensions.scheduleAllEvents

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Thread {
            context.apply {
                scheduleAllEvents()
                notifyRunningEvents()
                recheckCalDAVCalendars {}
            }
        }.start()
    }
}
