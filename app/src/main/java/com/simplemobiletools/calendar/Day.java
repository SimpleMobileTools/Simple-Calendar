package com.simplemobiletools.calendar;

public class Day {
    private final int value;
    private final boolean isThisMonth;
    private final boolean isToday;

    public Day(int value, boolean isThisMonth, boolean isToday) {
        this.value = value;
        this.isThisMonth = isThisMonth;
        this.isToday = isToday;
    }

    public int getValue() {
        return value;
    }

    public boolean getIsThisMonth() {
        return isThisMonth;
    }

    public boolean getIsToday() {
        return isToday;
    }
}
