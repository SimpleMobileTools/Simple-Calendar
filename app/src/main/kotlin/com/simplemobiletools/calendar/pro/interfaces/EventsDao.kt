package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.calendar.pro.helpers.REGULAR_EVENT_TYPE_ID
import com.simplemobiletools.calendar.pro.helpers.SOURCE_CONTACT_ANNIVERSARY
import com.simplemobiletools.calendar.pro.helpers.SOURCE_CONTACT_BIRTHDAY
import com.simplemobiletools.calendar.pro.models.Event

@Dao
interface EventsDao {
    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventWithId(id: Long): Event?

    @Query("SELECT id FROM events")
    fun getEventIds(): List<Long>

    @Query("SELECT id FROM events WHERE import_id = :importId")
    fun getEventIdWithImportId(importId: String): Long?

    @Query("SELECT id FROM events WHERE import_id LIKE :importId")
    fun getEventIdWithLastImportId(importId: String): Long?

    @Query("SELECT * FROM events WHERE id IN (:ids) AND import_id != \"\"")
    fun getEventsByIdsWithImportIds(ids: List<Long>): List<Event>

    @Query("SELECT id FROM events WHERE event_type = :eventTypeId")
    fun getEventIdsByEventType(eventTypeId: Long): List<Long>

    @Query("SELECT id FROM events WHERE event_type IN (:eventTypeIds)")
    fun getEventIdsByEventType(eventTypeIds: List<Long>): List<Long>

    @Query("SELECT * FROM events WHERE id IN (:ids)")
    fun getEventsWithIds(ids: List<Long>): List<Event>

    @Query("SELECT id FROM events WHERE parent_id IN (:parentIds)")
    fun getEventIdsWithParentIds(parentIds: List<Long>): List<Long>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_BIRTHDAY\'")
    fun getBirthdays(): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_ANNIVERSARY\'")
    fun getAnniversaries(): List<Event>

    @Query("SELECT * FROM events WHERE import_id != \"\"")
    fun getEventsWithImportIds(): List<Event>

    @Query("SELECT * FROM events WHERE source = :source")
    fun getEventsFromCalDAVCalendar(source: String): List<Event>

    @Query("SELECT id FROM events WHERE source = :source AND import_id != \"\"")
    fun getCalDAVCalendarEvents(source: String): List<Long>

    @Query("SELECT * FROM events WHERE title LIKE :searchQuery OR location LIKE :searchQuery OR description LIKE :searchQuery")
    fun getEventsForSearch(searchQuery: String): List<Event>

    @Query("UPDATE events SET event_type = $REGULAR_EVENT_TYPE_ID WHERE event_type = :eventTypeId")
    fun resetEventsWithType(eventTypeId: Long)

    @Query("UPDATE events SET import_id = :importId AND source = :source WHERE id = :id")
    fun updateEventImportIdAndSource(importId: String, source: String, id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(event: Event): Long

    @Query("DELETE FROM events WHERE id IN (:ids)")
    fun deleteEvents(ids: List<Long>)
}
