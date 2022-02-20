package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.calendar.pro.models.Task

@Dao
interface TasksDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(task: Task): Long
}
