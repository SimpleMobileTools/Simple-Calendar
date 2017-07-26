package com.simplemobiletools.calendar.helpers

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// database for storing operations performed on google events locally, while the user was offline
class GoogleSyncQueueDB private constructor(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val MAIN_TABLE_NAME = "operations"
    private val COL_ID = "id"
    private val COL_EVENT_ID = "event_id"
    private val COL_OPERATION = "operation"

    private val OPERATION_INSERT = 1
    private val OPERATION_UPDATE = 2
    private val OPERATION_DELETE = 3

    private val mDb: SQLiteDatabase = writableDatabase

    companion object {
        private val DB_VERSION = 1
        private val DB_NAME = "googlesyncqueue.db"
        var dbInstance: GoogleSyncQueueDB? = null

        fun newInstance(context: Context): GoogleSyncQueueDB {
            if (dbInstance == null)
                dbInstance = GoogleSyncQueueDB(context)

            return dbInstance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $MAIN_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY, $COL_EVENT_ID INTEGER, $COL_OPERATION INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }
}
