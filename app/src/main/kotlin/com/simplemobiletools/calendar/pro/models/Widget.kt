package com.simplemobiletools.calendar.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "widgets", indices = [(Index(value = ["widget_id"], unique = true))])
data class Widget(
        @PrimaryKey(autoGenerate = true) var id: Long?,
        @ColumnInfo(name = "widget_id") var widgetId: Int,
        @ColumnInfo(name = "period") var period: Int)
