package com.simplemobiletools.calendar.receivers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.scheduleNextEvent
import com.simplemobiletools.calendar.extensions.updateListWidget
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.REMINDER_OFF
import com.simplemobiletools.calendar.models.Event

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.updateListWidget()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = intent.getIntExtra(EVENT_ID, -1)
        if (id == -1)
            return

        val event = DBHelper(context).getEvent(id)
        if (event == null || (event.reminder1Minutes == REMINDER_OFF && event.reminder2Minutes == REMINDER_OFF && event.reminder3Minutes == REMINDER_OFF))
            return

        val pendingIntent = getPendingIntent(context, event)
        val startTime = Formatter.getTimeFromTS(context, event.startTS)
        val endTime = Formatter.getTimeFromTS(context, event.endTS)
        val notification = getNotification(context, pendingIntent, event.title, "${getEventTime(startTime, endTime)} ${event.description}")
        notificationManager.notify(id, notification)

        if (event.repeatInterval != 0)
            context.scheduleNextEvent(event)
    }

    private fun getEventTime(startTime: String, endTime: String) = if (startTime == endTime) startTime else "$startTime - $endTime"

    private fun getPendingIntent(context: Context, event: Event): PendingIntent {
        val intent = Intent(context, EventActivity::class.java)
        intent.putExtra(EVENT_ID, event.id)
        return PendingIntent.getActivity(context, event.id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getNotification(context: Context, pendingIntent: PendingIntent, title: String, content: String): Notification {
        val soundUri = Uri.parse(context.config.reminderSound)
        val builder = Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setAutoCancel(true)
                .setSound(soundUri)

        if (context.config.vibrateOnReminder)
            builder.setVibrate(longArrayOf(0, 300, 300, 300))

        return builder.build()
    }
}
