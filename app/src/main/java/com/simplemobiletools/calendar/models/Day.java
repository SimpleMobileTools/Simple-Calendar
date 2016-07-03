package com.simplemobiletools.calendar.models;

public class Day {
    private final int mValue;
    private final boolean mIsThisMonth;
    private final boolean mIsToday;
    private final boolean mHasNote;
    private final String mCode;

    public Day(int value, boolean isThisMonth, boolean isToday, String code, boolean hasNote) {
        mValue = value;
        mIsThisMonth = isThisMonth;
        mIsToday = isToday;
        mCode = code;
        mHasNote = hasNote;
    }

    public int getValue() {
        return mValue;
    }

    public boolean getIsThisMonth() {
        return mIsThisMonth;
    }

    public boolean getIsToday() {
        return mIsToday;
    }

    public String getCode() {
        return mCode;
    }

    public boolean getHasNote() {
        return mHasNote;
    }
}
