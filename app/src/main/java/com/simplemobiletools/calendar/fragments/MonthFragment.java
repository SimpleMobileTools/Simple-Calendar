package com.simplemobiletools.calendar.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplemobiletools.calendar.Calendar;
import com.simplemobiletools.calendar.CalendarImpl;
import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.models.Day;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MonthFragment extends Fragment implements Calendar {
    @BindView(R.id.top_text) TextView mMonthTV;
    private CalendarImpl mCalendar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.calendar_layout, container, false);
        ButterKnife.bind(this, view);

        final String code = getArguments().getString(Constants.DAY_CODE);
        mCalendar = new CalendarImpl(this, getActivity().getApplicationContext());
        mCalendar.updateCalendar(Formatter.getDateTimeFromCode(code));

        return view;
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        mMonthTV.setText(month);
    }
}
