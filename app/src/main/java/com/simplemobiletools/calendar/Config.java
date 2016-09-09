package com.simplemobiletools.calendar;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

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

    public boolean getIsDarkTheme() {
        return mPrefs.getBoolean(Constants.IS_DARK_THEME, false);
    }

    public void setIsDarkTheme(boolean isDarkTheme) {
        mPrefs.edit().putBoolean(Constants.IS_DARK_THEME, isDarkTheme).apply();
    }

    public boolean getIsSundayFirst() {
        boolean isSundayFirst = java.util.Calendar.getInstance(Locale.getDefault()).getFirstDayOfWeek() == java.util.Calendar.SUNDAY;
        return mPrefs.getBoolean(Constants.SUNDAY_FIRST, isSundayFirst);
    }

    public void setIsSundayFirst(boolean sundayFirst) {
        mPrefs.edit().putBoolean(Constants.SUNDAY_FIRST, sundayFirst).apply();
    }
}
