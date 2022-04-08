package com.simplemobiletools.calendar.pro.extensions

import android.accounts.Account
import android.annotation.SuppressLint
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.print.PrintHelper
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.EventActivity
import com.simplemobiletools.calendar.pro.activities.SnoozeReminderActivity
import com.simplemobiletools.calendar.pro.activities.TaskActivity
import com.simplemobiletools.calendar.pro.databases.EventsDatabase
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.interfaces.EventTypesDao
import com.simplemobiletools.calendar.pro.interfaces.EventsDao
import com.simplemobiletools.calendar.pro.interfaces.WidgetsDao
import com.simplemobiletools.calendar.pro.models.*
import com.simplemobiletools.calendar.pro.receivers.CalDAVSyncReceiver
import com.simplemobiletools.calendar.pro.receivers.NotificationReceiver
import com.simplemobiletools.calendar.pro.services.SnoozeService
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import kotlinx.android.synthetic.main.day_monthly_event_view.view.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import java.util.*

val Context.config: Config get() = Config.newInstance(applicationContext)
val Context.eventsDB: EventsDao get() = EventsDatabase.getInstance(applicationContext).EventsDao()
val Context.eventTypesDB: EventTypesDao get() = EventsDatabase.getInstance(applicationContext).EventTypesDao()
val Context.widgetsDB: WidgetsDao get() = EventsDatabase.getInstance(applicationContext).WidgetsDao()
val Context.eventsHelper: EventsHelper get() = EventsHelper(this)
val Context.calDAVHelper: CalDAVHelper get() = CalDAVHelper(this)

fun Context.updateWidgets() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetIds(ComponentName(applicationContext, MyWidgetMonthlyProvider::class.java))
        ?: return
    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetMonthlyProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }

    updateListWidget()
    updateDateWidget()
}

fun Context.updateListWidget() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetIds(ComponentName(applicationContext, MyWidgetListProvider::class.java))
        ?: return

    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetListProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }
    AppWidgetManager.getInstance(applicationContext)?.notifyAppWidgetViewDataChanged(widgetIDs, R.id.widget_event_list)
}

fun Context.updateDateWidget() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext)?.getAppWidgetIds(ComponentName(applicationContext, MyWidgetDateProvider::class.java))
        ?: return
    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetDateProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }
}

fun Context.scheduleAllEvents() {
    val events = eventsDB.getEventsAtReboot(getNowSeconds())
    events.forEach {
        scheduleNextEventReminder(it, false)
    }
}

fun Context.scheduleNextEventReminder(event: Event, showToasts: Boolean) {
    val validReminders = event.getReminders().filter { it.type == REMINDER_NOTIFICATION }
    if (validReminders.isEmpty()) {
        if (showToasts) {
            if (config.displayEventTypes.contains(event.eventType.toString())) {
                toast(R.string.saving)
            } else {
                toast(R.string.saving_filtered_out, Toast.LENGTH_LONG)
            }
        }
        return
    }

    val now = getNowSeconds()
    val reminderSeconds = validReminders.reversed().map { it.minutes * 60 }
    eventsHelper.getEvents(now, now + YEAR, event.id!!, false) {
        if (it.isNotEmpty()) {
            for (curEvent in it) {
                for (curReminder in reminderSeconds) {
                    if (curEvent.getEventStartTS() - curReminder > now) {
                        scheduleEventIn((curEvent.getEventStartTS() - curReminder) * 1000L, curEvent, showToasts)
                        return@getEvents
                    }
                }
            }
        }

        if (showToasts) {
            toast(R.string.saving)
        }
    }
}

fun Context.scheduleEventIn(notifTS: Long, event: Event, showToasts: Boolean) {
    if (notifTS < System.currentTimeMillis()) {
        if (showToasts) {
            toast(R.string.saving)
        }
        return
    }

    val newNotifTS = notifTS + 1000
    if (showToasts) {
        if (config.displayEventTypes.contains(event.eventType.toString())) {
            val secondsTillNotification = (newNotifTS - System.currentTimeMillis()) / 1000
            val msg = String.format(getString(R.string.time_remaining), formatSecondsToTimeString(secondsTillNotification.toInt()))
            toast(msg)
        } else {
            toast(R.string.saving_filtered_out, Toast.LENGTH_LONG)
        }
    }

    val pendingIntent = getNotificationIntent(event)
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    try {
        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, newNotifTS, pendingIntent)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

// hide the actual notification from the top bar
fun Context.cancelNotification(id: Long) {
    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id.toInt())
}

