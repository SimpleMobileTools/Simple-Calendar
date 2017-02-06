package com.simplemobiletools.calendar.extensions

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.receivers.NotificationReceiver
import com.simplemobiletools.commons.extensions.getContrastColor

fun Context.updateWidgets() {
    val widgetsCnt = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, MyWidgetMonthlyProvider::class.java))
    if (widgetsCnt.isNotEmpty()) {
        val ids = intArrayOf(R.xml.widget_monthly_info)
        Intent(this, MyWidgetMonthlyProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(this)
        }
    }

    updateListWidget()
}

fun Context.updateListWidget() {
    val widgetsCnt = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, MyWidgetListProvider::class.java))
    if (widgetsCnt.isNotEmpty()) {
        val ids = intArrayOf(R.xml.widget_list_info)
        Intent(this, MyWidgetListProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(this)
        }
    }
}

fun Context.scheduleNextEventReminder(event: Event) {
    var startTS = event.startTS - event.reminder1Minutes * 60
    var newTS = startTS
    if (event.repeatInterval == DAY || event.repeatInterval == WEEK || event.repeatInterval == BIWEEK) {
        while (startTS < System.currentTimeMillis() / 1000 + 5) {
            startTS += event.repeatInterval
        }
        newTS = startTS
    } else if (event.repeatInterval == MONTH) {
        newTS = getNewTS(startTS, true)
    } else if (event.repeatInterval == YEAR) {
        newTS = getNewTS(startTS, false)
    }

    if (newTS != 0 && (event.repeatLimit == 0 || event.repeatLimit > newTS))
        scheduleEventIn(newTS, event)
}

private fun getNewTS(ts: Int, isMonthly: Boolean): Int {
    var dateTime = Formatter.getDateTimeFromTS(ts)
    while (dateTime.isBeforeNow) {
        dateTime = if (isMonthly) dateTime.plusMonths(1) else dateTime.plusYears(1)
    }
    return dateTime.seconds()
}

fun Context.scheduleReminder(event: Event) {
    if (event.getRemindersCount() == 0)
        return

    scheduleNextEventReminder(event)
}

fun Context.scheduleEventIn(notifTS: Int, event: Event) {
    val delayFromNow = notifTS.toLong() * 1000 - System.currentTimeMillis()
    if (delayFromNow <= 0)
        return

    val notifInMs = SystemClock.elapsedRealtime() + delayFromNow
    val pendingIntent = getNotificationIntent(this, event.id)
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, notifInMs, pendingIntent)
    else
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, notifInMs, pendingIntent)
}

private fun getNotificationIntent(context: Context, eventId: Int): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java)
    intent.putExtra(EVENT_ID, eventId)
    return PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
}

fun Context.getAppropriateTheme(): Int {
    return if (config.backgroundColor.getContrastColor() == Color.WHITE) R.style.MyDialogTheme_Dark else R.style.MyDialogTheme
}

fun Context.getReminderText(minutes: Int) = when (minutes) {
    -1 -> getString(R.string.no_reminder)
    0 -> getString(R.string.at_start)
    else -> {
        if (minutes % 1440 == 0)
            resources.getQuantityString(R.plurals.days, minutes / 1440, minutes / 1440)
        else if (minutes % 60 == 0)
            resources.getQuantityString(R.plurals.hours, minutes / 60, minutes / 60)
        else
            resources.getQuantityString(R.plurals.minutes, minutes, minutes)
    }
}

fun Context.getNewEventTimestampFromCode(dayCode: String) = Formatter.getLocalDateTimeFromCode(dayCode).withTime(13, 0, 0, 0).seconds()

val Context.config: Config get() = Config.newInstance(this)
