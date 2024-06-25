package com.simplemobiletools.calendar.pro.ouragenda.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.simplemobiletools.calendar.pro.ouragenda.model.Friend

@Database(entities = [Friend::class], version = 2)  // Augmentez la version ici
abstract class AppDatabase: RoomDatabase() {
    abstract fun friendDao(): FriendDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Logique de migration ici
                // Par exemple, si vous avez ajouté une nouvelle colonne à la table "Friend":
                // database.execSQL("ALTER TABLE Friend ADD COLUMN new_column INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app-database-name"
                )
                    .addMigrations(MIGRATION_1_2)  // Ajoutez la migration ici
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
