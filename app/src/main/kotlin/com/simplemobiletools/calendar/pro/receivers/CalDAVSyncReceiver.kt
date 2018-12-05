package com.simplemobiletools.calendar.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.pro.extensions.recheckCalDAVCalendars
import com.simplemobiletools.calendar.pro.extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.recheckCalDAVCalendars {
            context.updateWidgets()
        }
    }
}
