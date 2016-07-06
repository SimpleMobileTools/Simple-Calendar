package com.simplemobiletools.calendar.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.simplemobiletools.calendar.Calendar;
import com.simplemobiletools.calendar.CalendarImpl;
import com.simplemobiletools.calendar.Config;
import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.Utils;
import com.simplemobiletools.calendar.models.Day;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Locale;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements Calendar {
    @BindView(R.id.top_left_arrow) ImageView mLeftArrow;
    @BindView(R.id.top_right_arrow) ImageView mRightArrow;
    @BindView(R.id.top_text) TextView mMonthTV;
    @BindView(R.id.calendar_holder) View mCalendarHolder;

    @BindDimen(R.dimen.day_text_size) float mDayTextSize;
    @BindDimen(R.dimen.today_text_size) float mTodayTextSize;
    @BindDimen(R.dimen.activity_margin) int mActivityMargin;

    private CalendarImpl mCalendar;
    private Resources mRes;
    private String mPackageName;

    private int mTextColor;
    private int mWeakTextColor;
    private int mTextColorWithEvent;
    private int mWeakTextColorWithEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mRes = getResources();
        Locale.setDefault(Locale.ENGLISH);
        mTextColor = Utils.adjustAlpha(Color.BLACK, Constants.HIGH_ALPHA);
        mTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.HIGH_ALPHA);
        mWeakTextColor = Utils.adjustAlpha(Color.BLACK, Constants.LOW_ALPHA);
        mWeakTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.LOW_ALPHA);
        mLeftArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        mRightArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);

        mPackageName = getPackageName();
        mDayTextSize /= mRes.getDisplayMetrics().density;
        mTodayTextSize /= mRes.getDisplayMetrics().density;
        setupLabels();

        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) mCalendarHolder.getLayoutParams();
        params.setMargins(mActivityMargin, mActivityMargin, mActivityMargin, mActivityMargin);

        mCalendar = new CalendarImpl(this, getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCalendar.updateCalendar(new DateTime());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Config.newInstance(getApplicationContext()).setIsFirstRun(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                final Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.calendar_fab)
    public void fabClicked(View view) {
        final Intent intent = new Intent(getApplicationContext(), EventActivity.class);
        final String tomorrowCode = Formatter.getDayCodeFromDateTime(new DateTime(DateTimeZone.getDefault()).plusDays(1));
        intent.putExtra(Constants.DAY_CODE, tomorrowCode);
        startActivity(intent);
    }

    private void updateDays(List<Day> days) {
        final int len = days.size();

        for (int i = 0; i < len; i++) {
            final Day day = days.get(i);
            final TextView dayTV = (TextView) findViewById(mRes.getIdentifier("day_" + i, "id", mPackageName));
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

        final Intent intent = new Intent(getApplicationContext(), DayActivity.class);
        intent.putExtra(Constants.DAY_CODE, code);
        startActivity(intent);
    }

    @OnClick(R.id.top_left_arrow)
    public void leftArrowClicked() {
        mCalendar.getPrevMonth();
    }

    @OnClick(R.id.top_right_arrow)
    public void rightArrowClicked() {
        mCalendar.getNextMonth();
    }

    @OnClick(R.id.top_text)
    public void pickMonth() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.MyAlertDialog);
        final View view = getLayoutInflater().inflate(R.layout.date_picker, null);
        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker);
        hideDayPicker(datePicker);

        final DateTime dateTime = new DateTime(mCalendar.getTargetDate().toString());
        datePicker.init(dateTime.getYear(), dateTime.getMonthOfYear() - 1, 1, null);

        alertDialog.setView(view);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final int month = datePicker.getMonth() + 1;
                final int year = datePicker.getYear();
                mCalendar.updateCalendar(new DateTime().withMonthOfYear(month).withYear(year));
            }
        });

        alertDialog.show();
    }

    private void hideDayPicker(DatePicker datePicker) {
        final LinearLayout ll = (LinearLayout) datePicker.getChildAt(0);
        final LinearLayout ll2 = (LinearLayout) ll.getChildAt(0);
        ll2.getChildAt(1).setVisibility(View.GONE);
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        updateMonth(month);
        updateDays(days);
    }

    private void updateMonth(String month) {
        mMonthTV.setText(month);
    }

    private void setupLabels() {
        for (int i = 0; i < 7; i++) {
            final TextView dayTV = (TextView) findViewById(mRes.getIdentifier("label_" + i, "id", mPackageName));
            if (dayTV != null) {
                dayTV.setTextSize(mDayTextSize);
                dayTV.setTextColor(mWeakTextColor);
            }
        }
    }
}
