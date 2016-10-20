package com.simplemobiletools.calendar;

import com.simplemobiletools.calendar.models.Day;

import java.util.List;

public interface MonthlyCalendar {
    void updateMonthlyCalendar(String month, List<Day> days);
}
