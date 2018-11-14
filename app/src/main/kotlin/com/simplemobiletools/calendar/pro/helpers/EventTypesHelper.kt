package com.simplemobiletools.calendar.pro.helpers

import android.app.Activity
import android.content.Context
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventType
import java.util.*

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

    fun deleteEventTypes(context: Context, eventTypes: ArrayList<EventType>, deleteEvents: Boolean) {
        val typesToDelete = eventTypes.asSequence().filter { it.caldavCalendarId == 0 && it.id != DBHelper.REGULAR_EVENT_TYPE_ID }.toMutableList()
        val deleteIds = typesToDelete.map { it.id }.toMutableList()
        val deletedSet = deleteIds.map { it.toString() }.toHashSet()
        context.config.removeDisplayEventTypes(deletedSet)

        if (deleteIds.isEmpty()) {
            return
        }

        for (eventTypeId in deleteIds) {
            if (deleteEvents) {
                context.dbHelper.deleteEventsWithType(eventTypeId!!)
            } else {
                context.dbHelper.resetEventsWithType(eventTypeId!!)
            }
        }

        context.eventTypesDB.deleteEventTypes(typesToDelete)
    }

    fun getEventRepetitionIgnoredOccurrences(context: Context, event: Event): ArrayList<String> {
        return if (event.id == null || event.repeatInterval == 0) {
            ArrayList()
        } else {
            context.eventRepetitionExceptionsDB.getEventRepetitionExceptions(event.id!!).toMutableList() as ArrayList<String>
        }
    }

    fun insertEvent(context: Context, activity: SimpleActivity? = null, event: Event, addToCalDAV: Boolean, callback: ((id: Long) -> Unit)? = null) {
        if (event.startTS > event.endTS) {
            callback?.invoke(0)
            return
        }

        val id = context.eventsDB.insert(event)
        event.id = id

        if (event.repeatInterval != 0 && event.parentId == 0L) {
            context.eventRepetitionsDB.insertOrUpdate(event.getEventRepetition())
        }

        context.updateWidgets()
        //context.scheduleNextEventReminder(event, this, activity)

        if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && context.config.caldavSync) {
            CalDAVHandler(context).insertCalDAVEvent(event)
        }

        callback?.invoke(event.id!!)
    }

    fun insertEvents(activity: Activity, events: ArrayList<Event>, addToCalDAV: Boolean) {
        try {
            for (event in events) {
                if (event.startTS > event.endTS) {
                    continue
                }

                val id = activity.eventsDB.insert(event)
                event.id = id

                if (event.repeatInterval != 0 && event.parentId == 0L) {
                    activity.eventRepetitionsDB.insertOrUpdate(event.getEventRepetition())
                }

                //context.scheduleNextEventReminder(event, this)
                if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && event.source != SOURCE_IMPORTED_ICS && activity.config.caldavSync) {
                    CalDAVHandler(activity).insertCalDAVEvent(event)
                }
            }
        } finally {
            activity.updateWidgets()
        }
    }
}
