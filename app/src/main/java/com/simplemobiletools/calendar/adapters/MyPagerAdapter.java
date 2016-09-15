package com.simplemobiletools.calendar.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.fragments.MonthFragment;

import java.util.List;

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    private final List<String> mCodes;
    private final MonthFragment.NavigationListener mListener;

    public MyPagerAdapter(FragmentManager fm, List<String> codes, MonthFragment.NavigationListener listener) {
        super(fm);
        mCodes = codes;
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mCodes.size();
    }

    @Override
    public Fragment getItem(int position) {
        final Bundle bundle = new Bundle();
        final String code = mCodes.get(position);
        bundle.putString(Constants.DAY_CODE, code);

        final MonthFragment fragment = new MonthFragment();
        fragment.setArguments(bundle);
        fragment.setListener(mListener);
        return fragment;
    }
}
