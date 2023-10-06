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
import android.database.Cursor
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import androidx.print.PrintHelper
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.EventActivity
import com.simplemobiletools.calendar.pro.activities.EventTypePickerActivity
import com.simplemobiletools.calendar.pro.activities.SnoozeReminderActivity
import com.simplemobiletools.calendar.pro.activities.TaskActivity
import com.simplemobiletools.calendar.pro.databases.EventsDatabase
import com.simplemobiletools.calendar.pro.databinding.DayMonthlyEventViewBinding
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.interfaces.EventTypesDao
import com.simplemobiletools.calendar.pro.interfaces.EventsDao
import com.simplemobiletools.calendar.pro.interfaces.TasksDao
import com.simplemobiletools.calendar.pro.interfaces.WidgetsDao
import com.simplemobiletools.calendar.pro.models.*
import com.simplemobiletools.calendar.pro.receivers.AutomaticBackupReceiver
import com.simplemobiletools.calendar.pro.receivers.CalDAVSyncReceiver
import com.simplemobiletools.calendar.pro.receivers.NotificationReceiver
import com.simplemobiletools.calendar.pro.services.MarkCompletedService
import com.simplemobiletools.calendar.pro.services.SnoozeService
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.LocalDate
import java.io.File
import java.io.FileOutputStream
import java.util.*

val Context.config: Config get() = Config.newInstance(applicationContext)
val Context.eventsDB: EventsDao get() = EventsDatabase.getInstance(applicationContext).EventsDao()
val Context.eventTypesDB: EventTypesDao get() = EventsDatabase.getInstance(applicationContext).EventTypesDao()
val Context.widgetsDB: WidgetsDao get() = EventsDatabase.getInstance(applicationContext).WidgetsDao()
val Context.completedTasksDB: TasksDao get() = EventsDatabase.getInstance(applicationContext).TasksDao()
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
    val events = eventsDB.getEventsOrTasksAtReboot(getNowSeconds())
    events.forEach {
        scheduleNextEventReminder(it, false)
    }
}

fun Context.scheduleNextEventReminder(event: Event, showToasts: Boolean) {
    val validReminders = event.getReminders().filter { it.type == REMINDER_NOTIFICATION }
    if (validReminders.isEmpty()) {
        if (showToasts) {
            toast(com.simplemobiletools.commons.R.string.saving)
        }
        return
    }

    val now = getNowSeconds()
    val reminderSeconds = validReminders.reversed().map { it.minutes * 60 }
    val isTask = event.isTask()
    eventsHelper.getEvents(now, now + YEAR, event.id!!, false) { events ->
        if (events.isNotEmpty()) {
            for (curEvent in events) {
                if (isTask && curEvent.isTaskCompleted()) {
                    // skip scheduling reminders for completed tasks
                    continue
                }

                for (curReminder in reminderSeconds) {
                    if (curEvent.getEventStartTS() - curReminder > now) {
                        scheduleEventIn((curEvent.getEventStartTS() - curReminder) * 1000L, curEvent, showToasts)
                        return@getEvents
                    }
                }
            }
        }

        if (showToasts) {
            toast(com.simplemobiletools.commons.R.string.saving)
        }
    }
}

