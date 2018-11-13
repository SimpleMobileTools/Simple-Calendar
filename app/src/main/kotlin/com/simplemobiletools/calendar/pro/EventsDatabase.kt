package com.simplemobiletools.calendar.pro

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.simplemobiletools.calendar.pro.interfaces.EventRepetitionExceptionsDao
import com.simplemobiletools.calendar.pro.interfaces.EventRepetitionsDao
import com.simplemobiletools.calendar.pro.interfaces.EventTypesDao
import com.simplemobiletools.calendar.pro.interfaces.EventsDao
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventRepetition
import com.simplemobiletools.calendar.pro.models.EventRepetitionException
import com.simplemobiletools.calendar.pro.models.EventType

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
    }
}
