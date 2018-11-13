package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.calendar.pro.models.EventType

@Dao
interface EventTypesDao {
    @Query("SELECT * FROM event_types ORDER BY title ASC")
    fun getEventTypes(): List<EventType>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(eventType: EventType): Long
}
