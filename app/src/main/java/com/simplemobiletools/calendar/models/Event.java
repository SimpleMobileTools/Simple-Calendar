package com.simplemobiletools.calendar.models;

public class Event {
    private final int mStartTS;
    private final int mEndTS;
    private final String mDescription;

    public Event(int startTS, int endTS, String description) {
        mStartTS = startTS;
        mEndTS = endTS;
        mDescription = description;
    }

    public int getStartTS() {
        return mStartTS;
    }

    public int getEndTS() {
        return mEndTS;
    }

    public String getDescription() {
        return mDescription;
    }
}
