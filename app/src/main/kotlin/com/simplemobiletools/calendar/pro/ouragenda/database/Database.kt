package com.simplemobiletools.calendar.pro.ouragenda.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.simplemobiletools.calendar.pro.ouragenda.model.Friend

@Database(entities = [Friend::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun friendDao(): FriendDao

}
