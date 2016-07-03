package com.simplemobiletools.calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseBooleanArray;

public class Config {
    private SharedPreferences mPrefs;

    public static Config newInstance(Context context) {
        return new Config(context);
    }

    public Config(Context context) {
        mPrefs = context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE);
    }

    public boolean getIsFirstRun() {
        return mPrefs.getBoolean(Constants.IS_FIRST_RUN, true);
    }

    public void setIsFirstRun(boolean firstRun) {
        mPrefs.edit().putBoolean(Constants.IS_FIRST_RUN, firstRun).apply();
    }

    public SparseBooleanArray getEvents() {
        final String json = mPrefs.getString(Constants.EVENTS, "{}");
        return Utils.deserializeJson(json);
    }

    public void setEvents(SparseBooleanArray events) {
        mPrefs.edit().putString(Constants.EVENTS, Utils.serializeArray(events)).apply();
    }
}
