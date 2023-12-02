package com.simplemobiletools.calendar.pro.helpers

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.collection.LongSparseArray
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.CHOPPED_LIST_DEFAULT_SIZE
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class EventsHelper(val context: Context) {
    private val config = context.config
    private val eventsDB = context.eventsDB
    private val eventTypesDB = context.eventTypesDB

    fun getEventTypes(activity: Activity, showWritableOnly: Boolean, callback: (eventTypes: ArrayList<EventType>) -> Unit) {
        ensureBackgroundThread {
            var eventTypes = ArrayList<EventType>()
            try {
                eventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            } catch (ignored: Exception) {
            }

            if (showWritableOnly) {
                val caldavCalendars = activity.calDAVHelper.getCalDAVCalendars("", true)
                eventTypes = eventTypes.filter {
                    val eventType = it
                    it.caldavCalendarId == 0 || caldavCalendars.firstOrNull { it.id == eventType.caldavCalendarId }?.canWrite() == true
                }.toMutableList() as ArrayList<EventType>
            }

            activity.runOnUiThread {
                callback(eventTypes)
            }
        }
    }

    fun getEventTypesSync() = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>

    fun insertOrUpdateEventType(activity: Activity, eventType: EventType, callback: ((newEventTypeId: Long) -> Unit)? = null) {
        ensureBackgroundThread {
            val eventTypeId = insertOrUpdateEventTypeSync(eventType)
            activity.runOnUiThread {
                callback?.invoke(eventTypeId)
            }
        }
    }

    fun insertOrUpdateEventTypeSync(eventType: EventType): Long {
        if (eventType.id != null && eventType.id!! > 0 && eventType.caldavCalendarId != 0) {
            context.calDAVHelper.updateCalDAVCalendar(eventType)
        }

        val newId = eventTypesDB.insertOrUpdate(eventType)
        if (eventType.id == null) {
            config.addDisplayEventType(newId.toString())

            if (config.quickFilterEventTypes.isNotEmpty()) {
                config.addQuickFilterEventType(newId.toString())
            } else {
                val eventTypes = getEventTypesSync()
                if (eventTypes.size == 2) {
                    eventTypes.forEach {
                        config.addQuickFilterEventType(it.id.toString())
                    }
                }
            }
        }
        return newId
    }

    fun getEventTypeIdWithTitle(title: String) = eventTypesDB.getEventTypeIdWithTitle(title) ?: -1L

    fun getEventTypeIdWithClass(classId: Int) = eventTypesDB.getEventTypeIdWithClass(classId) ?: -1L

    private fun getLocalEventTypeIdWithTitle(title: String) = eventTypesDB.getLocalEventTypeIdWithTitle(title) ?: -1L

    private fun getLocalEventTypeIdWithClass(classId: Int) = eventTypesDB.getLocalEventTypeIdWithClass(classId) ?: -1L

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

        if (getEventTypesSync().size == 1) {
            config.quickFilterEventTypes = HashSet()
        }
    }

    fun insertEvent(event: Event, addToCalDAV: Boolean, showToasts: Boolean, enableEventType: Boolean = true, callback: ((id: Long) -> Unit)? = null) {
        if (event.startTS > event.endTS) {
            callback?.invoke(0)
            return
        }

        maybeUpdateParentExceptions(event)
        event.id = eventsDB.insertOrUpdate(event)
        ensureEventTypeVisibility(event, enableEventType)
        context.updateWidgets()
        context.scheduleNextEventReminder(event, showToasts)

        if (addToCalDAV && config.caldavSync && event.source != SOURCE_SIMPLE_CALENDAR && event.source != SOURCE_IMPORTED_ICS) {
            context.calDAVHelper.insertCalDAVEvent(event)
        }

        callback?.invoke(event.id!!)
    }

    fun insertTask(task: Event, showToasts: Boolean, enableEventType: Boolean = true, callback: () -> Unit) {
        maybeUpdateParentExceptions(task)
        task.id = eventsDB.insertOrUpdate(task)
        ensureEventTypeVisibility(task, enableEventType)
        context.updateWidgets()
        context.scheduleNextEventReminder(task, showToasts)
        callback()
    }

    private fun maybeUpdateParentExceptions(event: Event) {
        // if the event is an exception from another event, update the parent event's exceptions list
        val parentEventId = event.parentId
        if (parentEventId != 0L) {
            val parentEvent = eventsDB.getEventOrTaskWithId(parentEventId) ?: return
            val startDayCode = Formatter.getDayCodeFromTS(event.startTS)
            parentEvent.addRepetitionException(startDayCode)
            eventsDB.updateEventRepetitionExceptions(parentEvent.repetitionExceptions.toString(), parentEventId)
        }
    }

    fun insertEvents(events: ArrayList<Event>, addToCalDAV: Boolean) {
        try {
            for (event in events) {
                if (event.startTS > event.endTS) {
                    context.toast(R.string.end_before_start, Toast.LENGTH_LONG)
                    continue
                }

                event.id = eventsDB.insertOrUpdate(event)
                ensureEventTypeVisibility(event, true)
                context.scheduleNextEventReminder(event, false)
                if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && event.source != SOURCE_IMPORTED_ICS && config.caldavSync) {
                    context.calDAVHelper.insertCalDAVEvent(event)
                }
            }
        } finally {
            context.updateWidgets()
        }
    }

    fun updateEvent(event: Event, updateAtCalDAV: Boolean, showToasts: Boolean, enableEventType: Boolean = true, callback: (() -> Unit)? = null) {
        eventsDB.insertOrUpdate(event)
        ensureEventTypeVisibility(event, enableEventType)
        context.updateWidgets()
        context.scheduleNextEventReminder(event, showToasts)
        if (updateAtCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && config.caldavSync) {
            context.calDAVHelper.updateCalDAVEvent(event)
        }
        callback?.invoke()
    }

    fun applyOriginalStartEndTimes(event: Event, oldStartTS: Long, oldEndTS: Long) {
        val originalEvent = eventsDB.getEventOrTaskWithId(event.id!!) ?: return
        val originalStartTS = originalEvent.startTS
        val originalEndTS = originalEvent.endTS

        event.apply {
            val startTSDelta = oldStartTS - startTS
            val endTSDelta = oldEndTS - endTS
            startTS = originalStartTS - startTSDelta
            endTS = if (isTask()) startTS else originalEndTS - endTSDelta
        }
    }

    fun editSelectedOccurrence(event: Event, showToasts: Boolean, callback: () -> Unit) {
        ensureBackgroundThread {
            event.apply {
                parentId = id!!.toLong()
                id = null
                repeatRule = 0
                repeatInterval = 0
                repeatLimit = 0
            }
            if (event.isTask()) {
                insertTask(event, showToasts = showToasts, callback = callback)
            } else {
                insertEvent(event, addToCalDAV = true, showToasts = showToasts) {
                    callback()
                }
            }
        }
    }

    fun editFutureOccurrences(event: Event, eventOccurrenceTS: Long, showToasts: Boolean, callback: () -> Unit) {
        ensureBackgroundThread {
            val eventId = event.id!!
            val originalEvent = eventsDB.getEventOrTaskWithId(event.id!!) ?: return@ensureBackgroundThread
            event.maybeAdjustRepeatLimitCount(originalEvent, eventOccurrenceTS)
            event.id = null
            addEventRepeatLimit(eventId, eventOccurrenceTS)
            if (eventOccurrenceTS == originalEvent.startTS) {
                deleteEvent(eventId, true)
            }

            if (event.isTask()) {
                insertTask(event, showToasts = showToasts, callback = callback)
            } else {
                insertEvent(event, addToCalDAV = true, showToasts = showToasts) {
                    callback()
                }
            }
        }
    }

    fun editAllOccurrences(event: Event, originalStartTS: Long, originalEndTS: Long = 0, showToasts: Boolean, callback: () -> Unit) {
        ensureBackgroundThread {
            applyOriginalStartEndTimes(event, originalStartTS, originalEndTS)
            updateEvent(event, updateAtCalDAV = !event.isTask(), showToasts = showToasts, callback = callback)
        }
    }

    private fun ensureEventTypeVisibility(event: Event, enableEventType: Boolean) {
        if (enableEventType) {
            val eventType = event.eventType.toString()
            val displayEventTypes = config.displayEventTypes
            if (!displayEventTypes.contains(eventType)) {
                config.displayEventTypes = displayEventTypes.plus(eventType)
            }
        }
    }

    fun deleteAllEvents() {
        ensureBackgroundThread {
            val eventIds = eventsDB.getEventIds().toMutableList()
            deleteEvents(eventIds, true)
        }
    }

    fun deleteEvent(id: Long, deleteFromCalDAV: Boolean) = deleteEvents(arrayListOf(id), deleteFromCalDAV)

    fun deleteEvents(ids: MutableList<Long>, deleteFromCalDAV: Boolean) {
        if (ids.isEmpty()) {
            return
        }

        ids.chunked(CHOPPED_LIST_DEFAULT_SIZE).forEach {
            val eventsWithImportId = eventsDB.getEventsByIdsWithImportIds(it)
            eventsDB.deleteEvents(it)

            it.forEach {
                context.cancelNotification(it)
                context.cancelPendingIntent(it)
            }

            if (deleteFromCalDAV && config.caldavSync) {
                eventsWithImportId.forEach {
                    context.calDAVHelper.deleteCalDAVEvent(it)
                }
            }

            deleteChildEvents(it as MutableList<Long>, deleteFromCalDAV)
            context.updateWidgets()
        }
    }

    private fun deleteChildEvents(ids: List<Long>, deleteFromCalDAV: Boolean) {
        val childIds = eventsDB.getEventIdsWithParentIds(ids).toMutableList()
        if (childIds.isNotEmpty()) {
            deleteEvents(childIds, deleteFromCalDAV)
        }
    }

    private fun deleteEventsWithType(eventTypeId: Long) {
        val eventIds = eventsDB.getEventIdsByEventType(eventTypeId).toMutableList()
        deleteEvents(eventIds, true)
    }

    fun addEventRepeatLimit(eventId: Long, occurrenceTS: Long) {
        val event = eventsDB.getEventOrTaskWithId(eventId) ?: return
        val previousOccurrenceTS = occurrenceTS - event.repeatInterval // always update repeat limit of the occurrence preceding the one being edited
        val repeatLimitDateTime = Formatter.getDateTimeFromTS(previousOccurrenceTS).withTimeAtStartOfDay()
        val repeatLimitTS = if (event.getIsAllDay()) {
            repeatLimitDateTime.seconds()
        } else {
            repeatLimitDateTime.withTime(23, 59, 59, 0).seconds()
        }

        eventsDB.updateEventRepetitionLimit(repeatLimitTS, eventId)
        context.cancelNotification(eventId)
        context.cancelPendingIntent(eventId)
        if (config.caldavSync) {
            val event = eventsDB.getEventWithId(eventId)
            if (event != null && event.getCalDAVCalendarId() != 0) {
                context.calDAVHelper.updateCalDAVEvent(event)
            }
        }
    }

    fun doEventTypesContainEvents(eventTypeIds: ArrayList<Long>, callback: (contain: Boolean) -> Unit) {
        ensureBackgroundThread {
            val eventIds = eventsDB.getEventIdsByEventType(eventTypeIds)
            callback(eventIds.isNotEmpty())
        }
    }

    fun deleteRepeatingEventOccurrence(parentEventId: Long, occurrenceTS: Long, addToCalDAV: Boolean) {
        ensureBackgroundThread {
            val parentEvent = eventsDB.getEventOrTaskWithId(parentEventId) ?: return@ensureBackgroundThread
            val occurrenceDayCode = Formatter.getDayCodeFromTS(occurrenceTS)
            parentEvent.addRepetitionException(occurrenceDayCode)
            eventsDB.updateEventRepetitionExceptions(parentEvent.repetitionExceptions.toString(), parentEventId)
            context.scheduleNextEventReminder(parentEvent, false)

            if (addToCalDAV && config.caldavSync) {
                context.calDAVHelper.insertEventRepeatException(parentEvent, occurrenceTS)
            }
        }
    }

    fun getEvents(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean = true,
        searchQuery: String = "",
        callback: (events: ArrayList<Event>) -> Unit
    ) {
        ensureBackgroundThread {
            getEventsSync(fromTS, toTS, eventId, applyTypeFilter, searchQuery, callback)
        }
    }

    fun getEventsSync(
        fromTS: Long,
        toTS: Long,
        eventId: Long = -1L,
        applyTypeFilter: Boolean,
        searchQuery: String = "",
        callback: (events: ArrayList<Event>) -> Unit
    ) {
        val birthDayEventId = getLocalBirthdaysEventTypeId(createIfNotExists = false)
        val anniversaryEventId = getAnniversariesEventTypeId(createIfNotExists = false)

        var events = ArrayList<Event>()
        if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                callback(ArrayList())
                return
            } else {
                try {
                    val typesList = context.config.getDisplayEventTypessAsList()

                    if (searchQuery.isEmpty()) {
                        events.addAll(eventsDB.getOneTimeEventsFromToWithTypes(toTS, fromTS, typesList).toMutableList() as ArrayList<Event>)
                    } else {
                        events.addAll(
                            eventsDB.getOneTimeEventsFromToWithTypesForSearch(toTS, fromTS, typesList, "%$searchQuery%").toMutableList() as ArrayList<Event>
                        )
                    }
                } catch (e: Exception) {
                }
            }
        } else {
            events.addAll(eventsDB.getTasksFromTo(fromTS, toTS, ArrayList()))

            events.addAll(
                if (eventId == -1L) {
                    eventsDB.getOneTimeEventsOrTasksFromTo(toTS, fromTS).toMutableList() as ArrayList<Event>
                } else {
                    eventsDB.getOneTimeEventFromToWithId(eventId, toTS, fromTS).toMutableList() as ArrayList<Event>
                }
            )
        }

        events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter, searchQuery))

        events = events
            .asSequence()
            .distinct()
            .filterNot { it.repetitionExceptions.contains(Formatter.getDayCodeFromTS(it.startTS)) }
            .toMutableList() as ArrayList<Event>

        val eventTypeColors = LongSparseArray<Int>()
        context.eventTypesDB.getEventTypes().forEach {
            eventTypeColors.put(it.id!!, it.color)
        }

        events.forEach {
            if (it.isTask()) {
                updateIsTaskCompleted(it)
            }

            it.updateIsPastEvent()
            val originalEvent = eventsDB.getEventWithId(it.id!!)
            if (originalEvent != null &&
                (birthDayEventId != -1L && it.eventType == birthDayEventId) or
                (anniversaryEventId != -1L && it.eventType == anniversaryEventId)
            ) {
                val eventStartDate = Formatter.getDateFromTS(it.startTS)
                val originalEventStartDate = Formatter.getDateFromTS(originalEvent.startTS)
                if (it.hasMissingYear().not()) {
                    val years = (eventStartDate.year - originalEventStartDate.year).coerceAtLeast(0)
                    if (years > 0) {
                        it.title = "${it.title} ($years)"
                    }
                }
            }

            if (it.color == 0) {
                it.color = eventTypeColors.get(it.eventType) ?: context.getProperPrimaryColor()
            }
        }

        callback(events)
    }

    fun createPredefinedEventType(title: String, @ColorRes colorResId: Int, type: Int, caldav: Boolean = false): Long {
        val eventType = EventType(id = null, title = title, color = context.resources.getColor(colorResId), type = type)

        // check if the event type already exists but without the type (e.g. BIRTHDAY_EVENT) so as to avoid duplication
        val originalEventTypeId = if (caldav) {
            getEventTypeIdWithTitle(title)
        } else {
            getLocalEventTypeIdWithTitle(title)
        }
        if (originalEventTypeId != -1L) {
            eventType.id = originalEventTypeId
        }

        return insertOrUpdateEventTypeSync(eventType)
    }

    fun getLocalBirthdaysEventTypeId(createIfNotExists: Boolean = true): Long {
        var eventTypeId = getLocalEventTypeIdWithClass(BIRTHDAY_EVENT)
        if (eventTypeId == -1L && createIfNotExists) {
            val birthdays = context.getString(R.string.birthdays)
            eventTypeId = createPredefinedEventType(birthdays, R.color.default_birthdays_color, BIRTHDAY_EVENT)
        }
        return eventTypeId
    }

    fun getAnniversariesEventTypeId(createIfNotExists: Boolean = true): Long {
        var eventTypeId = getLocalEventTypeIdWithClass(ANNIVERSARY_EVENT)
        if (eventTypeId == -1L && createIfNotExists) {
            val anniversaries = context.getString(R.string.anniversaries)
            eventTypeId = createPredefinedEventType(anniversaries, R.color.default_anniversaries_color, ANNIVERSARY_EVENT)
        }
        return eventTypeId
    }

    fun getRepeatableEventsFor(fromTS: Long, toTS: Long, eventId: Long = -1L, applyTypeFilter: Boolean = false, searchQuery: String = ""): List<Event> {
        val events = if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                return ArrayList()
            } else if (searchQuery.isEmpty()) {
                eventsDB.getRepeatableEventsOrTasksWithTypes(toTS, context.config.getDisplayEventTypessAsList()).toMutableList() as ArrayList<Event>
            } else {
                eventsDB.getRepeatableEventsOrTasksWithTypesForSearch(toTS, context.config.getDisplayEventTypessAsList(), "%$searchQuery%")
                    .toMutableList() as ArrayList<Event>
            }
        } else {
            if (eventId == -1L) {
                eventsDB.getRepeatableEventsOrTasksWithTypes(toTS).toMutableList() as ArrayList<Event>
            } else {
                eventsDB.getRepeatableEventsOrTasksWithId(eventId, toTS).toMutableList() as ArrayList<Event>
            }
        }

        val startTimes = LongSparseArray<Long>()
        val newEvents = ArrayList<Event>()
        events.forEach {
            startTimes.put(it.id!!, it.startTS)
            if (it.repeatLimit >= 0) {
                newEvents.addAll(getEventsRepeatingTillDateOrForever(fromTS, toTS, startTimes, it))
            } else {
                newEvents.addAll(getEventsRepeatingXTimes(fromTS, toTS, startTimes, it))
            }
        }

        return newEvents
    }

    private fun getEventsRepeatingXTimes(fromTS: Long, toTS: Long, startTimes: LongSparseArray<Long>, event: Event): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.repeatLimit < 0 && event.startTS <= toTS) {
            if (event.repeatInterval.isXWeeklyRepetition()) {
                if (event.startTS.isTsOnProperDay(event)) {
                    if (event.isOnProperWeek(startTimes)) {
                        if (event.endTS >= fromTS) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                        event.repeatLimit++
                    }
                }
            } else {
                if (event.endTS >= fromTS) {
                    event.copy().apply {
                        updateIsPastEvent()
                        color = event.color
                        events.add(this)
                    }
                } else if (event.getIsAllDay()) {
                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                    if (dayCode == endDayCode) {
                        event.copy().apply {
                            updateIsPastEvent()
                            color = event.color
                            events.add(this)
                        }
                    }
                }
                event.repeatLimit++
            }
            event.addIntervalTime(original)
        }
        return events
    }

    private fun getEventsRepeatingTillDateOrForever(fromTS: Long, toTS: Long, startTimes: LongSparseArray<Long>, event: Event): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.startTS <= toTS && (event.repeatLimit == 0L || event.repeatLimit >= event.startTS)) {
            if (event.endTS >= fromTS) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.startTS.isTsOnProperDay(event)) {
                        if (event.isOnProperWeek(startTimes)) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                    }
                } else {
                    event.copy().apply {
                        updateIsPastEvent()
                        color = event.color
                        events.add(this)
                    }
                }
            }

            if (event.getIsAllDay()) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.endTS >= toTS && event.startTS.isTsOnProperDay(event)) {
                        if (event.isOnProperWeek(startTimes)) {
                            event.copy().apply {
                                updateIsPastEvent()
                                color = event.color
                                events.add(this)
                            }
                        }
                    }
                } else {
                    val dayCode = Formatter.getDayCodeFromTS(fromTS)
                    val endDayCode = Formatter.getDayCodeFromTS(event.endTS)
                    if (dayCode == endDayCode) {
                        event.copy().apply {
                            updateIsPastEvent()
                            color = event.color
                            events.add(this)
                        }
                    }
                }
            }
            event.addIntervalTime(original)
        }
        return events
    }

    fun updateIsTaskCompleted(event: Event) {
        val task = context.completedTasksDB.getTaskWithIdAndTs(event.id!!, startTs = event.startTS)
        event.flags = task?.flags ?: event.flags
    }

    fun getRunningEventsOrTasks(): List<Event> {
        val ts = getNowSeconds()
        val events = eventsDB.getOneTimeEventsOrTasksFromTo(ts, ts).toMutableList() as ArrayList<Event>
        events.addAll(getRepeatableEventsFor(ts, ts))
        events.forEach {
            if (it.isTask()) updateIsTaskCompleted(it)
        }
        return events
    }

    fun getEventsToExport(eventTypes: List<Long>, exportEvents: Boolean, exportTasks: Boolean, exportPastEntries: Boolean): MutableList<Event> {
        val currTS = getNowSeconds()
        var events = mutableListOf<Event>()
        val tasks = mutableListOf<Event>()
        if (exportPastEntries) {
            if (exportEvents) {
                events.addAll(eventsDB.getAllEventsWithTypes(eventTypes))
            }
            if (exportTasks) {
                tasks.addAll(eventsDB.getAllTasksWithTypes(eventTypes))
            }
        } else {
            if (exportEvents) {
                events.addAll(eventsDB.getAllFutureEventsWithTypes(currTS, eventTypes))
            }
            if (exportTasks) {
                tasks.addAll(eventsDB.getAllFutureTasksWithTypes(currTS, eventTypes))
            }
        }

        tasks.forEach {
            updateIsTaskCompleted(it)
        }
        events.addAll(tasks)

        events = events.distinctBy { it.id } as ArrayList<Event>
        return events
    }
}
