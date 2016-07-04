package com.simplemobiletools.calendar.models;

public class Event {
    private final int mId;
    private final int mStartTS;
    private final int mEndTS;
    private final String mTitle;
    private final String mDescription;

    public Event(int id, int startTS, int endTS, String title, String description) {
        mId = id;
        mStartTS = startTS;
        mEndTS = endTS;
        mTitle = title;
        mDescription = description;
    }

    public int getId() {
        return mId;
    }

    public int getStartTS() {
        return mStartTS;
    }

    public int getEndTS() {
        return mEndTS;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    @Override
    public String toString() {
        return "Event {" +
                "id=" + getId() +
                ", startTS=" + getStartTS() +
                ", endTS=" + getEndTS() +
                ", title=" + getTitle() +
                ", description=" + getDescription() +
                "}";
    }
}
