package com.simplemobiletools.calendar.pro.activities

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FriendDao {
    @Insert
    fun insert(friend: Friend)

    @Query("SELECT * FROM friends")
    fun getAllFriends(): List<Friend>
}
