package com.simplemobiletools.calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.simplemobiletools.calendar.models.Event;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static SQLiteDatabase mDb;
    private static DBOperationsListener mCallback;

    private static final String DB_NAME = "events.db";
    private static final int DB_VERSION = 2;

    private static final String TABLE_NAME = "events";
    private static final String COL_ID = "id";
    private static final String COL_START_TS = "start_ts";
    private static final String COL_END_TS = "end_ts";
    private static final String COL_TITLE = "title";
    private static final String COL_DESCRIPTION = "description";
    private static final String COL_REMINDER_MINUTES = "reminder_minutes";

    public static DBHelper newInstance(Context context, DBOperationsListener callback) {
        mCallback = callback;
        return new DBHelper(context);
    }

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mDb = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_START_TS + " INTEGER," +
                COL_END_TS + " INTEGER," +
                COL_TITLE + " TEXT," +
                COL_DESCRIPTION + " TEXT," +
                COL_REMINDER_MINUTES + " INTEGER" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_REMINDER_MINUTES + " INTEGER DEFAULT -1");
        }
    }

    public void insert(Event event) {
        final ContentValues values = fillContentValues(event);
        long id = mDb.insert(TABLE_NAME, null, values);
        event.setId((int) id);

        if (mCallback != null)
            mCallback.eventInserted(event);
    }

    public void update(Event event) {
        final ContentValues values = fillContentValues(event);
        final String selection = COL_ID + " = ?";
        final String[] selectionArgs = {String.valueOf(event.getId())};
        mDb.update(TABLE_NAME, values, selection, selectionArgs);

        if (mCallback != null)
            mCallback.eventUpdated(event);
    }

    private ContentValues fillContentValues(Event event) {
        final ContentValues values = new ContentValues();
        values.put(COL_START_TS, event.getStartTS());
        values.put(COL_END_TS, event.getEndTS());
        values.put(COL_TITLE, event.getTitle());
        values.put(COL_DESCRIPTION, event.getDescription());
        values.put(COL_REMINDER_MINUTES, event.getReminderMinutes());
        return values;
    }

    public void deleteEvents(String[] ids) {
        final String selection = COL_ID + " IN (?)";
        mDb.delete(TABLE_NAME, selection, ids);

        if (mCallback != null)
            mCallback.eventsDeleted(ids.length);
    }

    public Event getEvent(int id) {
        final String[] projection = {COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION, COL_REMINDER_MINUTES};
        final String selection = COL_ID + " = ?";
        final String[] selectionArgs = {String.valueOf(id)};
        final Cursor cursor = mDb.query(TABLE_NAME, projection, selection, selectionArgs, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int startTS = cursor.getInt(cursor.getColumnIndex(COL_START_TS));
                final int endTS = cursor.getInt(cursor.getColumnIndex(COL_END_TS));
                final String title = cursor.getString(cursor.getColumnIndex(COL_TITLE));
                final String description = cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION));
                final int reminderMinutes = cursor.getInt(cursor.getColumnIndex(COL_REMINDER_MINUTES));
                cursor.close();
                return new Event(id, startTS, endTS, title, description, reminderMinutes);
            }
        }
        return null;
    }

    public void getEvents(int fromTS, int toTS) {
        List<Event> events = new ArrayList<>();
        final String[] projection = getAllColumns();
        final String selection = COL_START_TS + " <= ? AND " + COL_END_TS + " >= ?";
        final String[] selectionArgs = {String.valueOf(toTS), String.valueOf(fromTS)};
        final Cursor cursor = mDb.query(TABLE_NAME, projection, selection, selectionArgs, null, null, COL_START_TS);
        if (cursor != null) {
            events = fillEvents(cursor);
        }

        if (mCallback != null)
            mCallback.gotEvents(events);
    }

    public List<Event> getEventsAtReboot() {
        List<Event> events = new ArrayList<>();
        final String[] projection = getAllColumns();
        final String selection = COL_START_TS + " > ? AND " + COL_REMINDER_MINUTES + " != ?";
        final String[] selectionArgs = {String.valueOf(DateTime.now().getMillis() / 1000), "-1"};
        final Cursor cursor = mDb.query(TABLE_NAME, projection, selection, selectionArgs, null, null, null);
        if (cursor != null) {
            events = fillEvents(cursor);
        }
        return events;
    }

    private String[] getAllColumns() {
        return new String[]{COL_ID, COL_START_TS, COL_END_TS, COL_TITLE, COL_DESCRIPTION, COL_REMINDER_MINUTES};
    }

    private List<Event> fillEvents(Cursor cursor) {
        final List<Event> events = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                final int id = cursor.getInt(cursor.getColumnIndex(COL_ID));
                final int startTS = cursor.getInt(cursor.getColumnIndex(COL_START_TS));
                final int endTS = cursor.getInt(cursor.getColumnIndex(COL_END_TS));
                final String title = cursor.getString(cursor.getColumnIndex(COL_TITLE));
                final String description = cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION));
                final int reminderMinutes = cursor.getInt(cursor.getColumnIndex(COL_REMINDER_MINUTES));
                events.add(new Event(id, startTS, endTS, title, description, reminderMinutes));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return events;
    }

    public interface DBOperationsListener {
        void eventInserted(Event event);

        void eventUpdated(Event event);

        void eventsDeleted(int cnt);

        void gotEvents(List<Event> events);
    }
}
