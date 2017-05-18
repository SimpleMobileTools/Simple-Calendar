package com.simplemobiletools.calendar.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.text.TextUtils
import android.util.SparseIntArray
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import org.joda.time.DateTime
import java.util.*

class DBHelper private constructor(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val MAIN_TABLE_NAME = "events"
    private val COL_ID = "id"
    private val COL_START_TS = "start_ts"
    private val COL_END_TS = "end_ts"
    private val COL_TITLE = "title"
    private val COL_DESCRIPTION = "description"
    private val COL_REMINDER_MINUTES = "reminder_minutes"
    private val COL_REMINDER_MINUTES_2 = "reminder_minutes_2"
    private val COL_REMINDER_MINUTES_3 = "reminder_minutes_3"
    private val COL_IMPORT_ID = "import_id"
    private val COL_FLAGS = "flags"
    private val COL_EVENT_TYPE = "event_type"

    private val META_TABLE_NAME = "events_meta"
    private val COL_EVENT_ID = "event_id"
    private val COL_REPEAT_START = "repeat_start"
    private val COL_REPEAT_INTERVAL = "repeat_interval"
    private val COL_REPEAT_RULE = "repeat_rule"
    private val COL_REPEAT_LIMIT = "repeat_limit"

    private val TYPES_TABLE_NAME = "event_types"
    private val COL_TYPE_ID = "event_type_id"
    private val COL_TYPE_TITLE = "event_type_title"
    private val COL_TYPE_COLOR = "event_type_color"

    private val EXCEPTIONS_TABLE_NAME = "event_repeat_exceptions"
    private val COL_EXCEPTION_ID = "event_exception_id"
    private val COL_OCCURRENCE_TIMESTAMP = "event_occurrence_timestamp"
    private val COL_OCCURRENCE_DAYCODE = "event_occurrence_daycode"
    private val COL_PARENT_EVENT_ID = "event_parent_id"

    private val mDb: SQLiteDatabase = writableDatabase

    companion object {
        private val DB_VERSION = 11
        val DB_NAME = "events.db"
        val REGULAR_EVENT_TYPE_ID = 1

        private var mEventsListener: EventUpdateListener? = null

        fun newInstance(context: Context, callback: EventUpdateListener? = null): DBHelper {
            mEventsListener = callback
            return DBHelper(context)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $MAIN_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY, $COL_START_TS INTEGER, $COL_END_TS INTEGER, $COL_TITLE TEXT, " +
                "$COL_DESCRIPTION TEXT, $COL_REMINDER_MINUTES INTEGER, $COL_REMINDER_MINUTES_2 INTEGER, $COL_REMINDER_MINUTES_3 INTEGER, " +
                "$COL_IMPORT_ID TEXT, $COL_FLAGS INTEGER, $COL_EVENT_TYPE INTEGER NOT NULL DEFAULT $REGULAR_EVENT_TYPE_ID, " +
                "$COL_PARENT_EVENT_ID INTEGER)")

        createMetaTable(db)
        createTypesTable(db)
        createExceptionsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_REMINDER_MINUTES INTEGER DEFAULT -1")
        }

        if (oldVersion < 3) {
            createMetaTable(db)
        }

        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_IMPORT_ID TEXT DEFAULT ''")
        }

        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_FLAGS INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $META_TABLE_NAME ADD COLUMN $COL_REPEAT_LIMIT INTEGER NOT NULL DEFAULT 0")
        }

        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_REMINDER_MINUTES_2 INTEGER NOT NULL DEFAULT -1")
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_REMINDER_MINUTES_3 INTEGER NOT NULL DEFAULT -1")
        }

        if (oldVersion < 7) {
            createTypesTable(db)
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_EVENT_TYPE INTEGER NOT NULL DEFAULT $REGULAR_EVENT_TYPE_ID")
        }

        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_PARENT_EVENT_ID INTEGER NOT NULL DEFAULT 0")
            createExceptionsTable(db)
        }

        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE $EXCEPTIONS_TABLE_NAME ADD COLUMN $COL_OCCURRENCE_DAYCODE INTEGER NOT NULL DEFAULT 0")
            } catch (e: SQLiteException) {
            }
            convertExceptionTimestampToDaycode(db)
        }

        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE $META_TABLE_NAME ADD COLUMN $COL_REPEAT_RULE INTEGER NOT NULL DEFAULT 0")
            setupRepeatRules(db)
        }
    }

    private fun createMetaTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $META_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY, $COL_EVENT_ID INTEGER UNIQUE, $COL_REPEAT_START INTEGER, " +
                "$COL_REPEAT_INTERVAL INTEGER, $COL_REPEAT_LIMIT INTEGER, $COL_REPEAT_RULE INTEGER)")
    }

    private fun createTypesTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TYPES_TABLE_NAME ($COL_TYPE_ID INTEGER PRIMARY KEY, $COL_TYPE_TITLE TEXT, $COL_TYPE_COLOR INTEGER)")
        addRegularEventType(db)
    }

    private fun createExceptionsTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $EXCEPTIONS_TABLE_NAME ($COL_EXCEPTION_ID INTEGER PRIMARY KEY, $COL_PARENT_EVENT_ID INTEGER, $COL_OCCURRENCE_TIMESTAMP INTEGER, " +
                "$COL_OCCURRENCE_DAYCODE INTEGER)")
    }

    private fun addRegularEventType(db: SQLiteDatabase) {
        val regularEvent = context.resources.getString(R.string.regular_event)
        val eventType = EventType(REGULAR_EVENT_TYPE_ID, regularEvent, context.config.primaryColor)
        addEventType(eventType, db)
    }

    fun insert(event: Event): Int {
        if (event.startTS > event.endTS || event.title.trim().isEmpty())
            return 0

        val eventValues = fillEventValues(event)
        val id = mDb.insert(MAIN_TABLE_NAME, null, eventValues)
        event.id = id.toInt()

        if (event.repeatInterval != 0) {
            val metaValues = fillMetaValues(event)
            mDb.insert(META_TABLE_NAME, null, metaValues)
        }

        context.updateWidgets()
        context.scheduleReminder(event, this)
        mEventsListener?.eventInserted(event)
        return event.id
    }

    fun update(event: Event) {
        val selectionArgs = arrayOf(event.id.toString())
        val values = fillEventValues(event)
        val selection = "$COL_ID = ?"
        mDb.update(MAIN_TABLE_NAME, values, selection, selectionArgs)

        if (event.repeatInterval == 0) {
            val metaSelection = "$COL_EVENT_ID = ?"
            mDb.delete(META_TABLE_NAME, metaSelection, selectionArgs)
        } else {
            val metaValues = fillMetaValues(event)
            mDb.insertWithOnConflict(META_TABLE_NAME, null, metaValues, SQLiteDatabase.CONFLICT_REPLACE)
        }

        context.updateWidgets()
        context.scheduleReminder(event, this)
        mEventsListener?.eventUpdated(event)
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
            put(COL_PARENT_EVENT_ID, 0)
        }
    }

    private fun fillMetaValues(event: Event): ContentValues {
        return ContentValues().apply {
            put(COL_EVENT_ID, event.id)
            put(COL_REPEAT_START, event.startTS)
            put(COL_REPEAT_INTERVAL, event.repeatInterval)
            put(COL_REPEAT_LIMIT, event.repeatLimit)
            put(COL_REPEAT_RULE, event.repeatRule)
        }
    }

    private fun addEventType(eventType: EventType, db: SQLiteDatabase) {
        insertEventType(eventType, db)
    }

    fun insertEventType(eventType: EventType, db: SQLiteDatabase = mDb): Int {
        val values = fillEventTypeValues(eventType)
        val insertedId = db.insert(TYPES_TABLE_NAME, null, values).toInt()
        context.config.addDisplayEventType(insertedId.toString())
        return insertedId
    }

    fun updateEventType(eventType: EventType): Int {
        val selectionArgs = arrayOf(eventType.id.toString())
        val values = fillEventTypeValues(eventType)
        val selection = "$COL_TYPE_ID = ?"
        return mDb.update(TYPES_TABLE_NAME, values, selection, selectionArgs)
    }

    private fun fillEventTypeValues(eventType: EventType): ContentValues {
        return ContentValues().apply {
            put(COL_TYPE_TITLE, eventType.title)
            put(COL_TYPE_COLOR, eventType.color)
        }
    }

    private fun fillExceptionValues(parentEventId: Int, occurrenceTS: Int): ContentValues {
        return ContentValues().apply {
            put(COL_PARENT_EVENT_ID, parentEventId)
            put(COL_OCCURRENCE_DAYCODE, Formatter.getDayCodeFromTS(occurrenceTS))
        }
    }

    fun getEventTypeIdWithTitle(title: String): Int {
        val cols = arrayOf(COL_TYPE_ID)
        val selection = "$COL_TYPE_TITLE = ? COLLATE NOCASE"
        val selectionArgs = arrayOf(title)
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(TYPES_TABLE_NAME, cols, selection, selectionArgs, null, null, null)
            if (cursor?.moveToFirst() == true) {
                return cursor.getIntValue(COL_TYPE_ID)
            }
        } finally {
            cursor?.close()
        }
        return -1
    }

    fun getEventType(id: Int): EventType? {
        val cols = arrayOf(COL_TYPE_TITLE, COL_TYPE_COLOR)
        val selection = "$COL_TYPE_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(TYPES_TABLE_NAME, cols, selection, selectionArgs, null, null, null)
            if (cursor?.moveToFirst() == true) {
                val title = cursor.getStringValue(COL_TYPE_TITLE)
                val color = cursor.getIntValue(COL_TYPE_COLOR)
                return EventType(id, title, color)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    fun deleteEvents(ids: Array<String>) {
        val args = TextUtils.join(", ", ids)
        val selection = "$COL_ID IN ($args)"
        mDb.delete(MAIN_TABLE_NAME, selection, null)

        val metaSelection = "$COL_EVENT_ID IN ($args)"
        mDb.delete(META_TABLE_NAME, metaSelection, null)

        val exceptionSelection = "$COL_PARENT_EVENT_ID IN ($args)"
        mDb.delete(EXCEPTIONS_TABLE_NAME, exceptionSelection, null)

        context.updateWidgets()
        mEventsListener?.eventsDeleted(ids.size)

        ids.forEach {
            context.cancelNotification(it.toInt())
        }
    }

    fun addEventRepeatException(parentEventId: Int, occurrenceTS: Int) {
        val values = fillExceptionValues(parentEventId, occurrenceTS)
        mDb.insert(EXCEPTIONS_TABLE_NAME, null, values)
    }

    fun deleteEventTypes(ids: ArrayList<Int>, callback: (deletedCnt: Int) -> Unit) {
        var deleteIds = ids
        if (ids.contains(DBHelper.REGULAR_EVENT_TYPE_ID))
            deleteIds = ids.filter { it != DBHelper.REGULAR_EVENT_TYPE_ID } as ArrayList<Int>

        val deletedSet = HashSet<String>()
        deleteIds.map { deletedSet.add(it.toString()) }
        context.config.removeDisplayEventTypes(deletedSet)
        if (deleteIds.isEmpty())
            return

        for (eventTypeId in deleteIds) {
            resetEventsWithType(eventTypeId)
        }

        val args = TextUtils.join(", ", deleteIds)
        val selection = "$COL_TYPE_ID IN ($args)"
        callback.invoke(mDb.delete(TYPES_TABLE_NAME, selection, null))
    }

    private fun resetEventsWithType(eventTypeId: Int) {
        val values = ContentValues()
        values.put(COL_EVENT_TYPE, REGULAR_EVENT_TYPE_ID)

        val selection = "$COL_EVENT_TYPE = ?"
        val selectionArgs = arrayOf(eventTypeId.toString())
        mDb.update(MAIN_TABLE_NAME, values, selection, selectionArgs)
    }

    fun getImportIds(): ArrayList<String> {
        val ids = ArrayList<String>()
        val columns = arrayOf(COL_IMPORT_ID)
        val selection = "$COL_IMPORT_ID IS NOT NULL"
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(MAIN_TABLE_NAME, columns, selection, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getStringValue(COL_IMPORT_ID)
                    ids.add(id)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return ids.filter { it.trim().isNotEmpty() } as ArrayList<String>
    }

    fun getEventWithId(id: Int): Event? {
        val selection = "$MAIN_TABLE_NAME.$COL_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        val events = fillEvents(cursor)
        return if (!events.isEmpty())
            events[0]
        else
            null
    }

    fun getEvents(fromTS: Int, toTS: Int, eventId: Int = -1, callback: (events: MutableList<Event>) -> Unit) {
        Thread({
            getEventsInBackground(fromTS, toTS, eventId, callback)
        }).start()
    }

    fun getEventsInBackground(fromTS: Int, toTS: Int, eventId: Int = -1, callback: (events: MutableList<Event>) -> Unit) {
        val events = ArrayList<Event>()

        var selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS NULL"
        if (eventId != -1)
            selection += " AND $MAIN_TABLE_NAME.$COL_ID = $eventId"
        val selectionArgs = arrayOf(toTS.toString(), fromTS.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        events.addAll(fillEvents(cursor))

        events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId))

        val filtered = events.filterNot { it.ignoreEventOccurrences.contains(Formatter.getDayCodeFromTS(it.startTS).toInt()) } as MutableList<Event>
        callback(filtered)
    }

    private fun getRepeatableEventsFor(fromTS: Int, toTS: Int, eventId: Int = -1): List<Event> {
        val newEvents = ArrayList<Event>()

        // get repeatable events
        var selection = "$COL_REPEAT_INTERVAL != 0 AND $COL_START_TS <= $toTS"
        if (eventId != -1)
            selection += " AND $MAIN_TABLE_NAME.$COL_ID = $eventId"
        val events = getEvents(selection)
        val startTimes = SparseIntArray(events.size)
        events.forEach {
            startTimes.put(it.id, it.startTS)
            if (it.repeatLimit >= 0) {
                newEvents.addAll(getEventsRepeatingTillDateOrForever(fromTS, toTS, startTimes, it))
            } else {
                newEvents.addAll(getEventsRepeatingXTimes(fromTS, toTS, startTimes, it))
            }
        }

        return newEvents
    }

    private fun getEventsRepeatingTillDateOrForever(fromTS: Int, toTS: Int, startTimes: SparseIntArray, event: Event): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.startTS <= toTS && (event.repeatLimit == 0 || event.repeatLimit >= event.startTS)) {
            if (event.endTS >= fromTS) {
                if (event.repeatInterval.isXWeeklyRepetition()) {
                    if (event.startTS.isTsOnProperDay(event)) {
                        if (isOnProperWeek(event, startTimes)) {
                            events.add(event.copy())
                        }
                    }
                } else {
                    events.add(event.copy())
                }
            }
            event.addIntervalTime(original)
        }
        return events
    }

    private fun getEventsRepeatingXTimes(fromTS: Int, toTS: Int, startTimes: SparseIntArray, event: Event): ArrayList<Event> {
        val original = event.copy()
        val events = ArrayList<Event>()
        while (event.repeatLimit < 0 && event.startTS <= toTS) {
            if (event.repeatInterval.isXWeeklyRepetition()) {
                if (event.startTS.isTsOnProperDay(event)) {
                    if (isOnProperWeek(event, startTimes)) {
                        if (event.endTS >= fromTS) {
                            events.add(event.copy())
                        }
                        event.repeatLimit++
                    }
                }
            } else {
                if (event.endTS >= fromTS) {
                    events.add(event.copy())
                }
                event.repeatLimit++
            }
            event.addIntervalTime(original)
        }
        return events
    }

    // check if its the proper week, for events repeating by x weeks
    private fun isOnProperWeek(event: Event, startTimes: SparseIntArray): Boolean {
        val initialWeekOfYear = Formatter.getDateTimeFromTS(startTimes[event.id]).weekOfWeekyear
        val currentWeekOfYear = Formatter.getDateTimeFromTS(event.startTS).weekOfWeekyear
        return (currentWeekOfYear - initialWeekOfYear) % (event.repeatInterval / WEEK) == 0
    }

    fun getRunningEvents(): List<Event> {
        val events = ArrayList<Event>()
        val ts = (System.currentTimeMillis() / 1000).toInt()

        val selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS 0"
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

    fun getEventsWithIds(ids: List<Int>): ArrayList<Event> {
        val args = TextUtils.join(", ", ids)
        val selection = "$MAIN_TABLE_NAME.$COL_ID IN ($args)"
        return getEvents(selection) as ArrayList<Event>
    }

    fun getEventsAtReboot(): List<Event> {
        val selection = "$COL_REMINDER_MINUTES != -1 AND ($COL_START_TS > ? OR $COL_REPEAT_INTERVAL != 0)"
        val selectionArgs = arrayOf(DateTime.now().seconds().toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor)
    }

    fun getEventsToExport(includePast: Boolean): ArrayList<Event> {
        val currTime = (System.currentTimeMillis() / 1000).toString()
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

    private fun getEventsCursor(selection: String = "", selectionArgs: Array<String>? = null): Cursor? {
        val builder = SQLiteQueryBuilder()
        builder.tables = "$MAIN_TABLE_NAME LEFT OUTER JOIN $META_TABLE_NAME ON $COL_EVENT_ID = $MAIN_TABLE_NAME.$COL_ID"
        val projection = allColumns
        return builder.query(mDb, projection, selection, selectionArgs, "$MAIN_TABLE_NAME.$COL_ID", null, COL_START_TS)
    }

    private val allColumns: Array<String>
        get() = arrayOf("$MAIN_TABLE_NAME.$COL_ID", COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION, COL_REMINDER_MINUTES, COL_REMINDER_MINUTES_2,
                COL_REMINDER_MINUTES_3, COL_REPEAT_INTERVAL, COL_REPEAT_RULE, COL_IMPORT_ID, COL_FLAGS, COL_REPEAT_LIMIT, COL_EVENT_TYPE)

    private fun fillEvents(cursor: Cursor?): List<Event> {
        val events = ArrayList<Event>()
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getIntValue(COL_ID)
                    val startTS = cursor.getIntValue(COL_START_TS)
                    val endTS = cursor.getIntValue(COL_END_TS)
                    val reminder1Minutes = cursor.getIntValue(COL_REMINDER_MINUTES)
                    val reminder2Minutes = cursor.getIntValue(COL_REMINDER_MINUTES_2)
                    val reminder3Minutes = cursor.getIntValue(COL_REMINDER_MINUTES_3)
                    val repeatInterval = cursor.getIntValue(COL_REPEAT_INTERVAL)
                    var repeatRule = cursor.getIntValue(COL_REPEAT_RULE)
                    val title = cursor.getStringValue(COL_TITLE)
                    val description = cursor.getStringValue(COL_DESCRIPTION)
                    val importId = cursor.getStringValue(COL_IMPORT_ID)
                    val flags = cursor.getIntValue(COL_FLAGS)
                    val repeatLimit = cursor.getIntValue(COL_REPEAT_LIMIT)
                    val eventType = cursor.getIntValue(COL_EVENT_TYPE)

                    val ignoreEventOccurrences = if (repeatInterval != 0) {
                        getIgnoredOccurrences(id)
                    } else {
                        ArrayList<Int>()
                    }

                    if (repeatInterval % MONTH == 0 && repeatRule == 0) {
                        repeatRule = REPEAT_MONTH_SAME_DAY
                    }

                    val event = Event(id, startTS, endTS, title, description, reminder1Minutes, reminder2Minutes, reminder3Minutes,
                            repeatInterval, importId, flags, repeatLimit, repeatRule, eventType, ignoreEventOccurrences)
                    events.add(event)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return events
    }

    fun getEventTypes(callback: (types: ArrayList<EventType>) -> Unit) {
        Thread({
            fetchEventTypes(callback)
        }).start()
    }

    fun fetchEventTypes(callback: (types: ArrayList<EventType>) -> Unit) {
        val eventTypes = ArrayList<EventType>(3)
        val cols = arrayOf(COL_TYPE_ID, COL_TYPE_TITLE, COL_TYPE_COLOR)
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(TYPES_TABLE_NAME, cols, null, null, null, null, "$COL_TYPE_TITLE ASC")
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(COL_TYPE_ID)
                    val title = cursor.getStringValue(COL_TYPE_TITLE)
                    val color = cursor.getIntValue(COL_TYPE_COLOR)
                    val eventType = EventType(id, title, color)
                    eventTypes.add(eventType)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        callback.invoke(eventTypes)
    }

    private fun getIgnoredOccurrences(eventId: Int): ArrayList<Int> {
        val projection = arrayOf(COL_OCCURRENCE_DAYCODE)
        val selection = "$COL_PARENT_EVENT_ID = ?"
        val selectionArgs = arrayOf(eventId.toString())
        val daycodes = ArrayList<Int>()

        var cursor: Cursor? = null
        try {
            cursor = mDb.query(EXCEPTIONS_TABLE_NAME, projection, selection, selectionArgs, null, null, COL_OCCURRENCE_DAYCODE)
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

    private fun convertExceptionTimestampToDaycode(db: SQLiteDatabase) {
        val projection = arrayOf(COL_EXCEPTION_ID, COL_OCCURRENCE_TIMESTAMP)
        var cursor: Cursor? = null
        try {
            cursor = db.query(EXCEPTIONS_TABLE_NAME, projection, null, null, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getIntValue(COL_EXCEPTION_ID)
                    val ts = cursor.getIntValue(COL_OCCURRENCE_TIMESTAMP)
                    val values = ContentValues()
                    values.put(COL_OCCURRENCE_DAYCODE, Formatter.getDayCodeFromTS(ts))

                    val selection = "$COL_EXCEPTION_ID = ?"
                    val selectionArgs = arrayOf(id.toString())
                    db.update(EXCEPTIONS_TABLE_NAME, values, selection, selectionArgs)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
    }

    private fun setupRepeatRules(db: SQLiteDatabase) {
        val projection = arrayOf(COL_EVENT_ID, COL_REPEAT_INTERVAL, COL_REPEAT_START)
        val selection = "$COL_REPEAT_INTERVAL != 0"
        var cursor: Cursor? = null
        try {
            cursor = db.query(META_TABLE_NAME, projection, selection, null, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val interval = cursor.getIntValue(COL_REPEAT_INTERVAL)
                    if (interval != MONTH && interval % WEEK != 0)
                        continue

                    val eventId = cursor.getIntValue(COL_EVENT_ID)
                    val start = cursor.getIntValue(COL_REPEAT_START)
                    var rule = Math.pow(2.0, (Formatter.getDateTimeFromTS(start).dayOfWeek - 1).toDouble()).toInt()
                    if (interval == MONTH) {
                        rule = REPEAT_MONTH_SAME_DAY
                    }

                    val values = ContentValues()
                    values.put(COL_REPEAT_RULE, rule)
                    val curSelection = "$COL_EVENT_ID = ?"
                    val curSelectionArgs = arrayOf(eventId.toString())
                    db.update(META_TABLE_NAME, values, curSelection, curSelectionArgs)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
    }

    interface EventUpdateListener {
        fun eventInserted(event: Event)

        fun eventUpdated(event: Event)

        fun eventsDeleted(cnt: Int)

        fun gotEvents(events: MutableList<Event>)
    }
}
