package com.simplemobiletools.calendar.asynctasks

import android.app.Activity
import android.os.AsyncTask
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SettingsActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.FLAG_ALL_DAY
import com.simplemobiletools.calendar.helpers.Parser
import com.simplemobiletools.calendar.helpers.RRULE
import com.simplemobiletools.calendar.models.*
import org.joda.time.DateTime

// more info about event fields at https://developers.google.com/google-apps/calendar/v3/reference/events/insert
class FetchGoogleEventsTask(val activity: Activity, credential: GoogleAccountCredential) : AsyncTask<Void, Void, List<Event>>() {
    private val CONFIRMED = "confirmed"
    private val PRIMARY = "primary"
    private val ITEMS = "items"
    private val OVERRIDES = "overrides"
    private val POPUP = "popup"
    private val NEXT_PAGE_TOKEN = "nextPageToken"

    private var service: com.google.api.services.calendar.Calendar
    private var lastError: Exception? = null
    private var dbHelper = activity.dbHelper

    init {
        val transport = AndroidHttp.newCompatibleTransport()
        service = com.google.api.services.calendar.Calendar.Builder(transport, GsonFactory(), credential)
                .setApplicationName(activity.resources.getString(R.string.app_name))
                .build()
    }

    override fun doInBackground(vararg params: Void): List<Event>? {
        return try {
            getDataFromApi()
        } catch (e: Exception) {
            lastError = e
            cancel(true)
            null
        }
    }

    private fun getDataFromApi(): List<Event> {
        val parsedEvents = ArrayList<Event>()
        var currToken = ""
        while (true) {
            val events = service.events().list(PRIMARY)
                    .setPageToken(currToken)
                    .execute()

            for (event in events) {
                if (event.key == ITEMS) {
                    dbHelper.getEventTypes {
                        parseEvents(event.value.toString(), it)
                    }
                }
            }

            if (events.containsKey(NEXT_PAGE_TOKEN)) {
                currToken = events[NEXT_PAGE_TOKEN] as String
            } else {
                break
            }
        }

        return parsedEvents
    }

    private fun parseEvents(json: String, eventTypes: ArrayList<EventType>): List<Event> {
        val events = ArrayList<Event>()
        val token = object : TypeToken<List<GoogleEvent>>() {}.type
        val googleEvents = Gson().fromJson<ArrayList<GoogleEvent>>(json, token) ?: ArrayList<GoogleEvent>(8)
        for (googleEvent in googleEvents) {
            if (googleEvent.status != CONFIRMED)
                continue

            val reminders = getReminder(googleEvent.reminders)
            val start = googleEvent.start
            val end = googleEvent.end
            var startTS: Int
            var endTS: Int
            var flags = 0
            var eventTypeId = -1
            val eventType = "google_${googleEvent.colorId}"
            eventTypes.forEach {
                if (it.title.toLowerCase() == eventType)
                    eventTypeId = it.id
            }

            if (eventTypeId == -1) {
                val newEventType = EventType(0, eventType, activity.config.primaryColor)
                eventTypeId = dbHelper.insertEventType(newEventType)
                eventTypes.add(newEventType)
            }

            if (start.date != null) {
                startTS = DateTime(start.date).withHourOfDay(1).seconds()
                endTS = DateTime(end.date).withHourOfDay(1).seconds()
                flags = flags or FLAG_ALL_DAY
            } else {
                startTS = DateTime(start.dateTime).seconds()
                endTS = DateTime(end.dateTime).seconds()
            }

            val recurrence = googleEvent.recurrence?.firstOrNull()
            var repeatRule = RepeatRule(0, 0, 0)
            if (recurrence != null) {
                repeatRule = Parser().parseRepeatInterval(recurrence.toString().trim('\"').substring(RRULE.length), startTS)
            }

            Event(0, startTS, endTS, googleEvent.summary, googleEvent.description, reminders.getOrElse(0, { -1 }), reminders.getOrElse(1, { -1 }),
                    reminders.getOrElse(2, { -1 }), repeatRule.repeatInterval, googleEvent.iCalUID, flags, repeatRule.repeatLimit, repeatRule.repeatRule,
                    eventTypeId)
        }
        return events
    }

    private fun getReminder(json: JsonObject): List<Int> {
        val array = json.getAsJsonArray(OVERRIDES)
        val token = object : TypeToken<List<GoogleEventReminder>>() {}.type
        val reminders = Gson().fromJson<ArrayList<GoogleEventReminder>>(array, token) ?: ArrayList<GoogleEventReminder>(2)
        val reminderMinutes = ArrayList<Int>()
        for ((method, minutes) in reminders) {
            if (method == POPUP) {
                reminderMinutes.add(minutes)
            }
        }
        return reminderMinutes
    }

    override fun onCancelled() {
        if (lastError != null) {
            if (lastError is UserRecoverableAuthIOException) {
                activity.startActivityForResult((lastError as UserRecoverableAuthIOException).intent, SettingsActivity.REQUEST_AUTHORIZATION)
            }
        }
    }
}
