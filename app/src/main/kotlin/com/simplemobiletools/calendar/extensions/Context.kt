package com.simplemobiletools.calendar.extensions

import android.annotation.TargetApi
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.support.v7.app.NotificationCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.DayMonthly
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.receivers.CalDAVSyncReceiver
import com.simplemobiletools.calendar.receivers.NotificationReceiver
import com.simplemobiletools.calendar.services.SnoozeService
import com.simplemobiletools.commons.extensions.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.text.SimpleDateFormat
import java.util.*

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

fun Context.scheduleAllEvents() {
    val events = dbHelper.getEventsAtReboot()
    events.forEach {
        scheduleNextEventReminder(it, dbHelper)
    }
}

fun Context.scheduleNextEventReminder(event: Event, dbHelper: DBHelper) {
    if (event.getReminders().isEmpty())
        return

    val now = (System.currentTimeMillis() / 1000).toInt()
    val reminderSeconds = event.getReminders().reversed().map { it * 60 }

    dbHelper.getEvents(now, now + YEAR, event.id) {
        if (it.isNotEmpty()) {
            for (curEvent in it) {
                for (curReminder in reminderSeconds) {
                    if (curEvent.getEventStartTS() - curReminder > now) {
                        scheduleEventIn((curEvent.getEventStartTS() - curReminder) * 1000L, curEvent)
                        return@getEvents
                    }
                }
            }
        }
    }
}

fun Context.scheduleEventIn(notifTS: Long, event: Event) {
    if (notifTS < System.currentTimeMillis())
        return

    val pendingIntent = getNotificationIntent(this, event)
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (isKitkatPlus())
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, notifTS, pendingIntent)
    else
        alarmManager.set(AlarmManager.RTC_WAKEUP, notifTS, pendingIntent)
}

fun Context.cancelNotification(id: Int) {
    val intent = Intent(this, NotificationReceiver::class.java)
    PendingIntent.getBroadcast(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT).cancel()
}

