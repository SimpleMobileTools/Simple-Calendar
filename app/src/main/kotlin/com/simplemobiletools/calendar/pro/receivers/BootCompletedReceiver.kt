package com.simplemobiletools.calendar.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.AUTO_BACKUP_INTERVAL_IN_DAYS
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
                val intervalInSeconds = AUTO_BACKUP_INTERVAL_IN_DAYS * DAY
                if (config.autoBackup && config.lastAutoBackupTime !in (now - intervalInSeconds)..now) {
                    // device was probably off at the scheduled time so backup now
                    backupEventsAndTasks()
                }
            }
        }
    }
}
