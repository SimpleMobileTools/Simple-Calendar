package com.simplemobiletools.calendar.pro.helpers

import android.app.Activity
import android.content.Context
import androidx.collection.LongSparseArray
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventRepetitionException
import com.simplemobiletools.calendar.pro.models.EventType
import java.util.*

class EventsHelper(val context: Context) {
    private val config = context.config
    private val eventsDB = context.eventsDB
    private val eventTypesDB = context.eventTypesDB

    fun getEventTypes(activity: Activity, callback: (notes: ArrayList<EventType>) -> Unit) {
        Thread {
            val eventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            activity.runOnUiThread {
                callback(eventTypes)
            }
        }.start()
    }

    fun getEventTypesSync() = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>

    fun insertOrUpdateEventType(activity: Activity, eventType: EventType, callback: ((newEventTypeId: Long) -> Unit)? = null) {
        Thread {
            val eventTypeId = insertOrUpdateEventTypeSync(eventType)
            activity.runOnUiThread {
                callback?.invoke(eventTypeId)
            }
        }.start()
    }

    fun insertOrUpdateEventTypeSync(eventType: EventType): Long {
        if (eventType.id != null && eventType.id!! > 0 && eventType.caldavCalendarId != 0) {
            context.calDAVHelper.updateCalDAVCalendar(eventType)
        }

        val newId = eventTypesDB.insertOrUpdate(eventType)
        config.addDisplayEventType(newId.toString())
        return newId
    }

    fun getEventTypeIdWithTitle(title: String) = eventTypesDB.getEventTypeIdWithTitle(title) ?: -1L

    fun getEventTypeWithCalDAVCalendarId(calendarId: Int) = eventTypesDB.getEventTypeWithCalDAVCalendarId(calendarId)

    fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean) {
        val typesToDelete = eventTypes.asSequence().filter { it.caldavCalendarId == 0 && it.id != REGULAR_EVENT_TYPE_ID }.toMutableList()
        val deleteIds = typesToDelete.map { it.id }.toMutableList()
        val deletedSet = deleteIds.map { it.toString() }.toHashSet()
        config.removeDisplayEventTypes(deletedSet)

        if (deleteIds.isEmpty()) {
            return
        }

        for (eventTypeId in deleteIds) {
            if (deleteEvents) {
                deleteEventsWithType(eventTypeId!!)
            } else {
                eventsDB.resetEventsWithType(eventTypeId!!)
            }
        }

        eventTypesDB.deleteEventTypes(typesToDelete)
    }

    fun getEventRepetitionIgnoredOccurrences(event: Event): ArrayList<String> {
        return if (event.id == null || event.repeatInterval == 0) {
            ArrayList()
        } else {
            context.eventRepetitionExceptionsDB.getEventRepetitionExceptions(event.id!!).toMutableList() as ArrayList<String>
        }
    }

    fun insertEvent(activity: SimpleActivity? = null, event: Event, addToCalDAV: Boolean, callback: ((id: Long) -> Unit)? = null) {
        if (event.startTS > event.endTS) {
            callback?.invoke(0)
            return
        }

        event.id = eventsDB.insertOrUpdate(event)

        context.updateWidgets()
        context.scheduleNextEventReminder(event, activity)

        if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && config.caldavSync) {
            context.calDAVHelper.insertCalDAVEvent(event)
        }

        callback?.invoke(event.id!!)
    }

    fun insertEvents(events: ArrayList<Event>, addToCalDAV: Boolean) {
        try {
            for (event in events) {
                if (event.startTS > event.endTS) {
                    continue
                }

                event.id = eventsDB.insertOrUpdate(event)

                context.scheduleNextEventReminder(event)
                if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && event.source != SOURCE_IMPORTED_ICS && config.caldavSync) {
                    context.calDAVHelper.insertCalDAVEvent(event)
                }
            }
        } finally {
            context.updateWidgets()
        }
    }

    fun updateEvent(activity: SimpleActivity? = null, event: Event, updateAtCalDAV: Boolean, callback: (() -> Unit)? = null) {
        eventsDB.insertOrUpdate(event)

        context.updateWidgets()
        context.scheduleNextEventReminder(event, activity)
        if (updateAtCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && config.caldavSync) {
            context.calDAVHelper.updateCalDAVEvent(event)
        }
        callback?.invoke()
    }

    fun deleteAllEvents() {
        Thread {
            val eventIds = eventsDB.getEventIds().toMutableList()
            deleteEvents(eventIds, true)
        }.start()
    }

    fun deleteEvent(id: Long, deleteFromCalDAV: Boolean) = deleteEvents(arrayListOf(id), deleteFromCalDAV)

    fun deleteEvents(ids: MutableList<Long>, deleteFromCalDAV: Boolean) {
        val eventsWithImportId = eventsDB.getEventsByIdsWithImportIds(ids)
        eventsDB.deleteEvents(ids)

        ids.forEach {
            context.cancelNotification(it)
        }

        if (deleteFromCalDAV && config.caldavSync) {
            eventsWithImportId.forEach {
                context.calDAVHelper.deleteCalDAVEvent(it)
            }
        }

        deleteChildEvents(ids, deleteFromCalDAV)
        context.updateWidgets()
    }

    private fun deleteChildEvents(ids: MutableList<Long>, deleteFromCalDAV: Boolean) {
        val childIds = eventsDB.getEventIdsWithParentIds(ids).toMutableList()
        if (childIds.isNotEmpty()) {
            deleteEvents(childIds, deleteFromCalDAV)
        }
    }

    private fun deleteEventsWithType(eventTypeId: Long) {
        val eventIds = eventsDB.getEventIdsByEventType(eventTypeId).toMutableList()
        deleteEvents(eventIds, true)
    }

    fun addEventRepeatLimit(eventId: Long, limitTS: Long) {
        val time = Formatter.getDateTimeFromTS(limitTS)
        eventsDB.updateEventRepetitionLimit(limitTS - time.hourOfDay, eventId)

        if (config.caldavSync) {
            val event = eventsDB.getEventWithId(eventId)
            if (event?.getCalDAVCalendarId() != 0) {
                context.calDAVHelper.updateCalDAVEvent(event!!)
            }
        }
    }

    fun doEventTypesContainEvents(eventTypeIds: ArrayList<Long>, callback: (contain: Boolean) -> Unit) {
        Thread {
            val eventIds = eventsDB.getEventIdsByEventType(eventTypeIds)
            callback(eventIds.isNotEmpty())
        }.start()
    }

    fun getEventsWithSearchQuery(text: String, activity: Activity, callback: (searchedText: String, events: List<Event>) -> Unit) {
        Thread {
            val searchQuery = "%$text%"
            val events = eventsDB.getEventsForSearch(searchQuery)
            val displayEventTypes = config.displayEventTypes
            val filteredEvents = events.filter { displayEventTypes.contains(it.eventType.toString()) }
            activity.runOnUiThread {
                callback(text, filteredEvents)
            }
        }.start()
    }

    fun addEventRepeatException(parentEventId: Long, occurrenceTS: Long, addToCalDAV: Boolean, childImportId: String? = null) {
        fillExceptionValues(parentEventId, occurrenceTS, addToCalDAV, childImportId) {
            context.eventRepetitionExceptionsDB.insert(it)

            val parentEvent = eventsDB.getEventWithId(parentEventId)
            if (parentEvent != null) {
                context.scheduleNextEventReminder(parentEvent)
            }
        }
    }

    private fun fillExceptionValues(parentEventId: Long, occurrenceTS: Long, addToCalDAV: Boolean, childImportId: String?, callback: (eventRepetitionException: EventRepetitionException) -> Unit) {
        val childEvent = eventsDB.getEventWithId(parentEventId) ?: return

        childEvent.apply {
            id = 0
            parentId = parentEventId
            startTS = 0
            endTS = 0
            if (childImportId != null) {
                importId = childImportId
            }
        }

        insertEvent(null, childEvent, false) {
            val childEventId = it
            val eventRepetitionException = EventRepetitionException(null, Formatter.getDayCodeFromTS(occurrenceTS), parentEventId)
            callback(eventRepetitionException)

            Thread {
                if (addToCalDAV && config.caldavSync) {
                    val parentEvent = eventsDB.getEventWithId(parentEventId)
                    if (parentEvent != null) {
                        val newId = context.calDAVHelper.insertEventRepeatException(parentEvent, occurrenceTS)
                        val newImportId = "${parentEvent.source}-$newId"
                        eventsDB.updateEventImportIdAndSource(newImportId, parentEvent.source, childEventId)
                    }
                }
            }.start()
        }
    }

    fun getEvents(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean = true, callback: (events: ArrayList<Event>) -> Unit) {
        Thread {
            getEventsSync(fromTS, toTS, eventId, applyTypeFilter, callback)
        }.start()
    }

    fun getEventsSync(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean, callback: (events: ArrayList<Event>) -> Unit) {
        var events: ArrayList<Event>

        //var selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS NULL AND $COL_START_TS != 0"

        events = if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                callback(ArrayList())
                return
            } else {
                eventsDB.getOneTimeEventsFromToWithTypes(toTS, fromTS, context.config.getDisplayEventTypessAsList()).toMutableList() as ArrayList<Event>
            }
        } else {
            if (eventId == -1L) {
                eventsDB.getOneTimeEventsFromTo(toTS, fromTS).toMutableList() as ArrayList<Event>
            } else {
                eventsDB.getOneTimeEventFromToWithId(eventId, toTS, fromTS).toMutableList() as ArrayList<Event>
            }
        }

        // events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter))

        events = events
                .asSequence()
                .distinct()
                .filterNot { context.eventsHelper.getEventRepetitionIgnoredOccurrences(it).contains(Formatter.getDayCodeFromTS(it.startTS)) }
                .toMutableList() as ArrayList<Event>

        val eventTypeColors = LongSparseArray<Int>()
        context.eventTypesDB.getEventTypes().forEach {
            eventTypeColors.put(it.id!!, it.color)
        }

        events.forEach {
            it.updateIsPastEvent()
            it.color = eventTypeColors.get(it.eventType)!!
        }

        callback(events)
    }
}
