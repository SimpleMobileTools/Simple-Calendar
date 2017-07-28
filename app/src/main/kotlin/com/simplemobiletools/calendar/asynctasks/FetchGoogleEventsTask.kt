package com.simplemobiletools.calendar.asynctasks

import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.util.SparseIntArray
import android.widget.Toast
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.interfaces.GoogleSyncListener
import com.simplemobiletools.calendar.models.*
import com.simplemobiletools.commons.extensions.isOnMainThread
import com.simplemobiletools.commons.extensions.toast
import org.joda.time.DateTime
import java.util.*

// more info about event fields at https://developers.google.com/google-apps/calendar/v3/reference/events/insert
class FetchGoogleEventsTask(val context: Context, val googleSyncListener: GoogleSyncListener? = null) : AsyncTask<Void, Void, String>() {
    private val ITEMS = "items"
    private val NEXT_PAGE_TOKEN = "nextPageToken"

    private var dbHelper = context.dbHelper
    private var eventTypes = ArrayList<EventType>()
    private var eventColors = SparseIntArray()
    private var service = context.getGoogleSyncService()
    private var parseError: Exception? = null
    private var noTitleText = context.getString(R.string.no_title)

    override fun doInBackground(vararg params: Void): String {
        if (!context.isGoogleSyncActive() || !context.isOnline())
            return ""

        // always handle queued operations before fetching new data
        val queuedOperations = context.googleSyncQueue.getOperations()
        queuedOperations.forEach {
            when (it.operation) {
                OPERATION_INSERT -> {
                    val event = dbHelper.getEventWithId(it.eventId)
                    if (event != null)
                        GoogleSyncHandler().insertToGoogle(context, event)
                    context.googleSyncQueue.clearOperationsOf(it.eventId)
                }
                OPERATION_UPDATE -> {
                    val event = dbHelper.getEventWithId(it.eventId)
                    if (event != null)
                        GoogleSyncHandler().updateGoogleEvent(context, event)
                    context.googleSyncQueue.clearOperationsOf(it.eventId)
                }
                OPERATION_DELETE -> {
                    GoogleSyncHandler().deleteFromGoogle(context, it.importId)
                    context.googleSyncQueue.clearOperationsOf(it.eventId)
                }
            }
        }

        try {
            getColors()
            getDataFromApi()
        } catch (e: Exception) {
            parseError = e
        }
        googleSyncListener?.syncCompleted()
        return ""
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        if (context.isOnMainThread() && parseError != null && parseError is GoogleJsonResponseException) {
            val msg = String.format(context.getString(R.string.google_sync_error_fetch), parseError!!.getGoogleMessageError())
            context.toast(msg, Toast.LENGTH_LONG)
        }
    }

    private fun getColors() {
        val colors = service.colors().get().execute()
        for ((id, color) in colors.event.entries) {
            eventColors.put(id.toInt(), Color.parseColor(color.background))
        }
    }

    private fun getDataFromApi() {
        var currToken = ""
        while (true) {
            service.colors().get().execute()
            val events = service.events().list(PRIMARY)
                    .setPageToken(currToken)
                    .execute()

            for ((key, value) in events) {
                if (key == ITEMS) {
                    eventTypes = dbHelper.fetchEventTypes()
                    val localGoogleEvents = dbHelper.getGoogleSyncEvents()
                    val remoteGoogleEvents = parseEvents(value.toString())
                    removeRedundantLocalEvents(localGoogleEvents, remoteGoogleEvents)
                }
            }

            if (events.containsKey(NEXT_PAGE_TOKEN)) {
                currToken = events[NEXT_PAGE_TOKEN] as String
            } else {
                break
            }
        }
    }

