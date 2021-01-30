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
    @Query("SELECT * FROM events")
    fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE event_type IN (:eventTypeIds)")
    fun getAllEventsWithTypes(eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventWithId(id: Long): Event?

    @Query("SELECT * FROM events WHERE import_id = :importId")
    fun getEventWithImportId(importId: String): Event?

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND end_ts >= :fromTS AND repeat_interval = 0")
    fun getOneTimeEventsFromTo(toTS: Long, fromTS: Long): List<Event>

    @Query("SELECT * FROM events WHERE id = :id AND start_ts <= :toTS AND end_ts >= :fromTS AND repeat_interval = 0")
    fun getOneTimeEventFromToWithId(id: Long, toTS: Long, fromTS: Long): List<Event>

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND end_ts >= :fromTS AND start_ts != 0 AND repeat_interval = 0 AND event_type IN (:eventTypeIds)")
    fun getOneTimeEventsFromToWithTypes(toTS: Long, fromTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE end_ts > :toTS AND repeat_interval = 0 AND event_type IN (:eventTypeIds)")
    fun getOneTimeFutureEventsWithTypes(toTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND repeat_interval != 0")
    fun getRepeatableEventsFromToWithTypes(toTS: Long): List<Event>

    @Query("SELECT * FROM events WHERE id = :id AND start_ts <= :toTS AND repeat_interval != 0")
    fun getRepeatableEventFromToWithId(id: Long, toTS: Long): List<Event>

    @Query("SELECT * FROM events WHERE start_ts <= :toTS AND start_ts != 0 AND repeat_interval != 0 AND event_type IN (:eventTypeIds)")
    fun getRepeatableEventsFromToWithTypes(toTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE repeat_interval != 0 AND (repeat_limit == 0 OR repeat_limit > :currTS) AND event_type IN (:eventTypeIds)")
    fun getRepeatableFutureEventsWithTypes(currTS: Long, eventTypeIds: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE id IN (:ids) AND import_id != \"\"")
    fun getEventsByIdsWithImportIds(ids: List<Long>): List<Event>

    @Query("SELECT * FROM events WHERE title LIKE :searchQuery OR location LIKE :searchQuery OR description LIKE :searchQuery")
    fun getEventsForSearch(searchQuery: String): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_BIRTHDAY\'")
    fun getBirthdays(): List<Event>

    @Query("SELECT * FROM events WHERE source = \'$SOURCE_CONTACT_ANNIVERSARY\'")
    fun getAnniversaries(): List<Event>

    @Query("SELECT * FROM events WHERE import_id != \"\"")
    fun getEventsWithImportIds(): List<Event>

    @Query("SELECT * FROM events WHERE source = :source")
    fun getEventsFromCalDAVCalendar(source: String): List<Event>

    @Query("SELECT * FROM events WHERE id IN (:ids)")
    fun getEventsWithIds(ids: List<Long>): List<Event>

    //val selection = "$COL_REMINDER_MINUTES != -1 AND ($COL_START_TS > ? OR $COL_REPEAT_INTERVAL != 0) AND $COL_START_TS != 0"
    @Query("SELECT * FROM events WHERE reminder_1_minutes != -1 AND (start_ts > :currentTS OR repeat_interval != 0) AND start_ts != 0")
    fun getEventsAtReboot(currentTS: Long): List<Event>

    @Query("SELECT id FROM events")
    fun getEventIds(): List<Long>

    @Query("SELECT id FROM events WHERE import_id = :importId")
    fun getEventIdWithImportId(importId: String): Long?

    @Query("SELECT id FROM events WHERE import_id LIKE :importId")
    fun getEventIdWithLastImportId(importId: String): Long?

    @Query("SELECT id FROM events WHERE event_type = :eventTypeId")
    fun getEventIdsByEventType(eventTypeId: Long): List<Long>

    @Query("SELECT id FROM events WHERE event_type IN (:eventTypeIds)")
    fun getEventIdsByEventType(eventTypeIds: List<Long>): List<Long>

    @Query("SELECT id FROM events WHERE parent_id IN (:parentIds)")
    fun getEventIdsWithParentIds(parentIds: List<Long>): List<Long>

    @Query("SELECT id FROM events WHERE source = :source AND import_id != \"\"")
    fun getCalDAVCalendarEvents(source: String): List<Long>

    @Query("UPDATE events SET event_type = $REGULAR_EVENT_TYPE_ID WHERE event_type = :eventTypeId")
    fun resetEventsWithType(eventTypeId: Long)

    @Query("UPDATE events SET import_id = :importId, source = :source WHERE id = :id")
    fun updateEventImportIdAndSource(importId: String, source: String, id: Long)

    @Query("UPDATE events SET repeat_limit = :repeatLimit WHERE id = :id")
    fun updateEventRepetitionLimit(repeatLimit: Long, id: Long)

    @Query("UPDATE events SET repetition_exceptions = :repetitionExceptions WHERE id = :id")
    fun updateEventRepetitionExceptions(repetitionExceptions: String, id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(event: Event): Long

    @Query("DELETE FROM events WHERE id IN (:ids)")
    fun deleteEvents(ids: List<Long>)

    @Query("DELETE FROM events WHERE source = :source AND import_id = :importId")
    fun deleteBirthdayAnniversary(source: String, importId: String): Int
}
