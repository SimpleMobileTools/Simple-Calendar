package com.simplemobiletools.calendar;

import android.content.Context;
import android.util.SparseBooleanArray;

import com.simplemobiletools.calendar.models.Day;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class CalendarImpl {
    public static final int DAYS_CNT = 42;
    private static final String YEAR_PATTERN = "YYYY";

    private final Calendar mCallback;
    private final String mToday;
    private final Context mContext;
    private DateTime mTargetDate;
    private SparseBooleanArray mEvents;

    public CalendarImpl(Calendar callback, Context context) {
        this.mCallback = callback;
        mToday = new DateTime().toString(Constants.DATE_PATTERN);
        mContext = context;
        mEvents = Config.newInstance(context).getEvents();
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
        DateTime curDay = targetDate;

        for (int i = 0; i < DAYS_CNT; i++) {
            if (i < firstDayIndex) {
                isThisMonth = false;
                curDay = targetDate.minusMonths(1);
            } else if (i == firstDayIndex) {
                value = 1;
                isThisMonth = true;
                curDay = targetDate;
            } else if (value == currMonthDays + 1) {
                value = 1;
                isThisMonth = false;
                curDay = targetDate.plusMonths(1);
            }

            isToday = isThisMonth && isToday(targetDate, value);

            final String dayCode = curDay.withDayOfMonth(value).toDateTime(DateTimeZone.UTC).toString(Constants.DATE_PATTERN);
            final Day day = new Day(value, isThisMonth, isToday, dayCode, hasEvent(dayCode));
            days.add(day);
            value++;
        }

        mCallback.updateCalendar(getMonthName(), days);
    }

    private boolean hasEvent(String dayCode) {
        return mEvents.get(Integer.parseInt(dayCode));
    }

    private boolean isToday(DateTime targetDate, int curDayInMonth) {
        return targetDate.withDayOfMonth(curDayInMonth).toString(Constants.DATE_PATTERN).equals(mToday);
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