private fun getNotificationIntent(context: Context, event: Event): PendingIntent {
    val intent = Intent(context, NotificationReceiver::class.java)
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getBroadcast(context, event.id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
}

fun Context.getAppropriateTheme() = if (config.backgroundColor.getContrastColor() == Color.WHITE) R.style.MyDialogTheme_Dark else R.style.MyDialogTheme

fun Context.getFormattedMinutes(minutes: Int, showBefore: Boolean = true) = when (minutes) {
    -1 -> getString(R.string.no_reminder)
    0 -> getString(R.string.at_start)
    else -> {
        if (minutes % 525600 == 0)
            resources.getQuantityString(R.plurals.years, minutes / 525600, minutes / 525600)

        when {
            minutes % 43200 == 0 -> resources.getQuantityString(R.plurals.months, minutes / 43200, minutes / 43200)
            minutes % 10080 == 0 -> resources.getQuantityString(R.plurals.weeks, minutes / 10080, minutes / 10080)
            minutes % 1440 == 0 -> resources.getQuantityString(R.plurals.days, minutes / 1440, minutes / 1440)
            minutes % 60 == 0 -> {
                val base = if (showBefore) R.plurals.hours_before else R.plurals.by_hours
                resources.getQuantityString(base, minutes / 60, minutes / 60)
            }
            else -> {
                val base = if (showBefore) R.plurals.minutes_before else R.plurals.by_minutes
                resources.getQuantityString(base, minutes, minutes)
            }
        }
    }
}

fun Context.getRepetitionText(seconds: Int) = when (seconds) {
    0 -> getString(R.string.no_repetition)
    DAY -> getString(R.string.daily)
    WEEK -> getString(R.string.weekly)
    MONTH -> getString(R.string.monthly)
    YEAR -> getString(R.string.yearly)
    else -> {
        when {
            seconds % YEAR == 0 -> resources.getQuantityString(R.plurals.years, seconds / YEAR, seconds / YEAR)
            seconds % MONTH == 0 -> resources.getQuantityString(R.plurals.months, seconds / MONTH, seconds / MONTH)
            seconds % WEEK == 0 -> resources.getQuantityString(R.plurals.weeks, seconds / WEEK, seconds / WEEK)
            else -> resources.getQuantityString(R.plurals.days, seconds / DAY, seconds / DAY)
        }
    }
}

fun Context.getFilteredEvents(events: List<Event>): List<Event> {
    val displayEventTypes = config.displayEventTypes
    return events.filter { displayEventTypes.contains(it.eventType.toString()) }
}

fun Context.notifyRunningEvents() {
    dbHelper.getRunningEvents().forEach { notifyEvent(it) }
}

fun Context.notifyEvent(event: Event) {
    val pendingIntent = getPendingIntent(this, event)
    val startTime = Formatter.getTimeFromTS(this, event.startTS)
    val endTime = Formatter.getTimeFromTS(this, event.endTS)
    val timeRange = if (event.getIsAllDay()) getString(R.string.all_day) else getFormattedEventTime(startTime, endTime)
    val descriptionOrLocation = if (config.replaceDescription) event.location else event.description
    val notification = getNotification(this, pendingIntent, event, "$timeRange $descriptionOrLocation")
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(event.id, notification)
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
private fun getNotification(context: Context, pendingIntent: PendingIntent, event: Event, content: String): Notification {
    val soundUri = Uri.parse(context.config.reminderSound)
    val builder = NotificationCompat.Builder(context)
            .setContentTitle(event.title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setSound(soundUri)
            .addAction(R.drawable.ic_snooze, context.getString(R.string.snooze), getSnoozePendingIntent(context, event))

    if (context.isLollipopPlus())
        builder.setVisibility(Notification.VISIBILITY_PUBLIC)

    if (context.config.vibrateOnReminder)
        builder.setVibrate(longArrayOf(0, 300, 300, 300))

    return builder.build()
}

private fun getFormattedEventTime(startTime: String, endTime: String) = if (startTime == endTime) startTime else "$startTime - $endTime"

private fun getPendingIntent(context: Context, event: Event): PendingIntent {
    val intent = Intent(context, EventActivity::class.java)
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getActivity(context, event.id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
}

private fun getSnoozePendingIntent(context: Context, event: Event): PendingIntent {
    val intent = Intent(context, SnoozeService::class.java).setAction("snooze")
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getService(context, event.id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
}

fun Context.launchNewEventIntent() {
    val code = Formatter.getDayCodeFromDateTime(DateTime(DateTimeZone.getDefault()))
    Intent(applicationContext, EventActivity::class.java).apply {
        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(code))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

fun Context.getNewEventTimestampFromCode(dayCode: String): Int {
    val currHour = DateTime(System.currentTimeMillis(), DateTimeZone.getDefault()).hourOfDay
    val dateTime = Formatter.getLocalDateTimeFromCode(dayCode).withHourOfDay(currHour)
    return dateTime.plusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).seconds()
}

fun Context.getCurrentOffset() = SimpleDateFormat("Z", Locale.getDefault()).format(Date())

fun Context.getSyncedCalDAVCalendars() = CalDAVHandler(this).getCalDAVCalendars(null, config.caldavSyncedCalendarIDs)

fun Context.recheckCalDAVCalendars(callback: () -> Unit) {
    if (config.caldavSync) {
        Thread({
            CalDAVHandler(this).refreshCalendars(null, callback)
            updateWidgets()
        }).start()
    }
}

fun Context.scheduleCalDAVSync(activate: Boolean) {
    val syncIntent = Intent(this, CalDAVSyncReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(this, 0, syncIntent, PendingIntent.FLAG_CANCEL_CURRENT)
    val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (activate) {
        val syncCheckInterval = 4 * AlarmManager.INTERVAL_HOUR
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + syncCheckInterval, syncCheckInterval, pendingIntent)
    } else {
        alarm.cancel(pendingIntent)
    }
}

val Context.config: Config get() = Config.newInstance(this)

val Context.dbHelper: DBHelper get() = DBHelper.newInstance(this)

fun Context.addDayNumber(rawTextColor: Int, day: DayMonthly, linearLayout: LinearLayout, dayLabelHeight: Int, callback: (Int) -> Unit) {
    var textColor = rawTextColor
    if (!day.isThisMonth)
        textColor = textColor.adjustAlpha(LOW_ALPHA)

    (View.inflate(this, R.layout.day_monthly_number_view, null) as TextView).apply {
        setTextColor(textColor)
        text = day.value.toString()
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        linearLayout.addView(this)

        if (day.isToday) {
            val primaryColor = config.primaryColor
            setTextColor(config.primaryColor.getContrastColor().adjustAlpha(MEDIUM_ALPHA))
            if (dayLabelHeight == 0) {
                onGlobalLayout {
                    val height = this@apply.height
                    if (height > 0) {
                        callback(height)
                        addTodaysBackground(this, resources, height, primaryColor)
                    }
                }
            } else {
                addTodaysBackground(this, resources, dayLabelHeight, primaryColor)
            }
        }
    }
}

private fun addTodaysBackground(textView: TextView, res: Resources, dayLabelHeight: Int, mPrimaryColor: Int) =
        textView.addResizedBackgroundDrawable(res, dayLabelHeight, mPrimaryColor, R.drawable.monthly_today_circle)

fun Context.addDayEvents(day: DayMonthly, linearLayout: LinearLayout, res: Resources, dividerMargin: Int) {
    day.dayEvents.forEach {
        val backgroundDrawable = res.getDrawable(R.drawable.day_monthly_event_background)
        backgroundDrawable.mutate().setColorFilter(it.color, PorterDuff.Mode.SRC_IN)

        val eventLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        eventLayoutParams.setMargins(dividerMargin, 0, dividerMargin, dividerMargin)

        var textColor = it.color.getContrastColor().adjustAlpha(MEDIUM_ALPHA)
        if (!day.isThisMonth) {
            backgroundDrawable.alpha = 64
            textColor = textColor.adjustAlpha(0.25f)
        }

        (View.inflate(this, R.layout.day_monthly_event_view, null) as TextView).apply {
            setTextColor(textColor)
            text = it.title.replace(" ", "\u00A0")  // allow word break by char
            background = backgroundDrawable
            layoutParams = eventLayoutParams
            linearLayout.addView(this)
        }
    }
}
