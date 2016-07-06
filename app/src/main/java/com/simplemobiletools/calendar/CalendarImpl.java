package com.simplemobiletools.calendar;

import android.content.Context;

import com.simplemobiletools.calendar.models.Day;
import com.simplemobiletools.calendar.models.Event;

import org.joda.time.DateTime;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class CalendarImpl implements DBHelper.DBOperationsListener {
    public static final int DAYS_CNT = 42;
    private static final String YEAR_PATTERN = "YYYY";

    private final Calendar mCallback;
    private final String mToday;
    private final Context mContext;
    private DateTime mTargetDate;
    private List<Event> mEvents;

    public CalendarImpl(Calendar callback, Context context) {
        mCallback = callback;
        mContext = context;
        mToday = new DateTime().toString(Formatter.DAYCODE_PATTERN);
    }

    public void updateCalendar(DateTime targetDate) {
        mTargetDate = targetDate;
        final int startTS = Formatter.getDayStartTS(Formatter.getDayCodeFromDateTime(mTargetDate.minusMonths(1)));
        final int endTS = Formatter.getDayEndTS(Formatter.getDayCodeFromDateTime(mTargetDate.plusMonths(1)));
        DBHelper.newInstance(mContext, this).getEvents(startTS, endTS);
    }

    public void getPrevMonth() {
        updateCalendar(mTargetDate.minusMonths(1));
    }

    public void getNextMonth() {
        updateCalendar(mTargetDate.plusMonths(1));
    }

    private void getDays() {
        final List<Day> days = new ArrayList<>(DAYS_CNT);

        final int currMonthDays = mTargetDate.dayOfMonth().getMaximumValue();
        final int firstDayIndex = mTargetDate.withDayOfMonth(1).getDayOfWeek() - 1;
        final int prevMonthDays = mTargetDate.minusMonths(1).dayOfMonth().getMaximumValue();

        boolean isThisMonth = false;
        boolean isToday;
        int value = prevMonthDays - firstDayIndex + 1;
        DateTime curDay = mTargetDate;

        for (int i = 0; i < DAYS_CNT; i++) {
            if (i < firstDayIndex) {
                isThisMonth = false;
                curDay = mTargetDate.minusMonths(1);
            } else if (i == firstDayIndex) {
                value = 1;
                isThisMonth = true;
                curDay = mTargetDate;
            } else if (value == currMonthDays + 1) {
                value = 1;
                isThisMonth = false;
                curDay = mTargetDate.plusMonths(1);
            }

            isToday = isThisMonth && isToday(mTargetDate, value);

            final String dayCode = Formatter.getDayCodeFromDateTime(curDay.withDayOfMonth(value));
            final Day day = new Day(value, isThisMonth, isToday, dayCode, hasEvent(dayCode));
            days.add(day);
            value++;
        }

        mCallback.updateCalendar(getMonthName(), days);
    }

    private boolean hasEvent(String dayCode) {
        for (Event event : mEvents) {
            if (Formatter.getDayCodeFromTS(event.getStartTS()).equals(dayCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isToday(DateTime targetDate, int curDayInMonth) {
        return targetDate.withDayOfMonth(curDayInMonth).toString(Formatter.DAYCODE_PATTERN).equals(mToday);
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

    @Override
    public void eventInserted(Event event) {

    }

    @Override
    public void eventUpdated(Event event) {

    }

    @Override
    public void eventsDeleted(int cnt) {

    }

    @Override
    public void gotEvents(List<Event> events) {
        mEvents = events;
        getDays();
    }
}
