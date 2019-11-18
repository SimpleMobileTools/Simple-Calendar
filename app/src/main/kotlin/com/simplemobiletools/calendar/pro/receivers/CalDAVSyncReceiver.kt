package com.simplemobiletools.calendar.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.recheckCalDAVCalendars
import com.simplemobiletools.calendar.pro.extensions.refreshCalDAVCalendars
import com.simplemobiletools.calendar.pro.extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.config.caldavSync) {
            context.refreshCalDAVCalendars(context.config.caldavSyncedCalendarIds, false)
        }

        context.recheckCalDAVCalendars {
            context.updateWidgets()
        }
    }
}
