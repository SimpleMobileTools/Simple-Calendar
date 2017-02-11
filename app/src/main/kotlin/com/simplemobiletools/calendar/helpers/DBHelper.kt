package com.simplemobiletools.calendar.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.text.TextUtils
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.scheduleReminder
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.extensions.updateWidgets
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
    private val COL_TYPE = "type"

    private val META_TABLE_NAME = "events_meta"
    private val COL_EVENT_ID = "event_id"
    private val COL_REPEAT_START = "repeat_start"
    private val COL_REPEAT_INTERVAL = "repeat_interval"
    private val COL_REPEAT_MONTH = "repeat_month"
    private val COL_REPEAT_DAY = "repeat_day"
    private val COL_REPEAT_LIMIT = "repeat_limit"

    private val TYPES_TABLE_NAME = "event_types"
    private val COL_TYPE_ID = "event_type_id"
    private val COL_TYPE_TITLE = "event_type_title"
    private val COL_TYPE_COLOR = "event_type_color"

    private val mDb: SQLiteDatabase = writableDatabase

    companion object {
        private val DB_NAME = "events.db"
        private val DB_VERSION = 7
        private var mEventsListener: EventUpdateListener? = null

        fun newInstance(context: Context, callback: EventUpdateListener? = null): DBHelper {
            mEventsListener = callback
            return DBHelper(context)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $MAIN_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY, $COL_START_TS INTEGER, $COL_END_TS INTEGER, $COL_TITLE TEXT, " +
                "$COL_DESCRIPTION TEXT, $COL_REMINDER_MINUTES INTEGER, $COL_REMINDER_MINUTES_2 INTEGER, $COL_REMINDER_MINUTES_3 INTEGER, " +
                "$COL_IMPORT_ID TEXT, $COL_FLAGS INTEGER, $COL_TYPE INTEGER)")

        createMetaTable(db)
        createTypesTable(db)
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
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_TYPE INTEGER NOT NULL DEFAULT 0")
        }
    }

    private fun createMetaTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $META_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY, $COL_EVENT_ID INTEGER UNIQUE, $COL_REPEAT_START INTEGER, " +
                "$COL_REPEAT_INTERVAL INTEGER, $COL_REPEAT_MONTH INTEGER, $COL_REPEAT_DAY INTEGER, $COL_REPEAT_LIMIT INTEGER)")
    }

    private fun createTypesTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TYPES_TABLE_NAME ($COL_TYPE_ID INTEGER PRIMARY KEY, $COL_TYPE_TITLE TEXT, $COL_TYPE_COLOR INTEGER)")
        addRegularEventType(db)
    }

    private fun addRegularEventType(db: SQLiteDatabase) {
        val regularEvent = context.resources.getString(R.string.regular_event)
        val eventType = EventType(1, regularEvent, context.config.primaryColor)
        insertEventType(eventType, db)
    }

    fun insert(event: Event, insertListener: (event: Event) -> Unit) {
        if (event.startTS > event.endTS || event.title.trim().isEmpty())
            return

        val eventValues = fillEventValues(event)
        val id = mDb.insert(MAIN_TABLE_NAME, null, eventValues)
        event.id = id.toInt()

        if (event.repeatInterval != 0) {
            val metaValues = fillMetaValues(event)
            mDb.insert(META_TABLE_NAME, null, metaValues)
        }

        context.updateWidgets()
        context.scheduleReminder(event)
        mEventsListener?.eventInserted(event)
        insertListener.invoke(event)
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
        context.scheduleReminder(event)
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
        }
    }

    private fun fillMetaValues(event: Event): ContentValues {
        val repeatInterval = event.repeatInterval
        val dateTime = Formatter.getDateTimeFromTS(event.startTS)

        return ContentValues().apply {
            put(COL_EVENT_ID, event.id)
            put(COL_REPEAT_START, event.startTS)
            put(COL_REPEAT_INTERVAL, repeatInterval)
            put(COL_REPEAT_LIMIT, event.repeatLimit)

            if (repeatInterval == MONTH || repeatInterval == YEAR) {
                put(COL_REPEAT_DAY, dateTime.dayOfMonth)
            }

            if (repeatInterval == YEAR) {
                put(COL_REPEAT_MONTH, dateTime.monthOfYear)
            }
        }
    }

    private fun insertEventType(eventType: EventType, db: SQLiteDatabase) {
        val values = fillEventTypeValues(eventType)
        db.insert(TYPES_TABLE_NAME, null, values)
    }

    private fun fillEventTypeValues(eventType: EventType): ContentValues {
        return ContentValues().apply {
            put(COL_TYPE_ID, eventType.id)
            put(COL_TYPE_TITLE, eventType.title)
            put(COL_TYPE_COLOR, eventType.color)
        }
    }

    fun deleteEvents(ids: Array<String>) {
        val args = TextUtils.join(", ", ids)
        val selection = "$COL_ID IN ($args)"
        mDb.delete(MAIN_TABLE_NAME, selection, null)

        val metaSelection = "$COL_EVENT_ID IN ($args)"
        mDb.delete(META_TABLE_NAME, metaSelection, null)

        context.updateWidgets()
        mEventsListener?.eventsDeleted(ids.size)
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
        return ids
    }

    fun getEvent(id: Int): Event? {
        val selection = "$MAIN_TABLE_NAME.$COL_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        val events = fillEvents(cursor)
        if (!events.isEmpty())
            return events[0]

        return null
    }

    fun getEvents(fromTS: Int, toTS: Int, callback: GetEventsListener?) {
        Thread({
            getEventsInBackground(fromTS, toTS, callback)
        }).start()
    }

    fun getEventsInBackground(fromTS: Int, toTS: Int, callback: GetEventsListener?) {
        val events = ArrayList<Event>()
        events.addAll(getEventsFor(fromTS, toTS))

        val selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS NULL"
        val selectionArgs = arrayOf(toTS.toString(), fromTS.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        events.addAll(fillEvents(cursor))
        callback?.gotEvents(events)
    }

    private fun getEventsFor(fromTS: Int, toTS: Int): List<Event> {
        val newEvents = ArrayList<Event>()

        // get repeatable events
        val selection = "$COL_REPEAT_INTERVAL != 0 AND $COL_START_TS < $toTS"
        val events = getEvents(selection)
        for (e in events) {
            while (e.startTS < toTS && (e.repeatLimit == 0 || e.repeatLimit > e.endTS)) {
                if (e.startTS > fromTS) {
                    val newEvent = Event(e.id, e.startTS, e.endTS, e.title, e.description, e.reminder1Minutes, e.reminder2Minutes, e.reminder3Minutes,
                            e.repeatInterval, e.importId, e.flags)
                    newEvents.add(newEvent)
                }
                e.addIntervalTime()
            }
        }

        return newEvents
    }

    private fun getEvents(selection: String): List<Event> {
        val events = ArrayList<Event>()
        var cursor: Cursor? = null
        try {
            cursor = getEventsCursor(selection, null)
            if (cursor != null) {
                val currEvents = fillEvents(cursor)
                events.addAll(currEvents)
            }
        } finally {
            cursor?.close()
        }

        return events
    }

    fun getEventsAtReboot(): List<Event> {
        val selection = "$COL_REMINDER_MINUTES != -1 AND ($COL_START_TS > ? OR $COL_REPEAT_INTERVAL != 0)"
        val selectionArgs = arrayOf(DateTime.now().seconds().toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor)
    }

    private fun getEventsCursor(selection: String, selectionArgs: Array<String>?): Cursor? {
        val builder = SQLiteQueryBuilder()
        builder.tables = "$MAIN_TABLE_NAME LEFT OUTER JOIN $META_TABLE_NAME ON $COL_EVENT_ID = $MAIN_TABLE_NAME.$COL_ID"
        val projection = allColumns
        return builder.query(mDb, projection, selection, selectionArgs, "$MAIN_TABLE_NAME.$COL_ID", null, COL_START_TS)
    }

    private val allColumns: Array<String>
        get() = arrayOf("$MAIN_TABLE_NAME.$COL_ID", COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION, COL_REMINDER_MINUTES, COL_REMINDER_MINUTES_2,
                COL_REMINDER_MINUTES_3, COL_REPEAT_INTERVAL, COL_REPEAT_MONTH, COL_REPEAT_DAY, COL_IMPORT_ID, COL_FLAGS, COL_REPEAT_LIMIT)

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
                    val title = cursor.getStringValue(COL_TITLE)
                    val description = cursor.getStringValue(COL_DESCRIPTION)
                    val importId = cursor.getStringValue(COL_IMPORT_ID)
                    val flags = cursor.getIntValue(COL_FLAGS)
                    val repeatLimit = cursor.getIntValue(COL_REPEAT_LIMIT)
                    events.add(Event(id, startTS, endTS, title, description, reminder1Minutes, reminder2Minutes, reminder3Minutes,
                            repeatInterval, importId, flags, repeatLimit))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return events
    }

    interface EventUpdateListener {
        fun eventInserted(event: Event)

        fun eventUpdated(event: Event)

        fun eventsDeleted(cnt: Int)

        fun gotEvents(events: MutableList<Event>)
    }

    interface GetEventsListener {
        fun gotEvents(events: MutableList<Event>)
    }
}
