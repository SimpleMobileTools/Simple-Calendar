package com.simplemobiletools.calendar.pro.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.DBHelper
import com.simplemobiletools.calendar.pro.interfaces.EventRepetitionExceptionsDao
import com.simplemobiletools.calendar.pro.interfaces.EventRepetitionsDao
import com.simplemobiletools.calendar.pro.interfaces.EventTypesDao
import com.simplemobiletools.calendar.pro.interfaces.EventsDao
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventRepetition
import com.simplemobiletools.calendar.pro.models.EventRepetitionException
import com.simplemobiletools.calendar.pro.models.EventType
import java.util.concurrent.Executors

@Database(entities = [Event::class, EventType::class, EventRepetition::class, EventRepetitionException::class], version = 1)
abstract class EventsDatabase : RoomDatabase() {

    abstract fun EventsDao(): EventsDao

    abstract fun EventTypesDao(): EventTypesDao

    abstract fun EventRepetitionsDao(): EventRepetitionsDao

    abstract fun EventRepetitionExceptionsDao(): EventRepetitionExceptionsDao

    companion object {
        private var db: EventsDatabase? = null

        fun getInstance(context: Context): EventsDatabase {
            if (db == null) {
                synchronized(EventsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, EventsDatabase::class.java, "events.db")
                                .addCallback(object : Callback() {
                                    override fun onCreate(db: SupportSQLiteDatabase) {
                                        super.onCreate(db)
                                        insertRegularEventType(context)
                                    }
                                })
                                .build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }

        private fun insertRegularEventType(context: Context) {
            Executors.newSingleThreadScheduledExecutor().execute {
                val regularEvent = context.resources.getString(R.string.regular_event)
                val eventType = EventType(DBHelper.REGULAR_EVENT_TYPE_ID, regularEvent, context.config.primaryColor)
                db!!.EventTypesDao().insertOrUpdate(eventType)
                context.config.addDisplayEventType(DBHelper.REGULAR_EVENT_TYPE_ID.toString())
            }
        }
    }
}
