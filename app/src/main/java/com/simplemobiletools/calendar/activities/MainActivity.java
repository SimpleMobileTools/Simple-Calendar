package com.simplemobiletools.calendar.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.simplemobiletools.calendar.Config;
import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.Utils;
import com.simplemobiletools.calendar.adapters.MyPagerAdapter;
import com.simplemobiletools.calendar.models.Day;
import com.simplemobiletools.calendar.views.MyViewPager;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends SimpleActivity {
    /*@BindView(R.id.top_left_arrow) ImageView mLeftArrow;
    @BindView(R.id.top_right_arrow) ImageView mRightArrow;
    @BindView(R.id.calendar_holder) View mCalendarHolder;*/
    @BindView(R.id.view_pager) MyViewPager mPager;

    @BindDimen(R.dimen.day_text_size) float mDayTextSize;
    @BindDimen(R.dimen.today_text_size) float mTodayTextSize;

    private static final int PREFILLED_MONTHS = 73;

    private Resources mRes;
    private String mPackageName;

    private int mTextColor;
    private int mWeakTextColor;
    private int mTextColorWithEvent;
    private int mWeakTextColorWithEvent;
    private boolean mSundayFirst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final int baseColor = mConfig.getIsDarkTheme() ? Color.WHITE : Color.BLACK;
        mRes = getResources();
        mTextColor = Utils.adjustAlpha(baseColor, Constants.HIGH_ALPHA);
        mTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.HIGH_ALPHA);
        mWeakTextColor = Utils.adjustAlpha(baseColor, Constants.LOW_ALPHA);
        mWeakTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.LOW_ALPHA);
        //mLeftArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        //mRightArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        mSundayFirst = mConfig.getIsSundayFirst();

        mPackageName = getPackageName();
        mDayTextSize /= mRes.getDisplayMetrics().density;
        mTodayTextSize /= mRes.getDisplayMetrics().density;
        setupLabels();

        final String today = new DateTime().toString(Formatter.DAYCODE_PATTERN);
        final List<String> codes = getMonths(today);
        final MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager(), codes);
        mPager.setAdapter(adapter);
        mPager.setCurrentItem(codes.size() / 2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mConfig.getIsSundayFirst() != mSundayFirst) {
            mSundayFirst = mConfig.getIsSundayFirst();
            setupLabels();
        }
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
            case R.id.settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            case R.id.about:
                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
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

    private List<String> getMonths(String code) {
        final List<String> months = new ArrayList<>(PREFILLED_MONTHS);
        final DateTime today = Formatter.getDateTimeFromCode(code);
        for (int i = -PREFILLED_MONTHS / 2; i <= PREFILLED_MONTHS / 2; i++) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)));
        }

        return months;
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

    /*@OnClick(R.id.top_left_arrow)
    public void leftArrowClicked() {
        mCalendar.getPrevMonth();
    }

    @OnClick(R.id.top_right_arrow)
    public void rightArrowClicked() {
        mCalendar.getNextMonth();
    }

    @OnClick(R.id.top_text)
    public void pickMonth() {
        final int theme = mConfig.getIsDarkTheme() ? R.style.MyAlertDialog_Dark : R.style.MyAlertDialog;
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, theme);
        final View view = getLayoutInflater().inflate(R.layout.date_picker, null);
        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker);
        hideDayPicker(datePicker);

        final DateTime dateTime = new DateTime(mCalendar.getTargetDate().toString());
        datePicker.init(dateTime.getYear(), dateTime.getMonthOfYear() - 1, 1, null);

        alertDialog.setView(view);
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final int month = datePicker.getMonth() + 1;
                final int year = datePicker.getYear();
                mCalendar.updateCalendar(new DateTime().withMonthOfYear(month).withYear(year));
            }
        });

        alertDialog.show();
    }*/

    private void hideDayPicker(DatePicker datePicker) {
        final LinearLayout ll = (LinearLayout) datePicker.getChildAt(0);
        final LinearLayout ll2 = (LinearLayout) ll.getChildAt(0);
        final NumberPicker picker1 = (NumberPicker) ll2.getChildAt(0);
        final NumberPicker picker2 = (NumberPicker) ll2.getChildAt(1);
        final NumberPicker dayPicker = (picker1.getMaxValue() > picker2.getMaxValue()) ? picker1 : picker2;
        dayPicker.setVisibility(View.GONE);
    }

    private void setupLabels() {
        int letters[] = Utils.getLetterIDs();

        for (int i = 0; i < 7; i++) {
            final TextView dayTV = (TextView) findViewById(mRes.getIdentifier("label_" + i, "id", mPackageName));
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
