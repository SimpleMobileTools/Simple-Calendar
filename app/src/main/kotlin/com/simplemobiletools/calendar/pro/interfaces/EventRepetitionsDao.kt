package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.calendar.pro.models.EventRepetition

@Dao
interface EventRepetitionsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(eventRepetition: EventRepetition)

    @Query("DELETE FROM event_repetitions WHERE event_id = :eventId")
    fun deleteEventRepetitionsOfEvent(eventId: Long)

    @Query("UPDATE event_repetitions SET repeat_limit = :repeatLimit WHERE event_id = :eventId")
    fun updateEventRepetitionLimit(repeatLimit: Int, eventId: Long)
}
