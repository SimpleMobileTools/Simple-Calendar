package com.simplemobiletools.calendar.pro.models

import androidx.room.*

@Entity(tableName = "event_repetitions", indices = [(Index(value = ["id"], unique = true))],
        foreignKeys = [ForeignKey(entity = Event::class, onDelete = ForeignKey.CASCADE, parentColumns = ["id"], childColumns = ["event_id"])])
data class EventRepetition(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "event_id") val eventId: Long,
        @ColumnInfo(name = "repeat_interval") val repeatInterval: Int,
        @ColumnInfo(name = "repeat_rule") val repeatRule: Int,
        @ColumnInfo(name = "repeat_limit") val repeatLimit: Long)
