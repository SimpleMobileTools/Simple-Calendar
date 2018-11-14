package com.simplemobiletools.calendar.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "event_repetition_exceptions", indices = [(Index(value = ["id"], unique = true))])
data class EventRepetitionException(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "occurrence_daycode") val daycode: String,
        @ColumnInfo(name = "event_id") val eventId: Long)
