package com.simplemobiletools.calendar;

public class Day {
    private final int mValue;
    private final boolean mIsThisMonth;
    private final boolean mIsToday;

    public Day(int value, boolean isThisMonth, boolean isToday) {
        this.mValue = value;
        this.mIsThisMonth = isThisMonth;
        this.mIsToday = isToday;
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
}
