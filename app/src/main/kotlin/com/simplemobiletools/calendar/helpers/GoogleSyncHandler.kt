package com.simplemobiletools.calendar.helpers

import android.widget.Toast
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.getGoogleMessageError
import com.simplemobiletools.calendar.extensions.getGoogleSyncService
import com.simplemobiletools.calendar.extensions.isGoogleSyncActive
import com.simplemobiletools.calendar.extensions.isOnline
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.toast
import java.util.*

class GoogleSyncHandler {
    fun uploadToGoogle(activity: SimpleActivity, event: Event) {
        if (activity.isGoogleSyncActive()) {
            if (activity.isOnline()) {
                Thread({
                    createRemoteGoogleEvent(activity, event)
                }).start()
            }
        }
    }

    private fun createRemoteGoogleEvent(activity: SimpleActivity, event: Event) {
        try {
            com.google.api.services.calendar.model.Event().apply {
                summary = event.title
                description = event.description

                if (event.getIsAllDay()) {
                    start = EventDateTime().setDate(com.google.api.client.util.DateTime(true, event.startTS * 1000L, null))
                    end = EventDateTime().setDate(com.google.api.client.util.DateTime(true, (event.endTS + DAY) * 1000L, null))
                } else {
                    start = EventDateTime().setDateTime(com.google.api.client.util.DateTime(event.startTS * 1000L)).setTimeZone(TimeZone.getDefault().id)
                    end = EventDateTime().setDateTime(com.google.api.client.util.DateTime(event.endTS * 1000L)).setTimeZone(TimeZone.getDefault().id)
                }

                status = CONFIRMED.toLowerCase()
                Parser().getShortRepeatInterval(event).let {
                    if (it.isNotEmpty()) {
                        recurrence = listOf(it)
                    }
                }

                if (event.getReminders().isNotEmpty()) {
                    reminders = getEventReminders(event).setUseDefault(false)
                }

                try {
                    activity.getGoogleSyncService().events().insert(PRIMARY, this).execute()
                } catch (e: GoogleJsonResponseException) {
                    val msg = String.format(activity.getString(R.string.google_sync_error_insert), e.getGoogleMessageError())
                    activity.toast(msg, Toast.LENGTH_LONG)
                }
            }
        } catch (ignored: Exception) {

        }
    }

    private fun getEventReminders(event: Event): com.google.api.services.calendar.model.Event.Reminders {
        val reminders = ArrayList<EventReminder>()
        event.getReminders().forEach {
            val reminder = EventReminder().setMinutes(it).setMethod(POPUP)
            reminders.add(reminder)
        }
        return com.google.api.services.calendar.model.Event.Reminders().setOverrides(reminders)
    }

    fun updateGoogleEvent(activity: SimpleActivity, event: Event) {
        if (activity.isGoogleSyncActive()) {
            if (activity.isOnline()) {
                Thread({
                    try {
                        val googleEvent = activity.getGoogleSyncService().events().get(PRIMARY, event.importId + "qw").execute()
                    } catch (e: GoogleJsonResponseException) {
                        val msg = String.format(activity.getString(R.string.google_sync_error_update), e.getGoogleMessageError())
                        activity.toast(msg, Toast.LENGTH_LONG)
                    }
                }).start()
            }
        }
    }
}
