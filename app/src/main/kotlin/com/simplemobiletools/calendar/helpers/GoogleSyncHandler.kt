package com.simplemobiletools.calendar.helpers

import android.content.Context
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import com.simplemobiletools.calendar.extensions.getGoogleSyncService
import com.simplemobiletools.calendar.extensions.isGoogleSyncActive
import com.simplemobiletools.calendar.extensions.isOnline
import com.simplemobiletools.calendar.models.Event
import java.util.*

class GoogleSyncHandler {
    fun uploadToGoogle(context: Context, event: Event) {
        if (context.isGoogleSyncActive()) {
            if (context.isOnline()) {
                Thread({
                    createRemoteGoogleEvent(context, event)
                }).start()
            }
        }
    }

    private fun createRemoteGoogleEvent(context: Context, event: Event) {
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

                context.getGoogleSyncService().events().insert(PRIMARY, this).execute()
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
}
