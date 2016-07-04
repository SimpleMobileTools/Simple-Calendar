package com.simplemobiletools.calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.simplemobiletools.calendar.models.Event;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static SQLiteDatabase mDb;
    private static String[] mProjection;

    private static final String DB_NAME = "events.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_NAME = "events";
    private static final String COL_ID = "id";
    private static final String COL_START_TS = "start_ts";
    private static final String COL_END_TS = "end_ts";
    private static final String COL_TITLE = "title";
    private static final String COL_DESCRIPTION = "description";

    public static DBHelper newInstance(Context context) {
        return new DBHelper(context);
    }

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mDb = getWritableDatabase();
        mProjection = new String[]{COL_ID, COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION};
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_START_TS + " INTEGER," +
                COL_END_TS + " INTEGER," +
                COL_TITLE + " TEXT," +
                COL_DESCRIPTION + " TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insert(Event event) {
        final ContentValues values = new ContentValues();
        values.put(COL_START_TS, event.getStartTS());
        values.put(COL_END_TS, event.getEndTS());
        values.put(COL_TITLE, event.getTitle());
        values.put(COL_DESCRIPTION, event.getDescription());
        mDb.insert(TABLE_NAME, null, values);
    }

    public List<Event> getEvents(int fromTS, int toTS) {
        List<Event> events = new ArrayList<>();
        final String selection = COL_START_TS + " >= ? AND " + COL_START_TS + " <= ?";
        final String[] selectionArgs = {String.valueOf(fromTS), String.valueOf(toTS)};
        final Cursor cursor = mDb.query(TABLE_NAME, mProjection, selection, selectionArgs, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    final int id = cursor.getInt(cursor.getColumnIndex(COL_ID));
                    final int startTS = cursor.getInt(cursor.getColumnIndex(COL_START_TS));
                    final int endTS = cursor.getInt(cursor.getColumnIndex(COL_END_TS));
                    final String title = cursor.getString(cursor.getColumnIndex(COL_TITLE));
                    final String description = cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION));
                    events.add(new Event(id, startTS, endTS, title, description));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return events;
    }
}
