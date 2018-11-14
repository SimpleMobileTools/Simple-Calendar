package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.calendar.pro.helpers.SOURCE_CONTACT_ANNIVERSARY
import com.simplemobiletools.calendar.pro.helpers.SOURCE_CONTACT_BIRTHDAY
import com.simplemobiletools.calendar.pro.models.Event

@Dao
interface EventsDao {
    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventWithId(id: Long): Event?

    @Query("SELECT * FROM events WHERE id IN (:ids)")
    fun getEventsWithIds(ids: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_BIRTHDAY\'")
    fun getBirthdays(): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_ANNIVERSARY\'")
    fun getAnniversaries(): List<Event>

    @Query("SELECT * FROM events WHERE import_id != \"\"")
    fun getEventsWithImportIds(): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(event: Event): Long
}
