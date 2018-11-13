package com.simplemobiletools.calendar.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "event_repetitions", indices = [(Index(value = ["id"], unique = true))])
data class EventRepetition(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "event_id") val eventId: Int,
        @ColumnInfo(name = "repeat_interval") val repeatInterval: Int,
        @ColumnInfo(name = "repeat_rule") val repeatRule: Int,
        @ColumnInfo(name = "repeat_limit") val repeatLimit: Int)
