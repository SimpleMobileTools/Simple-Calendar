package com.simplemobiletools.calendar.pro.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.text.TextUtils
import androidx.collection.LongSparseArray
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getLongValue
import com.simplemobiletools.commons.extensions.getStringValue
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

class DBHelper private constructor(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val MAIN_TABLE_NAME = "events"
    private val COL_ID = "id"
    private val COL_START_TS = "start_ts"
    private val COL_END_TS = "end_ts"
    private val COL_TITLE = "title"
    private val COL_LOCATION = "location"
    private val COL_DESCRIPTION = "description"
    private val COL_REMINDER_MINUTES = "reminder_minutes"
    private val COL_REMINDER_MINUTES_2 = "reminder_minutes_2"
    private val COL_REMINDER_MINUTES_3 = "reminder_minutes_3"
    private val COL_IMPORT_ID = "import_id"
    private val COL_FLAGS = "flags"
    private val COL_EVENT_TYPE = "event_type"
    private val COL_LAST_UPDATED = "last_updated"
    private val COL_EVENT_SOURCE = "event_source"

    private val REPETITIONS_TABLE_NAME = "event_repetitions"
    private val COL_EVENT_ID = "event_id"
    private val COL_REPEAT_INTERVAL = "repeat_interval"
    private val COL_REPEAT_RULE = "repeat_rule"
    private val COL_REPEAT_LIMIT = "repeat_limit"

    private val TYPES_TABLE_NAME = "event_types"
    private val COL_TYPE_TITLE = "event_type_title"
    private val COL_TYPE_COLOR = "event_type_color"
    private val COL_TYPE_CALDAV_CALENDAR_ID = "event_caldav_calendar_id"
    private val COL_TYPE_CALDAV_DISPLAY_NAME = "event_caldav_display_name"
    private val COL_TYPE_CALDAV_EMAIL = "event_caldav_email"

    private val REPEAT_EXCEPTIONS_TABLE_NAME = "event_repeat_exceptions"
    private val COL_OCCURRENCE_DAYCODE = "event_occurrence_daycode"
    private val COL_PARENT_EVENT_ID = "event_parent_id"

    private val mDb = writableDatabase

    companion object {
        private const val DB_VERSION = 19
        const val DB_NAME = "events_old.db"
        const val REGULAR_EVENT_TYPE_ID = 1L
        var dbInstance: DBHelper? = null

        fun newInstance(context: Context): DBHelper {
            if (dbInstance == null)
                dbInstance = DBHelper(context)

            return dbInstance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $MAIN_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_START_TS INTEGER, $COL_END_TS INTEGER, " +
                "$COL_TITLE TEXT, $COL_DESCRIPTION TEXT, $COL_REMINDER_MINUTES INTEGER, $COL_REMINDER_MINUTES_2 INTEGER, $COL_REMINDER_MINUTES_3 INTEGER, " +
                "$COL_IMPORT_ID TEXT, $COL_FLAGS INTEGER, $COL_EVENT_TYPE INTEGER NOT NULL DEFAULT $REGULAR_EVENT_TYPE_ID, " +
                "$COL_PARENT_EVENT_ID INTEGER, $COL_LAST_UPDATED INTEGER, $COL_EVENT_SOURCE TEXT, $COL_LOCATION TEXT)")

        createRepetitionsTable(db)
        createTypesTable(db)
        createExceptionsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    private fun createRepetitionsTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $REPETITIONS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_EVENT_ID INTEGER UNIQUE, " +
                "$COL_REPEAT_INTERVAL INTEGER, $COL_REPEAT_LIMIT INTEGER, $COL_REPEAT_RULE INTEGER)")
    }

    private fun createTypesTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TYPES_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_TYPE_TITLE TEXT, $COL_TYPE_COLOR INTEGER, " +
                "$COL_TYPE_CALDAV_CALENDAR_ID INTEGER, $COL_TYPE_CALDAV_DISPLAY_NAME TEXT, $COL_TYPE_CALDAV_EMAIL TEXT)")
    }

    private fun createExceptionsTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $REPEAT_EXCEPTIONS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_PARENT_EVENT_ID INTEGER, " +
                "$COL_OCCURRENCE_DAYCODE INTEGER)")
    }

    fun insert(event: Event, addToCalDAV: Boolean, activity: SimpleActivity? = null, callback: (id: Long) -> Unit) {
        if (event.startTS > event.endTS) {
            callback(0)
            return
        }

        val eventValues = fillEventValues(event)
        val id = mDb.insert(MAIN_TABLE_NAME, null, eventValues)
        event.id = id

        if (event.repeatInterval != 0 && event.parentId == 0L) {
            val metaValues = fillMetaValues(event)
            mDb.insert(REPETITIONS_TABLE_NAME, null, metaValues)
        }

        context.updateWidgets()
        context.scheduleNextEventReminder(event, this, activity)

        if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && context.config.caldavSync) {
            CalDAVHandler(context).insertCalDAVEvent(event)
        }

        callback(event.id!!)
    }

    fun insertEvents(events: ArrayList<Event>, addToCalDAV: Boolean) {
        mDb.beginTransaction()
        try {
            for (event in events) {
                if (event.startTS > event.endTS) {
                    continue
                }

                val eventValues = fillEventValues(event)
                val id = mDb.insert(MAIN_TABLE_NAME, null, eventValues)
                event.id = id

                if (event.repeatInterval != 0 && event.parentId == 0L) {
                    val metaValues = fillMetaValues(event)
                    mDb.insert(REPETITIONS_TABLE_NAME, null, metaValues)
                }

                context.scheduleNextEventReminder(event, this)
                if (addToCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && event.source != SOURCE_IMPORTED_ICS && context.config.caldavSync) {
                    CalDAVHandler(context).insertCalDAVEvent(event)
                }
            }
            mDb.setTransactionSuccessful()
        } finally {
            mDb.endTransaction()
            context.updateWidgets()
        }
    }

    fun update(event: Event, updateAtCalDAV: Boolean, activity: SimpleActivity? = null, callback: (() -> Unit)? = null) {
        val values = fillEventValues(event)
        val selection = "$COL_ID = ?"
        val selectionArgs = arrayOf(event.id.toString())
        mDb.update(MAIN_TABLE_NAME, values, selection, selectionArgs)

        if (event.repeatInterval == 0) {
            val metaSelection = "$COL_EVENT_ID = ?"
            mDb.delete(REPETITIONS_TABLE_NAME, metaSelection, selectionArgs)
        } else {
            val metaValues = fillMetaValues(event)
            mDb.insertWithOnConflict(REPETITIONS_TABLE_NAME, null, metaValues, SQLiteDatabase.CONFLICT_REPLACE)
        }

        context.updateWidgets()
        context.scheduleNextEventReminder(event, this, activity)
        if (updateAtCalDAV && event.source != SOURCE_SIMPLE_CALENDAR && context.config.caldavSync) {
            CalDAVHandler(context).updateCalDAVEvent(event)
        }
        callback?.invoke()
    }

    private fun fillEventValues(event: Event): ContentValues {
        return ContentValues().apply {
            put(COL_START_TS, event.startTS)
            put(COL_END_TS, event.endTS)
            put(COL_TITLE, event.title)
            put(COL_DESCRIPTION, event.description)
            put(COL_REMINDER_MINUTES, event.reminder1Minutes)
            put(COL_REMINDER_MINUTES_2, event.reminder2Minutes)
            put(COL_REMINDER_MINUTES_3, event.reminder3Minutes)
            put(COL_IMPORT_ID, event.importId)
            put(COL_FLAGS, event.flags)
            put(COL_EVENT_TYPE, event.eventType)
            put(COL_PARENT_EVENT_ID, event.parentId)
            put(COL_LAST_UPDATED, event.lastUpdated)
            put(COL_EVENT_SOURCE, event.source)
            put(COL_LOCATION, event.location)
        }
    }

    private fun fillMetaValues(event: Event): ContentValues {
        return ContentValues().apply {
            put(COL_EVENT_ID, event.id)
            put(COL_REPEAT_INTERVAL, event.repeatInterval)
            put(COL_REPEAT_LIMIT, event.repeatLimit)
            put(COL_REPEAT_RULE, event.repeatRule)
        }
    }

    private fun fillExceptionValues(parentEventId: Long, occurrenceTS: Int, addToCalDAV: Boolean, childImportId: String?, callback: (values: ContentValues) -> Unit) {
        val childEvent = getEventWithId(parentEventId)
        if (childEvent == null) {
            callback(ContentValues())
            return
        }

        childEvent.apply {
            id = 0
            parentId = parentEventId
            startTS = 0
            endTS = 0
            if (childImportId != null) {
                importId = childImportId
            }
        }

        insert(childEvent, false) {
            val childEventId = it
            val exceptionValues = ContentValues().apply {
                put(COL_PARENT_EVENT_ID, parentEventId)
                put(COL_OCCURRENCE_DAYCODE, Formatter.getDayCodeFromTS(occurrenceTS))
            }
            callback(exceptionValues)

            Thread {
                if (addToCalDAV && context.config.caldavSync) {
                    val parentEvent = getEventWithId(parentEventId)
                    if (parentEvent != null) {
                        val newId = CalDAVHandler(context).insertEventRepeatException(parentEvent, occurrenceTS)
                        val newImportId = "${parentEvent.source}-$newId"
                        updateEventImportIdAndSource(childEventId, newImportId, parentEvent.source)
                    }
                }
            }.start()
        }
    }

    fun getBirthdays(): List<Event> {
        val selection = "$MAIN_TABLE_NAME.$COL_EVENT_SOURCE = ?"
        val selectionArgs = arrayOf(SOURCE_CONTACT_BIRTHDAY)
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor)
    }

    fun getAnniversaries(): List<Event> {
        val selection = "$MAIN_TABLE_NAME.$COL_EVENT_SOURCE = ?"
        val selectionArgs = arrayOf(SOURCE_CONTACT_ANNIVERSARY)
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor)
    }

    fun deleteAllEvents() {
        val cursor = getEventsCursor()
        val events = fillEvents(cursor).map { it.id.toString() }.toTypedArray()
        deleteEvents(events, true)
    }

    fun deleteEvents(ids: Array<String>, deleteFromCalDAV: Boolean) {
        val args = TextUtils.join(", ", ids)
        val selection = "$MAIN_TABLE_NAME.$COL_ID IN ($args)"
        val cursor = getEventsCursor(selection)
        val events = fillEvents(cursor).filter { it.importId.isNotEmpty() }

        mDb.delete(MAIN_TABLE_NAME, selection, null)

        val metaSelection = "$COL_EVENT_ID IN ($args)"
        mDb.delete(REPETITIONS_TABLE_NAME, metaSelection, null)

        val exceptionSelection = "$COL_PARENT_EVENT_ID IN ($args)"
        mDb.delete(REPEAT_EXCEPTIONS_TABLE_NAME, exceptionSelection, null)

        context.updateWidgets()

        // temporary workaround, will be rewritten in Room
        ids.filterNot { it == "null" }.forEach {
            context.cancelNotification(it.toLong())
        }

        if (deleteFromCalDAV && context.config.caldavSync) {
            events.forEach {
                CalDAVHandler(context).deleteCalDAVEvent(it)
            }
        }

        deleteChildEvents(args, deleteFromCalDAV)
    }

    private fun deleteChildEvents(ids: String, deleteFromCalDAV: Boolean) {
        val projection = arrayOf(COL_ID)
        val selection = "$COL_PARENT_EVENT_ID IN ($ids)"
        val childIds = ArrayList<String>()

        var cursor: Cursor? = null
        try {
            cursor = mDb.query(MAIN_TABLE_NAME, projection, selection, null, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    childIds.add(cursor.getStringValue(COL_ID))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }

        if (childIds.isNotEmpty())
            deleteEvents(childIds.toTypedArray(), deleteFromCalDAV)
    }

    fun getCalDAVCalendarEvents(calendarId: Long): List<Event> {
        val selection = "$MAIN_TABLE_NAME.$COL_EVENT_SOURCE = (?)"
        val selectionArgs = arrayOf("$CALDAV-$calendarId")
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor).filter { it.importId.isNotEmpty() }
    }

    fun addEventRepeatException(parentEventId: Long, occurrenceTS: Int, addToCalDAV: Boolean, childImportId: String? = null) {
        fillExceptionValues(parentEventId, occurrenceTS, addToCalDAV, childImportId) {
            mDb.insert(REPEAT_EXCEPTIONS_TABLE_NAME, null, it)

            val parentEvent = getEventWithId(parentEventId)
            if (parentEvent != null) {
                context.scheduleNextEventReminder(parentEvent, this)
            }
        }
    }

    fun addEventRepeatLimit(eventId: Long, limitTS: Int) {
        val values = ContentValues()
        val time = Formatter.getDateTimeFromTS(limitTS)
        values.put(COL_REPEAT_LIMIT, limitTS - time.hourOfDay)

        val selection = "$COL_EVENT_ID = ?"
        val selectionArgs = arrayOf(eventId.toString())
        mDb.update(REPETITIONS_TABLE_NAME, values, selection, selectionArgs)

        if (context.config.caldavSync) {
            val event = getEventWithId(eventId)
            if (event?.getCalDAVCalendarId() != 0) {
                CalDAVHandler(context).updateCalDAVEvent(event!!)
            }
        }
    }

    fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean, callback: (deletedCnt: Int) -> Unit) {
        var deleteIds = eventTypes.asSequence().filter { it.caldavCalendarId == 0 }.map { it.id }.toList()
        deleteIds = deleteIds.filter { it != REGULAR_EVENT_TYPE_ID } as ArrayList<Long>

        val deletedSet = HashSet<String>()
        deleteIds.map { deletedSet.add(it.toString()) }
        context.config.removeDisplayEventTypes(deletedSet)
        if (deleteIds.isEmpty()) {
            return
        }

        for (eventTypeId in deleteIds) {
            if (deleteEvents) {
                deleteEventsWithType(eventTypeId)
            } else {
                resetEventsWithType(eventTypeId)
            }
        }

        val args = TextUtils.join(", ", deleteIds)
        val selection = "$COL_ID IN ($args)"
        callback(mDb.delete(TYPES_TABLE_NAME, selection, null))
    }

    fun deleteEventTypesWithCalendarId(calendarIds: String) {
        val selection = "$COL_TYPE_CALDAV_CALENDAR_ID IN ($calendarIds)"
        mDb.delete(TYPES_TABLE_NAME, selection, null)
    }

    private fun deleteEventsWithType(eventTypeId: Long) {
        val selection = "$MAIN_TABLE_NAME.$COL_EVENT_TYPE = ?"
        val selectionArgs = arrayOf(eventTypeId.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        val events = fillEvents(cursor)
        val eventIDs = Array(events.size) { i -> (events[i].id.toString()) }
        deleteEvents(eventIDs, true)
    }

    private fun resetEventsWithType(eventTypeId: Long) {
        val values = ContentValues()
        values.put(COL_EVENT_TYPE, REGULAR_EVENT_TYPE_ID)

        val selection = "$COL_EVENT_TYPE = ?"
        val selectionArgs = arrayOf(eventTypeId.toString())
        mDb.update(MAIN_TABLE_NAME, values, selection, selectionArgs)
    }

    fun updateEventImportIdAndSource(eventId: Long, importId: String, source: String) {
        val values = ContentValues()
        values.put(COL_IMPORT_ID, importId)
        values.put(COL_EVENT_SOURCE, source)

        val selection = "$MAIN_TABLE_NAME.$COL_ID = ?"
        val selectionArgs = arrayOf(eventId.toString())
        mDb.update(MAIN_TABLE_NAME, values, selection, selectionArgs)
    }

    fun getEventsWithImportIds() = getEvents("").filter { it.importId.trim().isNotEmpty() } as ArrayList<Event>

    fun getEventWithId(id: Long): Event? {
        val selection = "$MAIN_TABLE_NAME.$COL_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        val events = fillEvents(cursor)
        return if (events.isNotEmpty()) {
            events.first()
        } else {
            null
        }
    }

    fun getEventIdWithImportId(id: String): Long {
        val selection = "$MAIN_TABLE_NAME.$COL_IMPORT_ID = ?"
        val selectionArgs = arrayOf(id)
        val cursor = getEventsCursor(selection, selectionArgs)
        val events = fillEvents(cursor)
        return if (events.isNotEmpty()) {
            events.minBy { it.id!! }?.id ?: 0L
        } else {
            0L
        }
    }

    fun getEventIdWithLastImportId(id: String): Long {
        val selection = "$MAIN_TABLE_NAME.$COL_IMPORT_ID LIKE ?"
        val selectionArgs = arrayOf("%-$id")
        val cursor = getEventsCursor(selection, selectionArgs)
        val events = fillEvents(cursor)
        return if (events.isNotEmpty()) {
            events.minBy { it.id!! }?.id ?: 0L
        } else {
            0L
        }
    }

    fun getEventsWithSearchQuery(text: String, callback: (searchedText: String, events: List<Event>) -> Unit) {
        Thread {
            val searchQuery = "%$text%"
            val selection = "$MAIN_TABLE_NAME.$COL_TITLE LIKE ? OR $MAIN_TABLE_NAME.$COL_LOCATION LIKE ? OR $MAIN_TABLE_NAME.$COL_DESCRIPTION LIKE ?"
            val selectionArgs = arrayOf(searchQuery, searchQuery, searchQuery)
            val cursor = getEventsCursor(selection, selectionArgs)
            val events = fillEvents(cursor)

            val displayEventTypes = context.config.displayEventTypes
            val filteredEvents = events.filter { displayEventTypes.contains(it.eventType.toString()) }
            callback(text, filteredEvents)
        }.start()
    }

    fun getEvents(fromTS: Int, toTS: Int, eventId: Long = -1L, applyTypeFilter: Boolean = true, callback: (events: ArrayList<Event>) -> Unit) {
        Thread {
            getEventsInBackground(fromTS, toTS, eventId, applyTypeFilter, callback)
        }.start()
    }

    fun getEventsInBackground(fromTS: Int, toTS: Int, eventId: Long = -1L, applyTypeFilter: Boolean, callback: (events: ArrayList<Event>) -> Unit) {
        var events = ArrayList<Event>()

        var selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS NULL AND $COL_START_TS != 0"
        if (eventId != -1L) {
            selection += " AND $MAIN_TABLE_NAME.$COL_ID = $eventId"
        }

        if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                callback(ArrayList())
                return
            } else {
                val types = TextUtils.join(",", displayEventTypes)
                selection += " AND $COL_EVENT_TYPE IN ($types)"
            }
        }

        val selectionArgs = arrayOf(toTS.toString(), fromTS.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        events.addAll(fillEvents(cursor))

        events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter))

        events.addAll(getAllDayEvents(fromTS, eventId, applyTypeFilter))

        events = events
                .asSequence()
                .distinct()
                .filterNot { getIgnoredOccurrences(it).contains(Formatter.getDayCodeFromTS(it.startTS).toInt()) }
                .toMutableList() as ArrayList<Event>
        callback(events)
    }

    fun getRepeatableEventsFor(fromTS: Int, toTS: Int, eventId: Long = -1L, applyTypeFilter: Boolean = false): List<Event> {
        val newEvents = ArrayList<Event>()
        var selection = "$COL_REPEAT_INTERVAL != 0 AND $COL_START_TS <= $toTS AND $COL_START_TS != 0"
        if (eventId != -1L)
            selection += " AND $MAIN_TABLE_NAME.$COL_ID = $eventId"

        if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                return ArrayList()
            } else {
                val types = TextUtils.join(",", displayEventTypes)
                selection += " AND $COL_EVENT_TYPE IN ($types)"
            }
        }

        val events = getEvents(selection)
        val startTimes = LongSparseArray<Int>()
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

    private fun getEventsRepeatingTillDateOrForever(fromTS: Int, toTS: Int, startTimes: LongSparseArray<Int>, event: Event): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.startTS <= toTS && (event.repeatLimit == 0 || event.repeatLimit >= event.startTS)) {
            if (event.endTS >= fromTS) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.startTS.isTsOnProperDay(event)) {
                        if (isOnProperWeek(event, startTimes)) {
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
                        if (isOnProperWeek(event, startTimes)) {
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

    private fun getEventsRepeatingXTimes(fromTS: Int, toTS: Int, startTimes: LongSparseArray<Int>, event: Event): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.repeatLimit < 0 && event.startTS <= toTS) {
            if (event.repeatInterval.isXWeeklyRepetition()) {
                if (event.startTS.isTsOnProperDay(event)) {
                    if (isOnProperWeek(event, startTimes)) {
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

    private fun getAllDayEvents(fromTS: Int, eventId: Long = -1L, applyTypeFilter: Boolean = false): List<Event> {
        val events = ArrayList<Event>()
        var selection = "($COL_FLAGS & $FLAG_ALL_DAY) != 0"
        if (eventId != -1L)
            selection += " AND $MAIN_TABLE_NAME.$COL_ID = $eventId"

        if (applyTypeFilter) {
            val displayEventTypes = context.config.displayEventTypes
            if (displayEventTypes.isEmpty()) {
                return ArrayList()
            } else {
                val types = TextUtils.join(",", displayEventTypes)
                selection += " AND $COL_EVENT_TYPE IN ($types)"
            }
        }

        val dayCode = Formatter.getDayCodeFromTS(fromTS)
        val cursor = getEventsCursor(selection)
        events.addAll(fillEvents(cursor).filter { dayCode == Formatter.getDayCodeFromTS(it.startTS) })
        return events
    }

    // check if its the proper week, for events repeating every x weeks
    private fun isOnProperWeek(event: Event, startTimes: LongSparseArray<Int>): Boolean {
        val initialWeekOfYear = Formatter.getDateTimeFromTS(startTimes[event.id!!]!!).weekOfWeekyear
        val currentWeekOfYear = Formatter.getDateTimeFromTS(event.startTS).weekOfWeekyear
        return (currentWeekOfYear - initialWeekOfYear) % (event.repeatInterval / WEEK) == 0
    }

    fun getRunningEvents(): List<Event> {
        val events = ArrayList<Event>()
        val ts = getNowSeconds()
        val selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS NULL AND $COL_START_TS != 0"
        val selectionArgs = arrayOf(ts.toString(), ts.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        events.addAll(fillEvents(cursor))

        events.addAll(getRepeatableEventsFor(ts, ts))
        return events
    }

    private fun getEvents(selection: String): List<Event> {
        val events = ArrayList<Event>()
        var cursor: Cursor? = null
        try {
            cursor = getEventsCursor(selection)
            if (cursor != null) {
                val currEvents = fillEvents(cursor)
                events.addAll(currEvents)
            }
        } finally {
            cursor?.close()
        }

        return events
    }

    fun getEventsWithIds(ids: List<Long>): ArrayList<Event> {
        val args = TextUtils.join(", ", ids)
        val selection = "$MAIN_TABLE_NAME.$COL_ID IN ($args)"
        return getEvents(selection) as ArrayList<Event>
    }

    fun getEventsAtReboot(): List<Event> {
        val selection = "$COL_REMINDER_MINUTES != -1 AND ($COL_START_TS > ? OR $COL_REPEAT_INTERVAL != 0) AND $COL_START_TS != 0"
        val selectionArgs = arrayOf(DateTime.now().seconds().toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor)
    }

    fun getEventsToExport(includePast: Boolean): ArrayList<Event> {
        val currTime = getNowSeconds().toString()
        var events = ArrayList<Event>()

        // non repeating events
        var cursor = if (includePast) {
            getEventsCursor()
        } else {
            val selection = "$COL_END_TS > ?"
            val selectionArgs = arrayOf(currTime)
            getEventsCursor(selection, selectionArgs)
        }
        events.addAll(fillEvents(cursor))

        // repeating events
        if (!includePast) {
            val selection = "$COL_REPEAT_INTERVAL != 0 AND ($COL_REPEAT_LIMIT == 0 OR $COL_REPEAT_LIMIT > ?)"
            val selectionArgs = arrayOf(currTime)
            cursor = getEventsCursor(selection, selectionArgs)
            events.addAll(fillEvents(cursor))
        }

        events = events.distinctBy { it.id } as ArrayList<Event>
        return events
    }

    fun getEventsFromCalDAVCalendar(calendarId: Int): List<Event> {
        val selection = "$MAIN_TABLE_NAME.$COL_EVENT_SOURCE = ?"
        val selectionArgs = arrayOf("$CALDAV-$calendarId")
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor)
    }

    private fun getEventsCursor(selection: String = "", selectionArgs: Array<String>? = null): Cursor? {
        val builder = SQLiteQueryBuilder()
        builder.tables = "$MAIN_TABLE_NAME LEFT OUTER JOIN $REPETITIONS_TABLE_NAME ON $COL_EVENT_ID = $MAIN_TABLE_NAME.$COL_ID"
        val projection = allColumns
        return builder.query(mDb, projection, selection, selectionArgs, "$MAIN_TABLE_NAME.$COL_ID", null, COL_START_TS)
    }

    private val allColumns: Array<String>
        get() = arrayOf("$MAIN_TABLE_NAME.$COL_ID", COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION, COL_REMINDER_MINUTES, COL_REMINDER_MINUTES_2,
                COL_REMINDER_MINUTES_3, COL_REPEAT_INTERVAL, COL_REPEAT_RULE, COL_IMPORT_ID, COL_FLAGS, COL_REPEAT_LIMIT, COL_EVENT_TYPE,
                COL_LAST_UPDATED, COL_EVENT_SOURCE, COL_LOCATION)

    private fun fillEvents(cursor: Cursor?): List<Event> {
        val eventTypeColors = LongSparseArray<Int>()
        context.eventTypesDB.getEventTypes().forEach {
            eventTypeColors.put(it.id!!, it.color)
        }

        val events = ArrayList<Event>()
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    val id = cursor.getLongValue(COL_ID)
                    val startTS = cursor.getIntValue(COL_START_TS)
                    val endTS = cursor.getIntValue(COL_END_TS)
                    val reminder1Minutes = cursor.getIntValue(COL_REMINDER_MINUTES)
                    val reminder2Minutes = cursor.getIntValue(COL_REMINDER_MINUTES_2)
                    val reminder3Minutes = cursor.getIntValue(COL_REMINDER_MINUTES_3)
                    val repeatInterval = cursor.getIntValue(COL_REPEAT_INTERVAL)
                    var repeatRule = cursor.getIntValue(COL_REPEAT_RULE)
                    val repeatLimit = cursor.getIntValue(COL_REPEAT_LIMIT)
                    val title = cursor.getStringValue(COL_TITLE)
                    val location = cursor.getStringValue(COL_LOCATION)
                    val description = cursor.getStringValue(COL_DESCRIPTION)
                    val importId = cursor.getStringValue(COL_IMPORT_ID) ?: ""
                    val flags = cursor.getIntValue(COL_FLAGS)
                    val eventType = cursor.getLongValue(COL_EVENT_TYPE)
                    val lastUpdated = cursor.getLongValue(COL_LAST_UPDATED)
                    val source = cursor.getStringValue(COL_EVENT_SOURCE)

                    if (repeatInterval > 0 && repeatRule == 0 && (repeatInterval % MONTH == 0 || repeatInterval % YEAR == 0)) {
                        repeatRule = REPEAT_SAME_DAY
                    }

                    val event = Event(id, startTS, endTS, title, location, description, reminder1Minutes, reminder2Minutes, reminder3Minutes,
                            repeatInterval, repeatRule, repeatLimit, importId, flags, eventType, 0, lastUpdated, source)
                    event.updateIsPastEvent()
                    event.color = eventTypeColors.get(eventType)!!

                    events.add(event)
                } while (cursor.moveToNext())
            }
        }
        return events
    }

    fun doEventTypesContainEvents(types: ArrayList<EventType>, callback: (contain: Boolean) -> Unit) {
        Thread {
            val args = TextUtils.join(", ", types.map { it.id })
            val columns = arrayOf(COL_ID)
            val selection = "$COL_EVENT_TYPE IN ($args)"
            var cursor: Cursor? = null
            try {
                cursor = mDb.query(MAIN_TABLE_NAME, columns, selection, null, null, null, null)
                callback(cursor?.moveToFirst() == true)
            } catch (e: Exception) {
                callback(false)
            } finally {
                cursor?.close()
            }
        }.start()
    }

    fun getIgnoredOccurrences(event: Event): ArrayList<Int> {
        if (event.repeatInterval == 0) {
            return ArrayList()
        }

        val projection = arrayOf(COL_OCCURRENCE_DAYCODE)
        val selection = "$COL_PARENT_EVENT_ID = ?"
        val selectionArgs = arrayOf(event.id.toString())
        val daycodes = ArrayList<Int>()

        var cursor: Cursor? = null
        try {
            cursor = mDb.query(REPEAT_EXCEPTIONS_TABLE_NAME, projection, selection, selectionArgs, null, null, COL_OCCURRENCE_DAYCODE)
            if (cursor?.moveToFirst() == true) {
                do {
                    daycodes.add(cursor.getIntValue(COL_OCCURRENCE_DAYCODE))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return daycodes
    }
}
