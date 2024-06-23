package com.simplemobiletools.calendar.pro.ouragenda.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "friends", indices = [Index(value = ["name"], unique = true)])
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)
