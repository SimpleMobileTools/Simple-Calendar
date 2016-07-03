package com.simplemobiletools.calendar;

import com.simplemobiletools.calendar.models.Day;

import java.util.List;

public interface Calendar {
    void updateCalendar(String month, List<Day> days);
}
