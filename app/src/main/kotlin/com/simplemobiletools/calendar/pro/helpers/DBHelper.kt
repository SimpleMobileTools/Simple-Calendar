package com.simplemobiletools.calendar.pro.helpers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import androidx.collection.LongSparseArray
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getLongValue
import com.simplemobiletools.commons.extensions.getStringValue
import org.joda.time.DateTime

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
    private val COL_PARENT_EVENT_ID = "event_parent_id"

    private val mDb = writableDatabase

    companion object {
        private const val DB_VERSION = 1
        const val DB_NAME = "events_old.db"
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun getEvents(fromTS: Int, toTS: Int, eventId: Long = -1L, applyTypeFilter: Boolean = true, callback: (events: ArrayList<Event>) -> Unit) {
        Thread {
            getEventsInBackground(fromTS, toTS, eventId, applyTypeFilter, callback)
        }.start()
    }

    fun getEventsInBackground(fromTS: Int, toTS: Int, eventId: Long = -1L, applyTypeFilter: Boolean, callback: (events: ArrayList<Event>) -> Unit) {
        var events = ArrayList<Event>()

        //var selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS NULL AND $COL_START_TS != 0"
        var selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_START_TS != 0"
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

        //events.addAll(getRepeatableEventsFor(fromTS, toTS, eventId, applyTypeFilter))

        events.addAll(getAllDayEvents(fromTS, eventId, applyTypeFilter))

        events = events
                .asSequence()
                .distinct()
                .filterNot { context.eventsHelper.getEventRepetitionIgnoredOccurrences(it).contains(Formatter.getDayCodeFromTS(it.startTS)) }
                .toMutableList() as ArrayList<Event>
        callback(events)
    }

    fun getRepeatableEventsFor(fromTS: Int, toTS: Int, eventId: Long = -1L, applyTypeFilter: Boolean = false): List<Event> {
        val newEvents = ArrayList<Event>()
        //var selection = "$COL_REPEAT_INTERVAL != 0 AND $COL_START_TS <= $toTS AND $COL_START_TS != 0"
        var selection = "$COL_START_TS <= $toTS AND $COL_START_TS != 0"
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

    private fun getEventsRepeatingXTimes(fromTS: Int, toTS: Int, startTimes: LongSparseArray<Int>, event: Event): ArrayList<Event> {
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

    fun getRunningEvents(): List<Event> {
        val events = ArrayList<Event>()
        val ts = getNowSeconds()
        //val selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_REPEAT_INTERVAL IS NULL AND $COL_START_TS != 0"
        val selection = "$COL_START_TS <= ? AND $COL_END_TS >= ? AND $COL_START_TS != 0"
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

    fun getEventsAtReboot(): List<Event> {
        //val selection = "$COL_REMINDER_MINUTES != -1 AND ($COL_START_TS > ? OR $COL_REPEAT_INTERVAL != 0) AND $COL_START_TS != 0"
        val selection = "$COL_REMINDER_MINUTES != -1 AND ($COL_START_TS > ?) AND $COL_START_TS != 0"
        val selectionArgs = arrayOf(DateTime.now().seconds().toString())
        val cursor = getEventsCursor(selection, selectionArgs)
        return fillEvents(cursor)
    }

    fun getEventsToExport(includePast: Boolean): ArrayList<Event> {
        val currTime = getNowSeconds().toString()
        var events = ArrayList<Event>()

        // non repeating events
        val cursor = if (includePast) {
            getEventsCursor()
        } else {
            val selection = "$COL_END_TS > ?"
            val selectionArgs = arrayOf(currTime)
            getEventsCursor(selection, selectionArgs)
        }
        events.addAll(fillEvents(cursor))

        // repeating events
        /*if (!includePast) {
            val selection = "$COL_REPEAT_INTERVAL != 0 AND ($COL_REPEAT_LIMIT == 0 OR $COL_REPEAT_LIMIT > ?)"
            val selectionArgs = arrayOf(currTime)
            cursor = getEventsCursor(selection, selectionArgs)
            events.addAll(fillEvents(cursor))
        }*/

        events = events.distinctBy { it.id } as ArrayList<Event>
        return events
    }

    private fun getEventsCursor(selection: String = "", selectionArgs: Array<String>? = null): Cursor? {
        return mDb.query(MAIN_TABLE_NAME, allColumns, selection, selectionArgs, null, null, COL_START_TS)
    }

    private val allColumns: Array<String>
        get() = arrayOf("$MAIN_TABLE_NAME.$COL_ID", COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION, COL_REMINDER_MINUTES, COL_REMINDER_MINUTES_2,
                COL_REMINDER_MINUTES_3, COL_IMPORT_ID, COL_FLAGS, COL_EVENT_TYPE, COL_LAST_UPDATED, COL_EVENT_SOURCE, COL_LOCATION)

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
                    val repeatInterval = 0
                    var repeatRule = 0
                    val repeatLimit = 0
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
}
