package com.simplemobiletools.calendar.pro.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.simplemobiletools.calendar.pro.models.Task

@Dao
interface TasksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(task: Task): Long

    @Query("SELECT * FROM tasks WHERE task_id = :id AND start_ts = :startTs")
    fun getTaskWithIdAndTs(id: Long, startTs: Long): Task?

    @Query("DELETE FROM tasks WHERE task_id = :id AND start_ts = :startTs")
    fun deleteTaskWithIdAndTs(id: Long, startTs: Long)
}
