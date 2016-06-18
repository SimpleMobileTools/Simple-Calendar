package com.simplemobiletools.calendar;

import org.joda.time.DateTime;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class CalendarImpl {
    public static final int DAYS_CNT = 42;
    private static final String DATE_PATTERN = "ddMMYYYY";
    private static final String YEAR_PATTERN = "YYYY";

    private final Calendar mCallback;
    private final String mToday;
    private DateTime mTargetDate;

    public CalendarImpl(Calendar callback) {
        this.mCallback = callback;
        mToday = new DateTime().toString(DATE_PATTERN);
    }

    public void updateCalendar(DateTime targetDate) {
        this.mTargetDate = targetDate;
        getMonthName();
        getDays(targetDate);
    }

    public void getPrevMonth() {
        updateCalendar(mTargetDate.minusMonths(1));
    }

    public void getNextMonth() {
        updateCalendar(mTargetDate.plusMonths(1));
    }

    private void getDays(DateTime targetDate) {
        final List<Day> days = new ArrayList<>(DAYS_CNT);

        final int currMonthDays = targetDate.dayOfMonth().getMaximumValue();
        final int firstDayIndex = targetDate.withDayOfMonth(1).getDayOfWeek() - 1;
        final int prevMonthDays = targetDate.minusMonths(1).dayOfMonth().getMaximumValue();

        boolean isThisMonth = false;
        boolean isToday;
        int value = prevMonthDays - firstDayIndex + 1;

        for (int i = 0; i < DAYS_CNT; i++) {
            if (i < firstDayIndex) {
                isThisMonth = false;
            } else if (i == firstDayIndex) {
                value = 1;
                isThisMonth = true;
            } else if (value == currMonthDays + 1) {
                value = 1;
                isThisMonth = false;
            }

            isToday = isThisMonth && isToday(targetDate, value);

            final Day day = new Day(value, isThisMonth, isToday);
            days.add(day);
            value++;
        }

        mCallback.updateCalendar(getMonthName(), days);
    }

    private boolean isToday(DateTime targetDate, int curDayInMonth) {
        return targetDate.withDayOfMonth(curDayInMonth).toString(DATE_PATTERN).equals(mToday);
    }

    private String getMonthName() {
        final String[] months = new DateFormatSymbols().getMonths();
        String month = (months[mTargetDate.getMonthOfYear() - 1]);
        final String targetYear = mTargetDate.toString(YEAR_PATTERN);
        if (!targetYear.equals(new DateTime().toString(YEAR_PATTERN))) {
            month += " " + targetYear;
        }
        return month;
    }

    public DateTime getTargetDate() {
        return mTargetDate;
    }
}
