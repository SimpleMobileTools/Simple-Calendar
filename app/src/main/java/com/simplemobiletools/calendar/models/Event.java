package com.simplemobiletools.calendar.models;

import java.io.Serializable;

public class Event implements Serializable {
    private static final long serialVersionUID = -32456795132354616L;
    private int mId;
    private int mStartTS;
    private int mEndTS;
    private String mTitle;
    private String mDescription;
    private int mReminderMinutes;

    public Event() {
        mId = 0;
        mStartTS = 0;
        mEndTS = 0;
        mTitle = "";
        mDescription = "";
        mReminderMinutes = -1;
    }

    public Event(int id, int startTS, int endTS, String title, String description, int reminerMinutes) {
        mId = id;
        mStartTS = startTS;
        mEndTS = endTS;
        mTitle = title;
        mDescription = description;
        mReminderMinutes = reminerMinutes;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
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

    public int getReminderMinutes() {
        return mReminderMinutes;
    }

    public void setReminderMinutes(int reminderMinutes) {
        mReminderMinutes = reminderMinutes;
    }

    @Override
    public String toString() {
        return "Event {" +
                "id=" + getId() +
                ", startTS=" + getStartTS() +
                ", endTS=" + getEndTS() +
                ", title=" + getTitle() +
                ", description=" + getDescription() +
                ", reminderMinutes=" + getReminderMinutes() +
                "}";
    }
}
