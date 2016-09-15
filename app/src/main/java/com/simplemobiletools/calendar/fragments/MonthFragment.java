package com.simplemobiletools.calendar.fragments;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
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
    private String mPackageName;
    private Config mConfig;

    private int mTextColor;
    private int mWeakTextColor;
    private int mTextColorWithEvent;
    private int mWeakTextColorWithEvent;
    private boolean mSundayFirst;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.calendar_layout, container, false);
        ButterKnife.bind(this, mView);

        final String code = getArguments().getString(Constants.DAY_CODE);
        mCalendar = new CalendarImpl(this, getContext());
        mCalendar.updateCalendar(Formatter.getDateTimeFromCode(code));
        mConfig = Config.newInstance(getContext());
        mSundayFirst = mConfig.getIsSundayFirst();

        mRes = getResources();
        setupColors();

        mPackageName = getActivity().getPackageName();
        mDayTextSize /= mRes.getDisplayMetrics().density;
        setupLabels();

        return mView;
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
    }

    private void setupColors() {
        final int baseColor = mConfig.getIsDarkTheme() ? Color.WHITE : Color.BLACK;
        mTextColor = Utils.adjustAlpha(baseColor, Constants.HIGH_ALPHA);
        mTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.HIGH_ALPHA);
        mWeakTextColor = Utils.adjustAlpha(baseColor, Constants.LOW_ALPHA);
        mWeakTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.LOW_ALPHA);
        mLeftArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        mRightArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
    }

    @OnClick(R.id.top_left_arrow)
    public void leftArrowClicked() {

    }

    @OnClick(R.id.top_right_arrow)
    public void rightArrowClicked() {

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
}
