package com.simplemobiletools.calendar.fragments;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.simplemobiletools.calendar.Calendar;
import com.simplemobiletools.calendar.CalendarImpl;
import com.simplemobiletools.calendar.Config;
import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.Utils;
import com.simplemobiletools.calendar.activities.DayActivity;
import com.simplemobiletools.calendar.models.Day;

import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MonthFragment extends Fragment implements Calendar {
    @BindView(R.id.top_text) TextView mMonthTV;
    @BindView(R.id.top_left_arrow) ImageView mLeftArrow;
    @BindView(R.id.top_right_arrow) ImageView mRightArrow;

    @BindDimen(R.dimen.day_text_size) float mDayTextSize;
    @BindDimen(R.dimen.today_text_size) float mTodayTextSize;

    private View mView;
    private CalendarImpl mCalendar;
    private Resources mRes;
    private Config mConfig;
    private NavigationListener mListener;
    private String mPackageName;
    private String mCode;

    private int mTextColor;
    private int mWeakTextColor;
    private int mTextColorWithEvent;
    private int mWeakTextColorWithEvent;
    private boolean mSundayFirst;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.calendar_layout, container, false);
        ButterKnife.bind(this, mView);
        mRes = getResources();

        mCode = getArguments().getString(Constants.DAY_CODE);
        mConfig = Config.newInstance(getContext());
        mSundayFirst = mConfig.getIsSundayFirst();

        setupColors();

        mPackageName = getActivity().getPackageName();
        mDayTextSize /= mRes.getDisplayMetrics().density;
        mTodayTextSize /= mRes.getDisplayMetrics().density;
        setupLabels();

        return mView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCalendar = new CalendarImpl(this, getContext());
        mCalendar.updateCalendar(Formatter.getDateTimeFromCode(mCode));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mConfig.getIsSundayFirst() != mSundayFirst) {
            mSundayFirst = mConfig.getIsSundayFirst();
            setupLabels();
        }
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        mMonthTV.setText(month);
        updateDays(days);
    }

    public void setListener(NavigationListener listener) {
        mListener = listener;
    }

    private void setupColors() {
        final int baseColor = mConfig.getIsDarkTheme() ? Color.WHITE : Color.BLACK;
        mTextColor = Utils.adjustAlpha(baseColor, Constants.HIGH_ALPHA);
        mTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.HIGH_ALPHA);
        mWeakTextColor = Utils.adjustAlpha(baseColor, Constants.LOW_ALPHA);
        mWeakTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.LOW_ALPHA);
        mLeftArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        mRightArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        mLeftArrow.setBackground(null);
        mRightArrow.setBackground(null);
    }

    @OnClick(R.id.top_left_arrow)
    public void leftArrowClicked() {
        if (mListener != null)
            mListener.goLeft();
    }

    @OnClick(R.id.top_right_arrow)
    public void rightArrowClicked() {
        if (mListener != null)
            mListener.goRight();
    }

    private void setupLabels() {
        int letters[] = Utils.getLetterIDs();

        for (int i = 0; i < 7; i++) {
            final TextView dayTV = (TextView) mView.findViewById(mRes.getIdentifier("label_" + i, "id", mPackageName));
            if (dayTV != null) {
                dayTV.setTextSize(mDayTextSize);
                dayTV.setTextColor(mWeakTextColor);

                int index = i;
                if (!mSundayFirst)
                    index = (index + 1) % letters.length;

                dayTV.setText(getString(letters[index]));
            }
        }
    }

    private void updateDays(List<Day> days) {
        final int len = days.size();

        for (int i = 0; i < len; i++) {
            final Day day = days.get(i);
            final TextView dayTV = (TextView) mView.findViewById(mRes.getIdentifier("day_" + i, "id", mPackageName));
            if (dayTV == null)
                continue;

            int curTextColor = day.getHasEvent() ? mWeakTextColorWithEvent : mWeakTextColor;
            float curTextSize = mDayTextSize;

            if (day.getIsThisMonth()) {
                curTextColor = day.getHasEvent() ? mTextColorWithEvent : mTextColor;
            }

            if (day.getIsToday()) {
                curTextSize = mTodayTextSize;
            }

            dayTV.setText(String.valueOf(day.getValue()));
            dayTV.setTextColor(curTextColor);
            dayTV.setTextSize(curTextSize);

            dayTV.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openDay(day.getCode());
                }
            });
        }
    }

    private void openDay(String code) {
        if (code.isEmpty())
            return;

        final Intent intent = new Intent(getContext(), DayActivity.class);
        intent.putExtra(Constants.DAY_CODE, code);
        startActivity(intent);
    }

    public interface NavigationListener {
        void goLeft();

        void goRight();
    }
}
