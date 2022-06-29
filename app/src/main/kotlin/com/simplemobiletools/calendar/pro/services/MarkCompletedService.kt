package com.simplemobiletools.calendar.pro.services

import android.app.IntentService
import android.content.Intent
import com.simplemobiletools.calendar.pro.extensions.cancelNotification
import com.simplemobiletools.calendar.pro.extensions.cancelPendingIntent
import com.simplemobiletools.calendar.pro.extensions.eventsDB
import com.simplemobiletools.calendar.pro.helpers.ACTION_MARK_COMPLETED
import com.simplemobiletools.calendar.pro.helpers.EVENT_ID
import com.simplemobiletools.calendar.pro.helpers.FLAG_TASK_COMPLETED

class MarkCompletedService : IntentService("MarkCompleted") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null && intent.action == ACTION_MARK_COMPLETED) {
            val taskId = intent.getLongExtra(EVENT_ID, 0L)
            val task = eventsDB.getTaskWithId(taskId)
            if (task != null) {
                task.flags = task.flags or FLAG_TASK_COMPLETED
                eventsDB.updateTaskCompletion(task.id!!, task.flags)
                cancelPendingIntent(task.id!!)
                cancelNotification(task.id!!)
            }
        }
    }
}
