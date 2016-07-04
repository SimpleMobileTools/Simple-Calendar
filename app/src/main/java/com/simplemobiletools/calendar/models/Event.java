package com.simplemobiletools.calendar.models;

import java.io.Serializable;

public class Event implements Serializable {
    private static final long serialVersionUID = -32456795132354616L;
    private final int mId;
    private int mStartTS;
    private int mEndTS;
    private String mTitle;
    private String mDescription;

    public Event() {
        mId = 0;
        mStartTS = 0;
        mEndTS = 0;
        mTitle = "";
        mDescription = "";
    }

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

    public void setStartTS(int startTS) {
        mStartTS = startTS;
    }

    public int getEndTS() {
        return mEndTS;
    }

    public void setEndTS(int endTS) {
        mEndTS = endTS;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
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
