package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.*
import com.simplemobiletools.calendar.pro.models.EventType

@Dao
interface EventTypesDao {
    @Query("SELECT * FROM event_types ORDER BY title ASC")
    fun getEventTypes(): List<EventType>

    @Query("SELECT * FROM event_types WHERE id = :id")
    fun getEventTypeWithId(id: Long): EventType?

    @Query("SELECT id FROM event_types WHERE title = :title COLLATE NOCASE")
    fun getEventTypeIdWithTitle(title: String): Long?

    @Query("SELECT * FROM event_types WHERE caldav_calendar_id = :calendarId")
    fun getEventTypeWithCalDAVCalendarId(calendarId: Int): EventType?

    @Query("DELETE FROM event_types WHERE caldav_calendar_id IN (:ids)")
    fun deleteEventTypesWithCalendarId(ids: List<Int>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(eventType: EventType): Long

    @Delete
    fun deleteEventTypes(eventTypes: List<EventType>)
}