    private fun parseEvents(json: String): ArrayList<String> {
        val remoteImportIds = ArrayList<String>()
        var updateEvent = false
        var eventId = 0
        val importIDs = context.dbHelper.getImportIds()
        val token = object : TypeToken<List<GoogleEvent>>() {}.type
        val googleEvents = Gson().fromJson<ArrayList<GoogleEvent>>(json, token) ?: ArrayList<GoogleEvent>(8)
        for (googleEvent in googleEvents) {
            val importId = googleEvent.id
            remoteImportIds.add(importId)
            if (!googleEvent.status.equals(CONFIRMED, true))
                continue

            val lastUpdate = DateTime(googleEvent.updated).millis
            if (importIDs.contains(importId)) {
                val oldEvent = dbHelper.getEventWithImportId(importId)
                if (oldEvent != null) {
                    if (oldEvent.lastUpdated >= lastUpdate) {
                        continue
                    } else {
                        updateEvent = true
                        eventId = oldEvent.id
                    }
                }
            }

            val start = googleEvent.start
            val end = googleEvent.end
            var startTS: Int
            var endTS: Int
            var flags = 0

            if (start.date != null) {
                startTS = DateTime(start.date).withHourOfDay(1).seconds()
                endTS = DateTime(end.date).withHourOfDay(1).seconds()
                flags = flags or FLAG_ALL_DAY
            } else {
                startTS = DateTime(start.dateTime).seconds()
                endTS = DateTime(end.dateTime).seconds()
            }

            val summary = googleEvent.summary ?: "($noTitleText)"
            val description = googleEvent.description ?: ""
            val reminders = getReminders(googleEvent.reminders)
            val repeatRule = getRepeatRule(googleEvent, startTS)
            val eventTypeId = getEventTypeId(googleEvent.colorId)
            val event = Event(eventId, startTS, endTS, summary, description, reminders.getOrElse(0, { -1 }),
                    reminders.getOrElse(1, { -1 }), reminders.getOrElse(2, { -1 }), repeatRule.repeatInterval, importId, flags, repeatRule.repeatLimit,
                    repeatRule.repeatRule, eventTypeId, lastUpdated = lastUpdate, source = SOURCE_GOOGLE_SYNC)

            if (event.getIsAllDay() && endTS > startTS) {
                event.endTS -= DAY
            }

            if (updateEvent) {
                dbHelper.update(event) {}
            } else {
                importIDs.add(importId)
                dbHelper.insert(event) {}
            }
        }
        return remoteImportIds
    }

    private fun removeRedundantLocalEvents(localGoogleEvents: List<Event>, remoteGoogleEvents: ArrayList<String>) {
        val filtered = localGoogleEvents.filter { !remoteGoogleEvents.contains(it.importId) }
        val filteredIds = Array(filtered.size, { i -> (filtered[i].id.toString()) })
        dbHelper.deleteEvents(filteredIds, false)
    }

    private fun getEventTypeId(colorId: Int): Int {
        var eventTypeId = -1
        val eventType = "google_sync_$colorId"
        eventTypes.forEach {
            if (it.title.toLowerCase() == eventType)
                eventTypeId = it.id
        }

        if (eventTypeId == -1) {
            val newColor = if (eventColors[colorId] != 0) eventColors[colorId] else context.config.googleDefaultEventColor
            val newEventType = EventType(0, eventType, newColor)
            eventTypeId = dbHelper.insertEventType(newEventType)
            eventTypes.add(newEventType)
        }

        return eventTypeId
    }

    private fun getRepeatRule(googleEvent: GoogleEvent, startTS: Int): RepeatRule {
        val recurrence = googleEvent.recurrence?.firstOrNull()
        return if (recurrence != null) {
            Parser().parseRepeatInterval(recurrence.toString().trim('\"').substring(RRULE.length), startTS)
        } else {
            RepeatRule(0, 0, 0)
        }
    }

    private fun getReminders(json: JsonObject?): List<Int> {
        val reminderMinutes = ArrayList<Int>()
        if (json?.has(OVERRIDES) == true) {
            val array = json.getAsJsonArray(OVERRIDES)
            val token = object : TypeToken<List<GoogleEventReminder>>() {}.type
            val reminders = Gson().fromJson<ArrayList<GoogleEventReminder>>(array, token) ?: ArrayList<GoogleEventReminder>(2)
            for ((method, minutes) in reminders) {
                if (method == POPUP) {
                    reminderMinutes.add(minutes)
                }
            }
        } else if (json?.has(USE_DEFAULT) == true) {
            val minutes = context.config.googleDefaultReminders.splitToSequence(',')
            minutes.forEach {
                reminderMinutes.add(it.toInt())
            }
        }
        return reminderMinutes
    }
}
