package com.simplemobiletools.calendar.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.text.TextUtils
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.extensions.updateWidgets
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue
import org.joda.time.DateTime
import java.util.*

class DBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val MAIN_TABLE_NAME = "events"
    private val COL_ID = "id"
    private val COL_START_TS = "start_ts"
    private val COL_END_TS = "end_ts"
    private val COL_TITLE = "title"
    private val COL_DESCRIPTION = "description"
    private val COL_REMINDER_MINUTES = "reminder_minutes"

    private val META_TABLE_NAME = "events_meta"
    private val COL_EVENT_ID = "event_id"
    private val COL_REPEAT_START = "repeat_start"
    private val COL_REPEAT_INTERVAL = "repeat_interval"
    private val COL_REPEAT_MONTH = "repeat_month"
    private val COL_REPEAT_DAY = "repeat_day"

    private var mEventsListener: EventUpdateListener? = null
    private var context: Context? = null

    companion object {
        private val DB_NAME = "events.db"
        private val DB_VERSION = 3
        lateinit private var mDb: SQLiteDatabase
    }

    constructor(context: Context, callback: EventUpdateListener?) : this(context) {
        mEventsListener = callback
        this.context = context
    }

    init {
        mDb = writableDatabase
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $MAIN_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY, $COL_START_TS INTEGER, $COL_END_TS INTEGER, $COL_TITLE TEXT, " +
                "$COL_DESCRIPTION TEXT, $COL_REMINDER_MINUTES INTEGER)")

        createMetaTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE $MAIN_TABLE_NAME ADD COLUMN $COL_REMINDER_MINUTES INTEGER DEFAULT -1")
        }

        if (newVersion == 3) {
            createMetaTable(db)
        }
    }

    private fun createMetaTable(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $META_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY, $COL_EVENT_ID INTEGER UNIQUE, $COL_REPEAT_START INTEGER, " +
                "$COL_REPEAT_INTERVAL INTEGER, $COL_REPEAT_MONTH INTEGER, $COL_REPEAT_DAY INTEGER)")
    }

    fun insert(event: Event) {
        val eventValues = fillContentValues(event)
        val id = mDb.insert(MAIN_TABLE_NAME, null, eventValues)
        event.id = id.toInt()
        if (event.repeatInterval != 0) {
            val metaValues = fillMetaValues(event)
            mDb.insert(META_TABLE_NAME, null, metaValues)
        }

        context?.updateWidgets()
        mEventsListener?.eventInserted(event)
    }

    fun update(event: Event) {
        val selectionArgs = arrayOf(event.id.toString())
        val values = fillContentValues(event)
        val selection = "$COL_ID = ?"
        mDb.update(MAIN_TABLE_NAME, values, selection, selectionArgs)

        if (event.repeatInterval == 0) {
            val metaSelection = "$COL_EVENT_ID = ?"
            mDb.delete(META_TABLE_NAME, metaSelection, selectionArgs)
        } else {
            val metaValues = fillMetaValues(event)
            mDb.insertWithOnConflict(META_TABLE_NAME, null, metaValues, SQLiteDatabase.CONFLICT_REPLACE)
        }

        context?.updateWidgets()
        mEventsListener?.eventUpdated(event)
    }

    private fun fillContentValues(event: Event): ContentValues {
        return ContentValues().apply {
            put(COL_START_TS, event.startTS)
            put(COL_END_TS, event.endTS)
            put(COL_TITLE, event.title)
            put(COL_DESCRIPTION, event.description)
            put(COL_REMINDER_MINUTES, event.reminderMinutes)
        }
    }

    private fun fillMetaValues(event: Event): ContentValues {
        val repeatInterval = event.repeatInterval
        val dateTime = Formatter.getDateTimeFromTS(event.startTS)

        return ContentValues().apply {
            put(COL_EVENT_ID, event.id)
            put(COL_REPEAT_START, event.startTS)
            put(COL_REPEAT_INTERVAL, repeatInterval)

            if (repeatInterval == MONTH || repeatInterval == YEAR) {
                put(COL_REPEAT_DAY, dateTime.dayOfMonth)
            }

            if (repeatInterval == YEAR) {
                put(COL_REPEAT_MONTH, dateTime.monthOfYear)
            }
        }
    }

    fun deleteEvents(ids: Array<String>) {
        val args = TextUtils.join(", ", ids)
        val selection = "$COL_ID IN ($args)"
        mDb.delete(MAIN_TABLE_NAME, selection, null)

        val metaSelection = "$COL_EVENT_ID IN ($args)"
        mDb.delete(META_TABLE_NAME, metaSelection, null)

        context?.updateWidgets()
        mEventsListener?.eventsDeleted(ids.size)
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
            while (e.startTS < toTS) {
                if (e.startTS > fromTS) {
                    val newEvent = Event(e.id, e.startTS, e.endTS, e.title, e.description, e.reminderMinutes, e.repeatInterval)
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
        get() = arrayOf("$MAIN_TABLE_NAME.$COL_ID", COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION, COL_REMINDER_MINUTES, COL_REPEAT_INTERVAL, COL_REPEAT_MONTH, COL_REPEAT_DAY)

    private fun fillEvents(cursor: Cursor?): List<Event> {
        val events = ArrayList<Event>()
        try {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val id = cursor.getIntValue(COL_ID)
                    val startTS = cursor.getIntValue(COL_START_TS)
                    val endTS = cursor.getIntValue(COL_END_TS)
                    val reminderMinutes = cursor.getIntValue(COL_REMINDER_MINUTES)
                    val repeatInterval = cursor.getIntValue(COL_REPEAT_INTERVAL)
                    val title = cursor.getStringValue(COL_TITLE)
                    val description = cursor.getStringValue(COL_DESCRIPTION)
                    events.add(Event(id, startTS, endTS, title, description, reminderMinutes, repeatInterval))
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
