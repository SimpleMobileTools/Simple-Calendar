package com.simplemobiletools.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.extensions.recheckCalDAVCalendars
import com.simplemobiletools.calendar.extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.recheckCalDAVCalendars {
            context.updateWidgets()
        }
    }
}
