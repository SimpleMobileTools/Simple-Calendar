package com.simplemobiletools.calendar.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.DAY
import com.simplemobiletools.calendar.pro.helpers.getNowSeconds
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            context.apply {
                scheduleAllEvents()
                notifyRunningEvents()
                recheckCalDAVCalendars(true) {}
                scheduleNextAutomaticBackup()
                val now = getNowSeconds()
                if (config.autoBackup && config.lastAutoBackupTime !in (now - DAY)..now) {
                    // device was probably off at the scheduled time so backup now
                    backupEventsAndTasks()
                }
            }
        }
    }
}
