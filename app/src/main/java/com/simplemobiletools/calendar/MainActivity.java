package com.simplemobiletools.calendar;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements Calendar {
    @BindView(R.id.left_arrow) ImageView leftArrow;
    @BindView(R.id.right_arrow) ImageView rightArrow;
    @BindView(R.id.table_month) TextView monthTV;
    @BindView(R.id.calendar_holder) View calendarHolder;
    @BindDimen(R.dimen.day_text_size) float dayTextSize;
    @BindDimen(R.dimen.today_text_size) float todayTextSize;
    @BindDimen(R.dimen.activity_margin) int activityMargin;

    private CalendarImpl calendar;
    private Resources res;
    private String packageName;
    private int textColor;
    private int weakTextColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        textColor = Helpers.adjustAlpha(Color.BLACK, Constants.HIGH_ALPHA);
        weakTextColor = Helpers.adjustAlpha(Color.BLACK, Constants.LOW_ALPHA);
        leftArrow.getDrawable().mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
        rightArrow.getDrawable().mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);

        res = getResources();
        packageName = getPackageName();
        dayTextSize /= res.getDisplayMetrics().density;
        todayTextSize /= res.getDisplayMetrics().density;
        setupLabels();

        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) calendarHolder.getLayoutParams();
        params.setMargins(activityMargin, activityMargin, activityMargin, activityMargin);

        calendar = new CalendarImpl(this);
        calendar.updateCalendar(new DateTime());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
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

    private void updateDays(List<Day> days) {
        final int len = days.size();

        for (int i = 0; i < len; i++) {
            final Day day = days.get(i);
            final TextView dayTV = (TextView) findViewById(res.getIdentifier("day_" + i, "id", packageName));
            int curTextColor = weakTextColor;
            float curTextSize = dayTextSize;

            if (day.getIsThisMonth()) {
                curTextColor = textColor;
            }

            if (day.getIsToday()) {
                curTextSize = todayTextSize;
            }

            dayTV.setText(String.valueOf(day.getValue()));
            dayTV.setTextColor(curTextColor);
            dayTV.setTextSize(curTextSize);
        }
    }

    @OnClick(R.id.left_arrow)
    public void leftArrowClicked() {
        calendar.getPrevMonth();
    }

    @OnClick(R.id.right_arrow)
    public void rightArrowClicked() {
        calendar.getNextMonth();
    }

    @OnClick(R.id.table_month)
    public void pickMonth() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.MyAlertDialog);
        final View view = getLayoutInflater().inflate(R.layout.date_picker, null);
        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker);
        hideDayPicker(datePicker);

        final DateTime dateTime = new DateTime(calendar.getTargetDate().toString());
        datePicker.init(dateTime.getYear(), dateTime.getMonthOfYear() - 1, 1, null);

        alertDialog.setView(view);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final int month = datePicker.getMonth() + 1;
                final int year = datePicker.getYear();
                calendar.updateCalendar(new DateTime().withMonthOfYear(month).withYear(year));
            }
        });

        alertDialog.show();
    }

    private void hideDayPicker(DatePicker datePicker) {
        final LinearLayout ll = (LinearLayout) datePicker.getChildAt(0);
        final LinearLayout ll2 = (LinearLayout) ll.getChildAt(0);
        ll2.getChildAt(0).setVisibility(View.GONE);
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        updateMonth(month);
        updateDays(days);
    }

    private void updateMonth(String month) {
        monthTV.setText(month);
        monthTV.setTextColor(textColor);
    }

    private void setupLabels() {
        for (int i = 0; i < 7; i++) {
            final TextView dayTV = (TextView) findViewById(res.getIdentifier("label_" + i, "id", packageName));
            dayTV.setTextSize(dayTextSize);
            dayTV.setTextColor(weakTextColor);
        }
    }
}
