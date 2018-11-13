package com.simplemobiletools.calendar.pro.helpers

import android.app.Activity
import android.content.Context
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventTypesDB
import com.simplemobiletools.calendar.pro.models.EventType

class EventTypesHelper {
    fun getEventTypes(activity: Activity, callback: (notes: ArrayList<EventType>) -> Unit) {
        Thread {
            val eventTypes = activity.eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            activity.runOnUiThread {
                callback(eventTypes)
            }
        }.start()
    }

    fun getEventTypesSync(context: Context) = context.eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>

    fun insertOrUpdateEventType(activity: Activity, eventType: EventType, callback: ((newEventTypeId: Long) -> Unit)? = null) {
        Thread {
            val eventTypeId = insertOrUpdateEventTypeSync(activity, eventType)
            activity.runOnUiThread {
                callback?.invoke(eventTypeId)
            }
        }.start()
    }

    fun insertOrUpdateEventTypeSync(context: Context, eventType: EventType): Long {
        if (eventType.id != null && eventType.id!! > 0 && eventType.caldavCalendarId != 0) {
            CalDAVHandler(context).updateCalDAVCalendar(eventType)
        }

        val newId = context.eventTypesDB.insertOrUpdate(eventType)
        context.config.addDisplayEventType(newId.toString())
        return newId
    }

    fun getEventTypeIdWithTitle(context: Context, title: String) = context.eventTypesDB.getEventTypeIdWithTitle(title) ?: -1L

    fun getEventTypeWithCalDAVCalendarId(context: Context, calendarId: Int) = context.eventTypesDB.getEventTypeWithCalDAVCalendarId(calendarId)
}
