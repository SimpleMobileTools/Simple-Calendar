package com.simplemobiletools.calendar.pro.ouragenda.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.simplemobiletools.calendar.pro.ouragenda.model.Friend

@Dao
interface FriendDao {
    @Insert
    fun insert(friend: Friend)

    @Query("SELECT * FROM friends")
    fun getAllFriends(): List<Friend>
}
