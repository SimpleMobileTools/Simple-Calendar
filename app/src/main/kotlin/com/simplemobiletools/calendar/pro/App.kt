package com.simplemobiletools.calendar.pro

import androidx.multidex.MultiDexApplication
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.simplemobiletools.calendar.pro.ouragenda.database.AppDatabase
import com.simplemobiletools.commons.extensions.checkUseEnglish


class App : MultiDexApplication() {

    private var database: AppDatabase? = null
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
        FirebaseApp.initializeApp(this)!!
        database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "app_database"
            ).build()
    }

    //fun getDatabase(): AppDatabase = database!!
}
