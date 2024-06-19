package com.simplemobiletools.calendar.pro.activities

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [Friend::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build().also { instance = it }
            }
    }
}
