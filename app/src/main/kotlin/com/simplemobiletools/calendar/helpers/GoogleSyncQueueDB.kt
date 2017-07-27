package com.simplemobiletools.calendar.helpers

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.simplemobiletools.calendar.models.GoogleOperation
import com.simplemobiletools.commons.extensions.getIntValue
import com.simplemobiletools.commons.extensions.getStringValue

// database for storing operations performed on google events locally, while the user was offline
class GoogleSyncQueueDB private constructor(val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    private val OPERATIONS_TABLE_NAME = "operations"
    private val COL_ID = "id"
    private val COL_EVENT_ID = "event_id"
    private val COL_OPERATION = "operation"
    private val COL_IMPORT_ID = "import_id"

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
        db.execSQL("CREATE TABLE $OPERATIONS_TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_EVENT_ID INTEGER, $COL_OPERATION INTEGER, " +
                "$COL_IMPORT_ID TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    fun addOperation(eventId: Int, operation: Int, importId: String) {
        val hadInsertOperation = getOperationOf(eventId)?.operation?.equals(OPERATION_INSERT) == true
        if (operation == OPERATION_DELETE) {
            clearOperationsOf(eventId)
            if (hadInsertOperation) {
                return
            }
        }

        if (operation == OPERATION_UPDATE) {
            if (hadInsertOperation) {
                return
            }
        }

        val contentValues = ContentValues().apply {
            put(COL_EVENT_ID, eventId)
            put(COL_OPERATION, operation)
            put(COL_IMPORT_ID, importId)
        }
        mDb.insert(OPERATIONS_TABLE_NAME, null, contentValues)
    }

    fun getOperationOf(eventId: Int): GoogleOperation? {
        val selection = "$COL_EVENT_ID = $eventId"
        val projection = arrayOf(COL_OPERATION, COL_IMPORT_ID)
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(OPERATIONS_TABLE_NAME, projection, selection, null, null, null, null)
            if (cursor?.moveToFirst() == true) {
                val operation = cursor.getIntValue(COL_OPERATION)
                val importId = cursor.getStringValue(COL_IMPORT_ID)
                return GoogleOperation(eventId, operation, importId)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    fun getOperations(): ArrayList<GoogleOperation> {
        val operations = ArrayList<GoogleOperation>()
        val projection = arrayOf(COL_EVENT_ID, COL_OPERATION, COL_IMPORT_ID)
        var cursor: Cursor? = null
        try {
            cursor = mDb.query(OPERATIONS_TABLE_NAME, projection, null, null, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val eventId = cursor.getIntValue(COL_EVENT_ID)
                    val operation = cursor.getIntValue(COL_OPERATION)
                    val importId = cursor.getStringValue(COL_IMPORT_ID)
                    operations.add(GoogleOperation(eventId, operation, importId))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return operations
    }

    fun clearOperationsOf(eventId: Int) {
        val selection = "$COL_EVENT_ID = $eventId"
        mDb.delete(OPERATIONS_TABLE_NAME, selection, null)
    }

    fun clearQueue() {
        mDb.delete(OPERATIONS_TABLE_NAME, null, null)
    }
}
