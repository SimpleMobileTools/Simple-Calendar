package com.simplemobiletools.calendar.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.R;

public class MonthFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.calendar_layout, container, false);

        final String code = getArguments().getString(Constants.DAY_CODE);

        return view;
    }
}
