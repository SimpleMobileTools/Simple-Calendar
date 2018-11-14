package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.calendar.pro.models.Event

@Dao
interface EventsDao {
    @Query("DELETE FROM events WHERE id IN (:ids)")
    fun getEventsWithIds(ids: List<Long>): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(event: Event): Long
}