fun Context.scheduleEventIn(notifyAtMillis: Long, event: Event, showToasts: Boolean) {
    val now = System.currentTimeMillis()
    if (notifyAtMillis < now) {
        if (showToasts) {
            toast(com.simplemobiletools.commons.R.string.saving)
        }
        return
    }

    val newNotifyAtMillis = notifyAtMillis + 1000
    if (showToasts) {
        val secondsTillNotification = (newNotifyAtMillis - now) / 1000
        val msg = String.format(getString(com.simplemobiletools.commons.R.string.time_remaining), formatSecondsToTimeString(secondsTillNotification.toInt()))
        toast(msg)
    }

    val pendingIntent = getNotificationIntent(event)
    setExactAlarm(newNotifyAtMillis, pendingIntent)
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

fun Context.getAutomaticBackupIntent(): PendingIntent {
    val intent = Intent(this, AutomaticBackupReceiver::class.java)
    return PendingIntent.getBroadcast(this, AUTOMATIC_BACKUP_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

fun Context.scheduleNextAutomaticBackup() {
    if (config.autoBackup) {
        val backupAtMillis = getNextAutoBackupTime().millis
        val pendingIntent = getAutomaticBackupIntent()
        setExactAlarm(backupAtMillis, pendingIntent)
    }
}

fun Context.cancelScheduledAutomaticBackup() = getAlarmManager().cancel(getAutomaticBackupIntent())

fun Context.checkAndBackupEventsOnBoot() {
    if (config.autoBackup) {
        val previousRealBackupTime = config.lastAutoBackupTime
        val previousScheduledBackupTime = getPreviousAutoBackupTime().seconds()
        val missedPreviousBackup = previousRealBackupTime < previousScheduledBackupTime
        if (missedPreviousBackup) {
            // device was probably off at the scheduled time so backup now
            backupEventsAndTasks()
        }
    }
}

fun Context.backupEventsAndTasks() {
    require(isRPlus())
    ensureBackgroundThread {
        val config = config
        val events = eventsHelper.getEventsToExport(
            eventTypes = config.autoBackupEventTypes.map { it.toLong() } as ArrayList<Long>,
            exportEvents = config.autoBackupEvents,
            exportTasks = config.autoBackupTasks,
            exportPastEntries = config.autoBackupPastEntries
        )
        if (events.isEmpty()) {
            toast(com.simplemobiletools.commons.R.string.no_entries_for_exporting)
            config.lastAutoBackupTime = getNowSeconds()
            scheduleNextAutomaticBackup()
            return@ensureBackgroundThread
        }

        val now = DateTime.now()
        val year = now.year.toString()
        val month = now.monthOfYear.ensureTwoDigits()
        val day = now.dayOfMonth.ensureTwoDigits()
        val hours = now.hourOfDay.ensureTwoDigits()
        val minutes = now.minuteOfHour.ensureTwoDigits()
        val seconds = now.secondOfMinute.ensureTwoDigits()

        val filename = config.autoBackupFilename
            .replace("%Y", year, false)
            .replace("%M", month, false)
            .replace("%D", day, false)
            .replace("%h", hours, false)
            .replace("%m", minutes, false)
            .replace("%s", seconds, false)

        val outputFolder = File(config.autoBackupFolder).apply {
            mkdirs()
        }

        var exportFile = File(outputFolder, "$filename.ics")
        var exportFilePath = exportFile.absolutePath
        val outputStream = try {
            if (hasProperStoredFirstParentUri(exportFilePath)) {
                val exportFileUri = createDocumentUriUsingFirstParentTreeUri(exportFilePath)
                if (!getDoesFilePathExist(exportFilePath)) {
                    createSAFFileSdk30(exportFilePath)
                }
                applicationContext.contentResolver.openOutputStream(exportFileUri, "wt") ?: FileOutputStream(exportFile)
            } else {
                var num = 0
                while (getDoesFilePathExist(exportFilePath) && !exportFile.canWrite()) {
                    num++
                    exportFile = File(outputFolder, "${filename}_${num}.ics")
                    exportFilePath = exportFile.absolutePath
                }
                FileOutputStream(exportFile)
            }
        } catch (e: Exception) {
            showErrorToast(e)
            null
        }

        IcsExporter(this).exportEvents(outputStream, events, showExportingToast = false) { result ->
            when (result) {
                IcsExporter.ExportResult.EXPORT_PARTIAL -> toast(com.simplemobiletools.commons.R.string.exporting_some_entries_failed)
                IcsExporter.ExportResult.EXPORT_FAIL -> toast(com.simplemobiletools.commons.R.string.exporting_failed)
                else -> {}
            }
            MediaScannerConnection.scanFile(
                this,
                arrayOf(exportFilePath),
                arrayOf(exportFilePath.getMimeType())
            ) { _, _ -> }

            config.lastAutoBackupTime = getNowSeconds()
        }
        scheduleNextAutomaticBackup()
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
            seconds % YEAR == 0 -> resources.getQuantityString(com.simplemobiletools.commons.R.plurals.years, seconds / YEAR, seconds / YEAR)
            seconds % MONTH == 0 -> resources.getQuantityString(com.simplemobiletools.commons.R.plurals.months, seconds / MONTH, seconds / MONTH)
            seconds % WEEK == 0 -> resources.getQuantityString(com.simplemobiletools.commons.R.plurals.weeks, seconds / WEEK, seconds / WEEK)
            else -> resources.getQuantityString(com.simplemobiletools.commons.R.plurals.days, seconds / DAY, seconds / DAY)
        }
    }
}

fun Context.notifyRunningEvents() {
    eventsHelper.getRunningEventsOrTasks()
        .filter { it.getReminders().any { reminder -> reminder.type == REMINDER_NOTIFICATION } }
        .forEach {
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
            val firstReminderMinutes =
                arrayOf(currEvent.reminder3Minutes, currEvent.reminder2Minutes, currEvent.reminder1Minutes).filter { it != REMINDER_OFF }.max()
            if (eventStartTS - firstReminderMinutes * 60 > currentSeconds) {
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
        LocalDate.now().plusDays(1) -> getString(com.simplemobiletools.commons.R.string.tomorrow)
        else -> "${Formatter.getDateFromCode(this, Formatter.getDayCodeFromTS(event.startTS))},"
    }

    val timeRange = if (event.getIsAllDay()) getString(R.string.all_day) else getFormattedEventTime(startTime, endTime)
    val descriptionOrLocation = if (config.replaceDescription) event.location else event.description
    val content = "$displayedStartDate $timeRange $descriptionOrLocation".trim()
    ensureBackgroundThread {
        if (event.isTask()) eventsHelper.updateIsTaskCompleted(event)
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
        .apply {
            if (event.isTask() && !event.isTaskCompleted()) {
                addAction(R.drawable.ic_task_vector, getString(R.string.mark_completed), getMarkCompletedPendingIntent(this@getNotification, event))
            }
            addAction(
                com.simplemobiletools.commons.R.drawable.ic_snooze_vector,
                getString(com.simplemobiletools.commons.R.string.snooze),
                getSnoozePendingIntent(this@getNotification, event)
            )
        }

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
    val activityClass = getActivityToOpen(event.isTask())
    val intent = Intent(context, activityClass)
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

private fun getMarkCompletedPendingIntent(context: Context, task: Event): PendingIntent {
    val intent = Intent(context, MarkCompletedService::class.java).setAction(ACTION_MARK_COMPLETED)
    intent.putExtra(EVENT_ID, task.id)
    return PendingIntent.getService(context, task.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
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

fun Context.launchNewEventOrTaskActivity() {
    if (config.allowCreatingTasks) {
        Intent(this, EventTypePickerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
        }
    } else {
        launchNewEventIntent()
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
    val alarmManager = getAlarmManager()
    alarmManager.cancel(pendingIntent)

    if (activate) {
        val syncCheckInterval = 2 * AlarmManager.INTERVAL_HOUR
        try {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + syncCheckInterval, syncCheckInterval, pendingIntent)
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

        DayMonthlyEventViewBinding.inflate(LayoutInflater.from(this)).apply {
            root.background = backgroundDrawable
            root.layoutParams = eventLayoutParams
            linearLayout.addView(root)

            dayMonthlyEventId.apply {
                setTextColor(textColor)
                text = it.title.replace(" ", "\u00A0")  // allow word break by char
                checkViewStrikeThrough(it.shouldStrikeThrough())
                contentDescription = it.title
            }

            dayMonthlyTaskImage.beVisibleIf(it.isTask())
            if (it.isTask()) {
                dayMonthlyTaskImage.applyColorFilter(textColor)
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
                it.isTaskCompleted(),
                it.isAttendeeInviteDeclined()
            )
        listItems.add(listEvent)
    }
    return listItems
}

fun Context.handleEventDeleting(eventIds: List<Long>, timestamps: List<Long>, action: Int) {
    when (action) {
        DELETE_SELECTED_OCCURRENCE -> {
            eventIds.forEachIndexed { index, value ->
                eventsHelper.deleteRepeatingEventOccurrence(value, timestamps[index], true)
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
        if (showToasts) {
            // Assume this is a manual synchronisation when we showToasts to the user (swipe refresh, MainMenu-> refresh caldav calendars, ...)
            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
        }
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
        putExtra(IS_TASK_COMPLETED, event.isTaskCompleted)
        startActivity(this)
    }
}

fun Context.getFirstDayOfWeek(date: DateTime): String {
    return getFirstDayOfWeekDt(date).toString()
}

fun Context.getFirstDayOfWeekDt(date: DateTime): DateTime {
    val currentDate = date.withTimeAtStartOfDay()
    if (config.startWeekWithCurrentDay) {
        return currentDate
    } else {
        val firstDayOfWeek = config.firstDayOfWeek
        val currentDayOfWeek = currentDate.dayOfWeek
        return if (currentDayOfWeek == firstDayOfWeek) {
            currentDate
        } else {
            // Joda-time's weeks always starts on Monday but user preferred firstDayOfWeek could be any week day
            if (firstDayOfWeek < currentDayOfWeek) {
                currentDate.withDayOfWeek(firstDayOfWeek)
            } else {
                currentDate.minusWeeks(1).withDayOfWeek(firstDayOfWeek)
            }
        }
    }
}

fun Context.getDayOfWeekString(dayOfWeek: Int): String {
    val dayOfWeekResId = when (dayOfWeek) {
        DateTimeConstants.MONDAY -> com.simplemobiletools.commons.R.string.monday
        DateTimeConstants.TUESDAY -> com.simplemobiletools.commons.R.string.tuesday
        DateTimeConstants.WEDNESDAY -> com.simplemobiletools.commons.R.string.wednesday
        DateTimeConstants.THURSDAY -> com.simplemobiletools.commons.R.string.thursday
        DateTimeConstants.FRIDAY -> com.simplemobiletools.commons.R.string.friday
        DateTimeConstants.SATURDAY -> com.simplemobiletools.commons.R.string.saturday
        DateTimeConstants.SUNDAY -> com.simplemobiletools.commons.R.string.sunday
        else -> throw IllegalArgumentException("Invalid day: $dayOfWeek")
    }

    return getString(dayOfWeekResId)
}

// format day bits to strings like "Mon, Tue, Wed"
fun Context.getShortDaysFromBitmask(bitMask: Int): String {
    val dayBits = withFirstDayOfWeekToFront(listOf(MONDAY_BIT, TUESDAY_BIT, WEDNESDAY_BIT, THURSDAY_BIT, FRIDAY_BIT, SATURDAY_BIT, SUNDAY_BIT))
    val weekDays = withFirstDayOfWeekToFront(resources.getStringArray(com.simplemobiletools.commons.R.array.week_days_short).toList())

    var days = ""
    dayBits.forEachIndexed { index, bit ->
        if (bitMask and bit != 0) {
            days += "${weekDays[index]}, "
        }
    }

    return days.trim().trimEnd(',')
}

fun <T> Context.withFirstDayOfWeekToFront(weekItems: Collection<T>): ArrayList<T> {
    val firstDayOfWeek = config.firstDayOfWeek
    if (firstDayOfWeek == DateTimeConstants.MONDAY) {
        return weekItems.toMutableList() as ArrayList<T>
    }

    val firstDayOfWeekIndex = config.firstDayOfWeek - 1
    val rotatedWeekItems = weekItems.drop(firstDayOfWeekIndex) + weekItems.take(firstDayOfWeekIndex)
    return rotatedWeekItems as ArrayList<T>
}

fun Context.getProperDayIndexInWeek(date: DateTime): Int {
    val firstDayOfWeek = config.firstDayOfWeek
    val dayOfWeek = date.dayOfWeek
    val dayIndex = if (dayOfWeek >= firstDayOfWeek) {
        dayOfWeek - firstDayOfWeek
    } else {
        dayOfWeek + (7 - firstDayOfWeek)
    }

    return dayIndex
}

fun Context.isWeekendIndex(dayIndex: Int): Boolean {
    val firstDayOfWeek = config.firstDayOfWeek
    val shiftedIndex = (dayIndex + firstDayOfWeek) % 7
    val dayOfWeek = if (shiftedIndex == 0) {
        DateTimeConstants.SUNDAY
    } else {
        shiftedIndex
    }

    return isWeekend(dayOfWeek)
}

fun Context.isTaskCompleted(event: Event): Boolean {
    if (event.id == null) return false
    val originalEvent = eventsDB.getTaskWithId(event.id!!)
    val task = completedTasksDB.getTaskWithIdAndTs(event.id!!, event.startTS)
    return originalEvent?.isTaskCompleted() == true || task?.isTaskCompleted() == true
}

fun Context.updateTaskCompletion(event: Event, completed: Boolean) {
    if (completed) {
        event.flags = event.flags or FLAG_TASK_COMPLETED
        val task = Task(null, event.id!!, event.startTS, event.flags)
        completedTasksDB.insertOrUpdate(task)
    } else {
        event.flags = event.flags.removeBit(FLAG_TASK_COMPLETED)
        completedTasksDB.deleteTaskWithIdAndTs(event.id!!, event.startTS)
    }

    // remove existing notification (if any) and schedule a new one if needed
    cancelPendingIntent(event.id!!)
    cancelNotification(event.id!!)
    scheduleNextEventReminder(event, showToasts = false)

    // mark event as "incomplete" in the main events db
    eventsDB.updateTaskCompletion(event.id!!, event.flags.removeBit(FLAG_TASK_COMPLETED))
}

// same as Context.queryCursor but inlined to allow non-local returns
inline fun Context.queryCursorInlined(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showErrors) {
            showErrorToast(e)
        }
    }
}

fun Context.addImportIdsToTasks(callback: () -> Unit) {
    ensureBackgroundThread {
        var count = 0

        eventsDB.getAllTasks().forEach { task ->
            if (task.importId.isEmpty()) {
                eventsDB.updateTaskImportId(
                    importId = generateImportId(),
                    id = task.id!!
                )
                count += 1
            }
        }

        if (count > 0) {
            callback()
        }
    }
}

fun Context.getAlarmManager() = getSystemService(Context.ALARM_SERVICE) as AlarmManager

fun Context.setExactAlarm(triggerAtMillis: Long, operation: PendingIntent, type: Int = AlarmManager.RTC_WAKEUP) {
    val alarmManager = getAlarmManager()
    try {
        if (isSPlus() && alarmManager.canScheduleExactAlarms() || !isSPlus()) {
            alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(type, triggerAtMillis, operation)
        }
    } catch (e: Exception) {
        showErrorToast(e)
    }
}
