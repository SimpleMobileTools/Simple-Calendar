package com.simplemobiletools.calendar.pro.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.simplemobiletools.calendar.pro.helpers.FLAG_TASK_COMPLETED

@Entity(tableName = "tasks", indices = [(Index(value = ["id", "task_id"], unique = true))])
data class Task(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "task_id") var task_id: Long,
    @ColumnInfo(name = "start_ts") var startTS: Long = 0L,
    @ColumnInfo(name = "flags") var flags: Int = 0,
) {
    fun isTaskCompleted() = flags and FLAG_TASK_COMPLETED != 0
}
