package com.simplemobiletools.calendar.pro.models

import androidx.room.*

@Entity(tableName = "event_repetition_exceptions", indices = [(Index(value = ["id"], unique = true))],
        foreignKeys = [ForeignKey(entity = Event::class, onDelete = ForeignKey.CASCADE, parentColumns = ["id"], childColumns = ["event_id"])])
data class EventRepetitionException(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "occurrence_daycode") val daycode: String,
        @ColumnInfo(name = "event_id") val eventId: Long)