fun Context.getNotificationIntent(event: Event): PendingIntent {
    val intent = Intent(this, NotificationReceiver::class.java)
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getBroadcast(this, event.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.cancelPendingIntent(id: Long) {
    val intent = Intent(this, NotificationReceiver::class.java)
    PendingIntent.getBroadcast(this, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE).cancel()
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

fun Context.notifyRunningEvents() {
    eventsHelper.getRunningEvents().filter { it.getReminders().any { it.type == REMINDER_NOTIFICATION } }.forEach {
        notifyEvent(it)
    }
}

fun Context.notifyEvent(originalEvent: Event) {
    var event = originalEvent.copy()
    val currentSeconds = getNowSeconds()

    var eventStartTS = if (event.getIsAllDay()) Formatter.getDayStartTS(Formatter.getDayCodeFromTS(event.startTS)) else event.startTS
    // make sure refer to the proper repeatable event instance with "Tomorrow", or the specific date
    if (event.repeatInterval != 0 && eventStartTS - event.reminder1Minutes * 60 < currentSeconds) {
        val events = eventsHelper.getRepeatableEventsFor(currentSeconds - WEEK_SECONDS, currentSeconds + YEAR_SECONDS, event.id!!)
        for (currEvent in events) {
            eventStartTS = if (currEvent.getIsAllDay()) Formatter.getDayStartTS(Formatter.getDayCodeFromTS(currEvent.startTS)) else currEvent.startTS
            if (eventStartTS - currEvent.reminder1Minutes * 60 > currentSeconds) {
                break
            }

            event = currEvent
        }
    }

    val pendingIntent = getPendingIntent(applicationContext, event)
    val startTime = Formatter.getTimeFromTS(applicationContext, event.startTS)
    val endTime = Formatter.getTimeFromTS(applicationContext, event.endTS)
    val startDate = Formatter.getDateFromTS(event.startTS)

    val displayedStartDate = when (startDate) {
        LocalDate.now() -> ""
        LocalDate.now().plusDays(1) -> getString(R.string.tomorrow)
        else -> "${Formatter.getDateFromCode(this, Formatter.getDayCodeFromTS(event.startTS))},"
    }

    val timeRange = if (event.getIsAllDay()) getString(R.string.all_day) else getFormattedEventTime(startTime, endTime)
    val descriptionOrLocation = if (config.replaceDescription) event.location else event.description
    val content = "$displayedStartDate $timeRange $descriptionOrLocation".trim()
    ensureBackgroundThread {
        val notification = getNotification(pendingIntent, event, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            if (notification != null) {
                notificationManager.notify(event.id!!.toInt(), notification)
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}

@SuppressLint("NewApi")
fun Context.getNotification(pendingIntent: PendingIntent, event: Event, content: String, publicVersion: Boolean = false): Notification? {
    var soundUri = config.reminderSoundUri
    if (soundUri == SILENT) {
        soundUri = ""
    } else {
        grantReadUriPermission(soundUri)
    }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // create a new channel for every new sound uri as the new Android Oreo notification system is fundamentally broken
    if (soundUri != config.lastSoundUri || config.lastVibrateOnReminder != config.vibrateOnReminder) {
        if (!publicVersion) {
            if (isOreoPlus()) {
                val oldChannelId = "simple_calendar_${config.lastReminderChannel}_${config.reminderAudioStream}_${event.eventType}"
                notificationManager.deleteNotificationChannel(oldChannelId)
            }
        }

        config.lastVibrateOnReminder = config.vibrateOnReminder
        config.lastReminderChannel = System.currentTimeMillis()
        config.lastSoundUri = soundUri
    }

    val channelId = "simple_calendar_${config.lastReminderChannel}_${config.reminderAudioStream}_${event.eventType}"
    if (isOreoPlus()) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(config.reminderAudioStream)
            .build()

        val name = eventTypesDB.getEventTypeWithId(event.eventType)?.getDisplayTitle()
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(channelId, name, importance).apply {
            setBypassDnd(true)
            enableLights(true)
            lightColor = event.color
            enableVibration(config.vibrateOnReminder)
            setSound(Uri.parse(soundUri), audioAttributes)
            try {
                notificationManager.createNotificationChannel(this)
            } catch (e: Exception) {
                showErrorToast(e)
                return null
            }
        }
    }

    val contentTitle = if (publicVersion) resources.getString(R.string.app_name) else event.title
    val contentText = if (publicVersion) resources.getString(R.string.public_event_notification_text) else content

    val builder = NotificationCompat.Builder(this, channelId)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_calendar_vector)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(Notification.DEFAULT_LIGHTS)
        .setCategory(Notification.CATEGORY_EVENT)
        .setAutoCancel(true)
        .setSound(Uri.parse(soundUri), config.reminderAudioStream)
        .setChannelId(channelId)
        .addAction(R.drawable.ic_snooze_vector, getString(R.string.snooze), getSnoozePendingIntent(this, event))

    if (config.vibrateOnReminder) {
        val vibrateArray = LongArray(2) { 500 }
        builder.setVibrate(vibrateArray)
    }

    if (!publicVersion) {
        val notification = getNotification(pendingIntent, event, content, true)
        if (notification != null) {
            builder.setPublicVersion(notification)
        }
    }

    val notification = builder.build()
    if (config.loopReminders) {
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
    }
    return notification
}

private fun getFormattedEventTime(startTime: String, endTime: String) = if (startTime == endTime) startTime else "$startTime \u2013 $endTime"

private fun getPendingIntent(context: Context, event: Event): PendingIntent {
    val intent = Intent(context, EventActivity::class.java)
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getActivity(context, event.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

private fun getSnoozePendingIntent(context: Context, event: Event): PendingIntent {
    val snoozeClass = if (context.config.useSameSnooze) SnoozeService::class.java else SnoozeReminderActivity::class.java
    val intent = Intent(context, snoozeClass).setAction("Snooze")
    intent.putExtra(EVENT_ID, event.id)
    return if (context.config.useSameSnooze) {
        PendingIntent.getService(context, event.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    } else {
        PendingIntent.getActivity(context, event.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

fun Context.rescheduleReminder(event: Event?, minutes: Int) {
    if (event != null) {
        cancelPendingIntent(event.id!!)
        applicationContext.scheduleEventIn(System.currentTimeMillis() + minutes * 60000, event, false)
        cancelNotification(event.id!!)
    }
}

// if the default event start time is set to "Next full hour" and the event is created before midnight, it could change the day
fun Context.launchNewEventIntent(dayCode: String = Formatter.getTodayCode(), allowChangingDay: Boolean = false) {
    Intent(applicationContext, EventActivity::class.java).apply {
        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode, allowChangingDay))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

// if the default start time is set to "Next full hour" and the task is created before midnight, it could change the day
fun Context.launchNewTaskIntent(dayCode: String = Formatter.getTodayCode(), allowChangingDay: Boolean = false) {
    Intent(applicationContext, TaskActivity::class.java).apply {
        putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(dayCode, allowChangingDay))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(this)
    }
}

fun Context.getNewEventTimestampFromCode(dayCode: String, allowChangingDay: Boolean = false): Long {
    val calendar = Calendar.getInstance()
    val defaultStartTime = config.defaultStartTime
    val currHour = calendar.get(Calendar.HOUR_OF_DAY)
    var dateTime = Formatter.getLocalDateTimeFromCode(dayCode).withHourOfDay(currHour)
    var newDateTime = dateTime.plusHours(1).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
    if (!allowChangingDay && dateTime.dayOfMonth() != newDateTime.dayOfMonth()) {
        newDateTime = newDateTime.minusDays(1)
    }

    return when (defaultStartTime) {
        DEFAULT_START_TIME_CURRENT_TIME -> {
            val currMinutes = calendar.get(Calendar.MINUTE)
            dateTime.withMinuteOfHour(currMinutes).seconds()
        }
        DEFAULT_START_TIME_NEXT_FULL_HOUR -> newDateTime.seconds()
        else -> {
            val hours = defaultStartTime / 60
            val minutes = defaultStartTime % 60
            dateTime = Formatter.getLocalDateTimeFromCode(dayCode).withHourOfDay(hours).withMinuteOfHour(minutes)
            newDateTime = dateTime

            // make sure the date doesn't change
            newDateTime.withDate(dateTime.year, dateTime.monthOfYear, dateTime.dayOfMonth).seconds()
        }
    }
}

fun Context.getSyncedCalDAVCalendars() = calDAVHelper.getCalDAVCalendars(config.caldavSyncedCalendarIds, false)

fun Context.recheckCalDAVCalendars(scheduleNextCalDAVSync: Boolean, callback: () -> Unit) {
    if (config.caldavSync) {
        ensureBackgroundThread {
            calDAVHelper.refreshCalendars(false, scheduleNextCalDAVSync, callback)
            updateWidgets()
        }
    }
}

fun Context.scheduleCalDAVSync(activate: Boolean) {
    val syncIntent = Intent(applicationContext, CalDAVSyncReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        SCHEDULE_CALDAV_REQUEST_CODE,
        syncIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarm.cancel(pendingIntent)

    if (activate) {
        val syncCheckInterval = 2 * AlarmManager.INTERVAL_HOUR
        try {
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + syncCheckInterval, syncCheckInterval, pendingIntent)
        } catch (ignored: Exception) {
        }
    }
}

fun Context.addDayEvents(day: DayMonthly, linearLayout: LinearLayout, res: Resources, dividerMargin: Int) {
    val eventLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    day.dayEvents.sortedWith(compareBy<Event> {
        if (it.getIsAllDay()) {
            Formatter.getDayStartTS(Formatter.getDayCodeFromTS(it.startTS)) - 1
        } else {
            it.startTS
        }
    }.thenBy {
        if (it.getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(it.endTS))
        } else {
            it.endTS
        }
    }.thenBy { it.title }).forEach {
        val backgroundDrawable = res.getDrawable(R.drawable.day_monthly_event_background)
        backgroundDrawable.applyColorFilter(it.color)
        eventLayoutParams.setMargins(dividerMargin, 0, dividerMargin, dividerMargin)

        var textColor = it.color.getContrastColor()
        if (!day.isThisMonth) {
            backgroundDrawable.alpha = 64
            textColor = textColor.adjustAlpha(0.25f)
        }

        (View.inflate(applicationContext, R.layout.day_monthly_event_view, null) as ConstraintLayout).apply {
            background = backgroundDrawable
            layoutParams = eventLayoutParams
            linearLayout.addView(this)

            day_monthly_event_id.apply {
                setTextColor(textColor)
                text = it.title.replace(" ", "\u00A0")  // allow word break by char
                checkViewStrikeThrough(it.isTaskCompleted())
                contentDescription = it.title
            }

            day_monthly_task_image.beVisibleIf(it.isTask())
            if (it.isTask()) {
                day_monthly_task_image.applyColorFilter(textColor)
            }
        }
    }
}

fun Context.getEventListItems(events: List<Event>, addSectionDays: Boolean = true, addSectionMonths: Boolean = true): ArrayList<ListItem> {
    val listItems = ArrayList<ListItem>(events.size)
    val replaceDescription = config.replaceDescription

    // move all-day events in front of others
    val sorted = events.sortedWith(compareBy<Event> {
        if (it.getIsAllDay()) {
            Formatter.getDayStartTS(Formatter.getDayCodeFromTS(it.startTS)) - 1
        } else {
            it.startTS
        }
    }.thenBy {
        if (it.getIsAllDay()) {
            Formatter.getDayEndTS(Formatter.getDayCodeFromTS(it.endTS))
        } else {
            it.endTS
        }
    }.thenBy { it.title }.thenBy { if (replaceDescription) it.location else it.description })

    var prevCode = ""
    var prevMonthLabel = ""
    val now = getNowSeconds()
    val todayCode = Formatter.getDayCodeFromTS(now)

    sorted.forEach {
        val code = Formatter.getDayCodeFromTS(it.startTS)
        if (addSectionMonths) {
            val monthLabel = Formatter.getLongMonthYear(this, code)
            if (monthLabel != prevMonthLabel) {
                val listSectionMonth = ListSectionMonth(monthLabel)
                listItems.add(listSectionMonth)
                prevMonthLabel = monthLabel
            }
        }

        if (code != prevCode && addSectionDays) {
            val day = Formatter.getDateDayTitle(code)
            val isToday = code == todayCode
            val listSectionDay = ListSectionDay(day, code, isToday, !isToday && it.startTS < now)
            listItems.add(listSectionDay)
            prevCode = code
        }

        val listEvent =
            ListEvent(
                it.id!!,
                it.startTS,
                it.endTS,
                it.title,
                it.description,
                it.getIsAllDay(),
                it.color,
                it.location,
                it.isPastEvent,
                it.repeatInterval > 0,
                it.isTask(),
                it.isTaskCompleted()
            )
        listItems.add(listEvent)
    }
    return listItems
}

fun Context.handleEventDeleting(eventIds: List<Long>, timestamps: List<Long>, action: Int) {
    when (action) {
        DELETE_SELECTED_OCCURRENCE -> {
            eventIds.forEachIndexed { index, value ->
                eventsHelper.addEventRepetitionException(value, timestamps[index], true)
            }
        }
        DELETE_FUTURE_OCCURRENCES -> {
            eventIds.forEachIndexed { index, value ->
                eventsHelper.addEventRepeatLimit(value, timestamps[index])
            }
        }
        DELETE_ALL_OCCURRENCES -> {
            eventsHelper.deleteEvents(eventIds.toMutableList(), true)
        }
    }
}

fun Context.refreshCalDAVCalendars(ids: String, showToasts: Boolean) {
    val uri = CalendarContract.Calendars.CONTENT_URI
    val accounts = HashSet<Account>()
    val calendars = calDAVHelper.getCalDAVCalendars(ids, showToasts)
    calendars.forEach {
        accounts.add(Account(it.accountName, it.accountType))
    }

    Bundle().apply {
        putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        accounts.forEach {
            ContentResolver.requestSync(it, uri.authority, this)
        }
    }
}

fun Context.getWidgetFontSize() = when (config.fontSize) {
    FONT_SIZE_SMALL -> getWidgetSmallFontSize()
    FONT_SIZE_MEDIUM -> getWidgetMediumFontSize()
    FONT_SIZE_LARGE -> getWidgetLargeFontSize()
    else -> getWidgetExtraLargeFontSize()
}

fun Context.getWidgetSmallFontSize() = getWidgetMediumFontSize() - 3f
fun Context.getWidgetMediumFontSize() = resources.getDimension(R.dimen.day_text_size) / resources.displayMetrics.density
fun Context.getWidgetLargeFontSize() = getWidgetMediumFontSize() + 3f
fun Context.getWidgetExtraLargeFontSize() = getWidgetMediumFontSize() + 6f

fun Context.getWeeklyViewItemHeight(): Float {
    val defaultHeight = resources.getDimension(R.dimen.weekly_view_row_height)
    val multiplier = config.weeklyViewItemHeightMultiplier
    return defaultHeight * multiplier
}

fun Context.printBitmap(bitmap: Bitmap) {
    val printHelper = PrintHelper(this)
    printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
    printHelper.orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    printHelper.printBitmap(getString(R.string.app_name), bitmap)
}

fun Context.editEvent(event: ListEvent) {
    Intent(this, getActivityToOpen(event.isTask)).apply {
        putExtra(EVENT_ID, event.id)
        putExtra(EVENT_OCCURRENCE_TS, event.startTS)
        startActivity(this)
    }
}

fun Context.getDatesWeekDateTime(date: DateTime): String {
    return if (!config.startWeekWithCurrentDay) {
        val currentOffsetHours = TimeZone.getDefault().rawOffset / 1000 / 60 / 60

        // not great, not terrible
        val useHours = if (currentOffsetHours >= 10) 8 else 12
        var thisweek = date.withZone(DateTimeZone.UTC).withDayOfWeek(1).withHourOfDay(useHours).minusDays(if (config.isSundayFirst) 1 else 0)
        if (date.minusDays(7).seconds() > thisweek.seconds()) {
            thisweek = thisweek.plusDays(7)
        }
        thisweek.toString()
    } else {
        date.withZone(DateTimeZone.UTC).toString()
    }
}
