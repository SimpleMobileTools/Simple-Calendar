package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.widget.Toast
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.toast
import java.util.*

class GoogleSyncHandler {
    fun tryInsertToGoogle(activity: SimpleActivity, event: Event) {
        if (activity.isGoogleSyncActive()) {
            if (activity.isOnline()) {
                Thread({
                    val errorMsg = insertToGoogle(activity, event)
                    if (errorMsg.isNotEmpty()) {
                        val msg = String.format(activity.getString(R.string.google_sync_error_insert), errorMsg)
                        activity.toast(msg, Toast.LENGTH_LONG)
                    }
                }).start()
            } else {
                activity.googleSyncQueue.addOperation(event.id, OPERATION_INSERT, event.importId)
            }
        }
    }

    fun insertToGoogle(context: Context, event: Event): String {
        val googleEvent = mergeMyEventToGoogleEvent(com.google.api.services.calendar.model.Event(), event)
        try {
            context.getGoogleSyncService().events().insert(PRIMARY, googleEvent).execute()
        } catch (e: GoogleJsonResponseException) {
            return e.getGoogleMessageError()
        }
        return ""
    }

    fun tryUpdateGoogleEvent(activity: SimpleActivity, event: Event) {
        if (activity.isGoogleSyncActive()) {
            if (activity.isOnline()) {
                Thread({
                    val errorMsg = updateGoogleEvent(activity.applicationContext, event)
                    if (errorMsg.isNotEmpty()) {
                        val msg = String.format(activity.getString(R.string.google_sync_error_update), errorMsg)
                        activity.toast(msg, Toast.LENGTH_LONG)
                    }
                }).start()
            } else {
                activity.googleSyncQueue.addOperation(event.id, OPERATION_UPDATE, event.importId)
            }
        }
    }

    fun updateGoogleEvent(context: Context, event: Event): String {
        try {
            val googleEvent = context.getGoogleSyncService().events().get(PRIMARY, event.importId).execute()
            val newGoogleEvent = mergeMyEventToGoogleEvent(googleEvent, event)
            context.getGoogleSyncService().events().update(PRIMARY, newGoogleEvent.id, newGoogleEvent).execute()
        } catch (e: GoogleJsonResponseException) {
            return e.getGoogleMessageError()
        }
        return ""
    }

    fun tryDeleteFromGoogle(context: Context, event: Event) {
        Thread({
            if (context.isOnline()) {
                deleteFromGoogle(context, event.importId)
            } else {
                context.googleSyncQueue.addOperation(event.id, OPERATION_DELETE, event.importId)
            }
        }).start()
    }

    fun deleteFromGoogle(context: Context, importId: String) {
        try {
            context.getGoogleSyncService().events().delete(PRIMARY, importId).execute()
        } catch (ignored: Exception) {
        }
    }

    private fun mergeMyEventToGoogleEvent(googleEvent: com.google.api.services.calendar.model.Event, event: Event): com.google.api.services.calendar.model.Event {
        googleEvent.apply {
            summary = event.title
            description = event.description

            if (event.getIsAllDay()) {
                start = EventDateTime().setDate(com.google.api.client.util.DateTime(true, event.startTS * 1000L, null))
                end = EventDateTime().setDate(com.google.api.client.util.DateTime(true, (event.endTS + DAY) * 1000L, null))
            } else {
                start = EventDateTime().setDateTime(com.google.api.client.util.DateTime(event.startTS * 1000L)).setTimeZone(TimeZone.getDefault().id)
                end = EventDateTime().setDateTime(com.google.api.client.util.DateTime(event.endTS * 1000L)).setTimeZone(TimeZone.getDefault().id)
            }

            id = event.importId
            status = CONFIRMED.toLowerCase()
            Parser().getShortRepeatInterval(event).let {
                if (it.isNotEmpty()) {
                    recurrence = listOf(it)
                } else {
                    recurrence = null
                }
            }

            if (event.getReminders().isNotEmpty()) {
                reminders = getEventReminders(event).setUseDefault(false)
            } else {
                reminders = com.google.api.services.calendar.model.Event.Reminders().setUseDefault(false)
            }
        }
        return googleEvent
    }

    private fun getEventReminders(event: Event): com.google.api.services.calendar.model.Event.Reminders {
        val reminders = ArrayList<EventReminder>()
        event.getReminders().forEach {
            val reminder = EventReminder().setMinutes(it).setMethod(POPUP)
            reminders.add(reminder)
        }
        return com.google.api.services.calendar.model.Event.Reminders().setOverrides(reminders)
    }
}
