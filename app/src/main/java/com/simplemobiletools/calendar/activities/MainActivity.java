package com.simplemobiletools.calendar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.simplemobiletools.calendar.Config;
import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.adapters.MyPagerAdapter;
import com.simplemobiletools.calendar.fragments.MonthFragment;
import com.simplemobiletools.calendar.views.MyViewPager;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends SimpleActivity implements MonthFragment.NavigationListener {
    @BindView(R.id.view_pager) MyViewPager mPager;

    @BindDimen(R.dimen.day_text_size) float mDayTextSize;
    @BindDimen(R.dimen.today_text_size) float mTodayTextSize;

    private static final int PREFILLED_MONTHS = 73;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final String today = new DateTime().toString(Formatter.DAYCODE_PATTERN);
        fillViewPager(today);
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

    private void fillViewPager(String targetMonth) {
        final List<String> codes = getMonths(targetMonth);
        final MyPagerAdapter adapter = new MyPagerAdapter(getSupportFragmentManager(), codes, this);
        mPager.setAdapter(adapter);
        mPager.setCurrentItem(codes.size() / 2);
    }

    private List<String> getMonths(String code) {
        final List<String> months = new ArrayList<>(PREFILLED_MONTHS);
        final DateTime today = Formatter.getDateTimeFromCode(code);
        for (int i = -PREFILLED_MONTHS / 2; i <= PREFILLED_MONTHS / 2; i++) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)));
        }

        return months;
    }

    @Override
    public void goLeft() {
        mPager.setCurrentItem(mPager.getCurrentItem() - 1);
    }

    @Override
    public void goRight() {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1);
    }

    @Override
    public void goToDateTime(DateTime dateTime) {
        fillViewPager(Formatter.getDayCodeFromDateTime(dateTime));
    }
}
