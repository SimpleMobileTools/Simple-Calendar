package com.simplemobiletools.calendar

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.text.TextUtils
import com.simplemobiletools.calendar.models.Event
import org.joda.time.DateTime
import java.util.*

class DBHelper(context: Context) : SQLiteOpenHelper(context, DBHelper.DB_NAME, null, DBHelper.DB_VERSION) {

    init {
        mDb = writableDatabase
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE " + MAIN_TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_START_TS + " INTEGER," +
                COL_END_TS + " INTEGER," +
                COL_TITLE + " TEXT," +
                COL_DESCRIPTION + " TEXT," +
                COL_REMINDER_MINUTES + " INTEGER" +
                ")")

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
        db.execSQL("CREATE TABLE " + META_TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_EVENT_ID + " INTEGER UNIQUE, " +
                COL_REPEAT_START + " INTEGER, " +
                COL_REPEAT_INTERVAL + " INTEGER, " +
                COL_REPEAT_MONTH + " INTEGER, " +
                COL_REPEAT_DAY + " INTEGER" +
                ")")
    }

    fun insert(event: Event) {
        val eventValues = fillContentValues(event)
        val id = mDb.insert(MAIN_TABLE_NAME, null, eventValues)
        event.id = id.toInt()
        if (event.repeatInterval != 0) {
            val metaValues = fillMetaValues(event)
            mDb.insert(META_TABLE_NAME, null, metaValues)
        }

        if (mCallback != null)
            mCallback!!.eventInserted(event)
    }

    fun update(event: Event) {
        val selectionArgs = arrayOf(event.id.toString())
        val values = fillContentValues(event)
        val selection = COL_ID + " = ?"
        mDb.update(MAIN_TABLE_NAME, values, selection, selectionArgs)

        if (event.repeatInterval == 0) {
            val metaSelection = COL_EVENT_ID + " = ?"
            mDb.delete(META_TABLE_NAME, metaSelection, selectionArgs)
        } else {
            val metaValues = fillMetaValues(event)
            mDb.insertWithOnConflict(META_TABLE_NAME, null, metaValues, SQLiteDatabase.CONFLICT_REPLACE)
        }

        if (mCallback != null)
            mCallback!!.eventUpdated(event)
    }

    private fun fillContentValues(event: Event): ContentValues {
        val values = ContentValues()
        values.put(COL_START_TS, event.startTS)
        values.put(COL_END_TS, event.endTS)
        values.put(COL_TITLE, event.title)
        values.put(COL_DESCRIPTION, event.description)
        values.put(COL_REMINDER_MINUTES, event.reminderMinutes)
        return values
    }

    private fun fillMetaValues(event: Event): ContentValues {
        val repeatInterval = event.repeatInterval
        val values = ContentValues()
        values.put(COL_EVENT_ID, event.id)
        values.put(COL_REPEAT_START, event.startTS)
        values.put(COL_REPEAT_INTERVAL, repeatInterval)
        val dateTime = Formatter.getDateTimeFromTS(event.startTS)

        if (repeatInterval == Constants.MONTH || repeatInterval == Constants.YEAR) {
            values.put(COL_REPEAT_DAY, dateTime.dayOfMonth)
        }

        if (repeatInterval == Constants.YEAR) {
            values.put(COL_REPEAT_MONTH, dateTime.monthOfYear)
        }

        return values
    }

    fun deleteEvents(ids: Array<String>) {
        val args = TextUtils.join(", ", ids)
        val selection = "$COL_ID IN ($args)"
        mDb.delete(MAIN_TABLE_NAME, selection, null)

        val metaSelection = "$COL_EVENT_ID IN ($args)"
        mDb.delete(META_TABLE_NAME, metaSelection, null)

        if (mCallback != null)
            mCallback!!.eventsDeleted(ids.size)
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

    fun getEvents(fromTS: Int, toTS: Int) {
        val events = ArrayList<Event>()
        var ts = fromTS
        while (ts <= toTS) {
            events.addAll(getEventsFor(ts))
            ts += Constants.DAY
        }

        val selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS NULL"
        val selectionArgs = arrayOf(toTS.toString(), fromTS.toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        events.addAll(fillEvents(cursor))

        if (mCallback != null)
            mCallback!!.gotEvents(events)
    }

    private fun getEventsFor(ts: Int): List<Event> {
        val newEvents = ArrayList<Event>()
        val dayExclusive = Constants.DAY - 1
        val dayEnd = ts + dayExclusive
        val dateTime = Formatter.getDateTimeFromTS(ts)

        // get daily and weekly events
        var selection = "(" + COL_REPEAT_INTERVAL + " = " + Constants.DAY + " OR " + COL_REPEAT_INTERVAL + " = " + Constants.WEEK +
                ") AND (" + dayEnd + " - " + COL_REPEAT_START + ") % " + COL_REPEAT_INTERVAL + " BETWEEN 0 AND " + dayExclusive
        newEvents.addAll(getEvents(selection, ts))

        // get monthly events
        selection = COL_REPEAT_INTERVAL + " = " + Constants.MONTH + " AND " + COL_REPEAT_DAY + " = " + dateTime.dayOfMonth +
                " AND " + COL_REPEAT_START + " <= " + dayEnd
        newEvents.addAll(getEvents(selection, ts))

        // get yearly events
        selection = COL_REPEAT_INTERVAL + " = " + Constants.YEAR + " AND " + COL_REPEAT_MONTH + " = " + dateTime.monthOfYear +
                " AND " + COL_REPEAT_DAY + " = " + dateTime.dayOfMonth + " AND " + COL_REPEAT_START + " <= " + dayEnd
        newEvents.addAll(getEvents(selection, ts))

        return newEvents
    }

    private fun getEvents(selection: String, ts: Int): List<Event> {
        val events = ArrayList<Event>()
        val cursor = getEventsCursor(selection, null)
        if (cursor != null) {
            val currEvents = fillEvents(cursor)
            for (e in currEvents) {
                updateEventTimes(e, ts)
            }
            events.addAll(currEvents)
        }

        return events
    }

    private fun updateEventTimes(e: Event, ts: Int) {
        val periods = (ts - e.startTS + Constants.DAY) / e.repeatInterval
        val currStart = Formatter.getDateTimeFromTS(e.startTS)
        val newStart: DateTime
        if (e.repeatInterval == Constants.DAY) {
            newStart = currStart.plusDays(periods)
        } else if (e.repeatInterval == Constants.WEEK) {
            newStart = currStart.plusWeeks(periods)
        } else if (e.repeatInterval == Constants.MONTH) {
            newStart = currStart.plusMonths(periods)
        } else {
            newStart = currStart.plusYears(periods)
        }

        val newStartTS = (newStart.millis / 1000).toInt()
        val newEndTS = newStartTS + (e.endTS - e.startTS)
        e.startTS = newStartTS
        e.endTS = newEndTS
    }

    fun getEventsAtReboot(): List<Event> {
        val selection = "$COL_REMINDER_MINUTES != -1 AND ($COL_START_TS > ? OR $COL_REPEAT_INTERVAL != 0)"
        val selectionArgs = arrayOf((DateTime.now().millis / 1000).toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor)
    }

    private fun getEventsCursor(selection: String, selectionArgs: Array<String>?): Cursor? {
        val builder = SQLiteQueryBuilder()
        builder.tables = "$MAIN_TABLE_NAME LEFT OUTER JOIN $META_TABLE_NAME ON $COL_EVENT_ID = $MAIN_TABLE_NAME.$COL_ID"
        val projection = allColumns
        return builder.query(mDb, projection, selection, selectionArgs, MAIN_TABLE_NAME + "." + COL_ID, null, COL_START_TS)
    }

    private val allColumns: Array<String>
        get() = arrayOf(MAIN_TABLE_NAME + "." + COL_ID, COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION, COL_REMINDER_MINUTES, COL_REPEAT_INTERVAL, COL_REPEAT_MONTH, COL_REPEAT_DAY)

    private fun fillEvents(cursor: Cursor?): List<Event> {
        val events = ArrayList<Event>()
        if (cursor == null)
            return events

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(COL_ID))
                val startTS = cursor.getInt(cursor.getColumnIndex(COL_START_TS))
                val endTS = cursor.getInt(cursor.getColumnIndex(COL_END_TS))
                val reminderMinutes = cursor.getInt(cursor.getColumnIndex(COL_REMINDER_MINUTES))
                val repeatInterval = cursor.getInt(cursor.getColumnIndex(COL_REPEAT_INTERVAL))
                val title = cursor.getString(cursor.getColumnIndex(COL_TITLE))
                val description = cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION))
                events.add(Event(id, startTS, endTS, title, description, reminderMinutes, repeatInterval))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return events
    }

    interface DBOperationsListener {
        fun eventInserted(event: Event)

        fun eventUpdated(event: Event)

        fun eventsDeleted(cnt: Int)

        fun gotEvents(events: MutableList<Event>)
    }

    companion object {
        lateinit private var mDb: SQLiteDatabase
        private var mCallback: DBOperationsListener? = null

        private val DB_NAME = "events.db"
        private val DB_VERSION = 3

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

        fun newInstance(context: Context, callback: DBOperationsListener): DBHelper {
            mCallback = callback
            return DBHelper(context)
        }
    }
}
