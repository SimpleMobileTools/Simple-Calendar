package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.calendar.pro.models.EventRepetitionException

@Dao
interface EventRepetitionExceptionsDao {
    @Query("SELECT occurrence_daycode FROM event_repetition_exceptions WHERE event_id = :id")
    fun getEventRepetitionExceptions(id: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(eventRepetitionException: EventRepetitionException)
}
