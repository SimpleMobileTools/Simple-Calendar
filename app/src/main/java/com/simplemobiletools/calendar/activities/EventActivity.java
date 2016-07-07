package com.simplemobiletools.calendar.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.DBHelper;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.Utils;
import com.simplemobiletools.calendar.models.Event;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;

public class EventActivity extends AppCompatActivity implements DBHelper.DBOperationsListener {
    @BindView(R.id.event_start_date) TextView mStartDate;
    @BindView(R.id.event_start_time) TextView mStartTime;
    @BindView(R.id.event_end_date) TextView mEndDate;
    @BindView(R.id.event_end_time) TextView mEndTime;
    @BindView(R.id.event_title) EditText mTitleET;
    @BindView(R.id.event_description) EditText mDescriptionET;
    @BindView(R.id.event_reminder_other) EditText mReminderOtherET;
    @BindView(R.id.event_reminder) AppCompatSpinner mReminder;

    private static DateTime mEventStartDateTime;
    private static DateTime mEventEndDateTime;
    private static Event mEvent;
    private static boolean mWasReminderInit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        ButterKnife.bind(this);

        final Intent intent = getIntent();
        if (intent == null)
            return;

        mWasReminderInit = false;
        final Event event = (Event) intent.getSerializableExtra(Constants.EVENT);
        if (event != null) {
            mEvent = event;
            setupEditEvent();
        } else {
            mEvent = new Event();
            final String dayCode = intent.getStringExtra(Constants.DAY_CODE);
            if (dayCode == null || dayCode.isEmpty())
                return;

            setupNewEvent(dayCode);
        }

        updateStartDate();
        updateStartTime();
        updateEndDate();
        updateEndTime();
        setupReminder();
    }

    private void setupEditEvent() {
        setTitle(getResources().getString(R.string.edit_event));
        mEventStartDateTime = new DateTime(mEvent.getStartTS() * 1000L, DateTimeZone.getDefault());
        mEventEndDateTime = new DateTime(mEvent.getEndTS() * 1000L, DateTimeZone.getDefault());
        mTitleET.setText(mEvent.getTitle());
        mDescriptionET.setText(mEvent.getDescription());
        hideKeyboard();
    }

    private void setupNewEvent(String dayCode) {
        setTitle(getResources().getString(R.string.new_event));
        mEventStartDateTime = Formatter.getDateTimeFromCode(dayCode).withZone(DateTimeZone.getDefault()).withHourOfDay(13);
        mEventEndDateTime = Formatter.getDateTimeFromCode(dayCode).withZone(DateTimeZone.getDefault()).withHourOfDay(14);
    }

    private void hideKeyboard() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void showKeyboard(EditText et) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
    }

    private void setupReminder() {
        switch (mEvent.getReminderMinutes()) {
            case -1:
                mReminder.setSelection(0);
                break;
            case 0:
                mReminder.setSelection(1);
                break;
            default:
                mReminder.setSelection(2);
                mReminderOtherET.setVisibility(View.VISIBLE);
                mReminderOtherET.setText(String.valueOf(mEvent.getReminderMinutes()));
                break;
        }
    }

    @OnItemSelected(R.id.event_reminder)
    public void handleReminder() {
        if (!mWasReminderInit) {
            mWasReminderInit = true;
            return;
        }

        if (mReminder.getSelectedItemPosition() == mReminder.getCount() - 1) {
            mReminderOtherET.setVisibility(View.VISIBLE);
            mReminderOtherET.requestFocus();
            showKeyboard(mReminderOtherET);
        } else {
            mReminderOtherET.setVisibility(View.GONE);
            hideKeyboard();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_event, menu);
        final MenuItem item = menu.findItem(R.id.delete);
        if (mEvent.getId() == 0) {
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                deleteEvent();
                return true;
            case R.id.save:
                saveEvent();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void deleteEvent() {
        final Intent intent = new Intent();
        intent.putExtra(DayActivity.DELETED_ID, mEvent.getId());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void saveEvent() {
        final String title = mTitleET.getText().toString().trim();
        if (title.isEmpty()) {
            Utils.showToast(getApplicationContext(), R.string.title_empty);
            mTitleET.requestFocus();
            return;
        }

        final int startTS = (int) (mEventStartDateTime.getMillis() / 1000);
        final int endTS = (int) (mEventEndDateTime.getMillis() / 1000);

        if (startTS > endTS) {
            Utils.showToast(getApplicationContext(), R.string.end_before_start);
            return;
        }

        if (DateTime.now().isAfter(mEventStartDateTime.getMillis())) {
            Utils.showToast(getApplicationContext(), R.string.start_in_past);
            return;
        }

        final DBHelper dbHelper = DBHelper.newInstance(getApplicationContext(), this);
        final String description = mDescriptionET.getText().toString().trim();
        final int reminderMinutes = getReminderMinutes();
        mEvent.setStartTS(startTS);
        mEvent.setEndTS(endTS);
        mEvent.setTitle(title);
        mEvent.setDescription(description);
        mEvent.setReminderMinutes(reminderMinutes);
        if (mEvent.getId() == 0) {
            dbHelper.insert(mEvent);
        } else {
            dbHelper.update(mEvent);
        }
    }

    private int getReminderMinutes() {
        switch (mReminder.getSelectedItemPosition()) {
            case 0:
                return -1;
            case 1:
                return 0;
            default:
                final String value = mReminderOtherET.getText().toString().trim();
                if (value.isEmpty())
                    return 0;

                return Integer.valueOf(value);
        }
    }

    private void updateStartDate() {
        mStartDate.setText(Formatter.getEventDate(mEventStartDateTime));
    }

    private void updateStartTime() {
        mStartTime.setText(Formatter.getEventTime(mEventStartDateTime));
    }

    private void updateEndDate() {
        mEndDate.setText(Formatter.getEventDate(mEventEndDateTime));
    }

    private void updateEndTime() {
        mEndTime.setText(Formatter.getEventTime(mEventEndDateTime));
    }

    @OnClick(R.id.event_start_date)
    public void startDateClicked(View view) {
        new DatePickerDialog(this, startDateSetListener, mEventStartDateTime.getYear(), mEventStartDateTime.getMonthOfYear() - 1,
                mEventStartDateTime.getDayOfMonth()).show();
    }

    @OnClick(R.id.event_start_time)
    public void startTimeClicked(View view) {
        new TimePickerDialog(this, startTimeSetListener, mEventStartDateTime.getHourOfDay(), mEventStartDateTime.getMinuteOfHour(), true)
                .show();
    }

    @OnClick(R.id.event_end_date)
    public void endDateClicked(View view) {
        new DatePickerDialog(this, endDateSetListener, mEventEndDateTime.getYear(), mEventEndDateTime.getMonthOfYear() - 1,
                mEventEndDateTime.getDayOfMonth()).show();
    }

    @OnClick(R.id.event_end_time)
    public void endTimeClicked(View view) {
        new TimePickerDialog(this, endTimeSetListener, mEventEndDateTime.getHourOfDay(), mEventEndDateTime.getMinuteOfHour(), true).show();
    }

    private final DatePickerDialog.OnDateSetListener startDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateSet(year, monthOfYear, dayOfMonth, true);
        }
    };

    private TimePickerDialog.OnTimeSetListener startTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            timeSet(hourOfDay, minute, true);
        }
    };

    private DatePickerDialog.OnDateSetListener endDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateSet(year, monthOfYear, dayOfMonth, false);
        }
    };

    private TimePickerDialog.OnTimeSetListener endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            timeSet(hourOfDay, minute, false);
        }
    };

    private void dateSet(int year, int month, int day, boolean isStart) {
        if (isStart) {
            mEventStartDateTime = mEventStartDateTime.withYear(year).withMonthOfYear(month + 1).withDayOfMonth(day);
            updateStartDate();
        } else {
            mEventEndDateTime = mEventEndDateTime.withYear(year).withMonthOfYear(month + 1).withDayOfMonth(day);
            updateEndDate();
        }
    }

    private void timeSet(int hours, int minutes, boolean isStart) {
        if (isStart) {
            mEventStartDateTime = mEventStartDateTime.withHourOfDay(hours).withMinuteOfHour(minutes);
            updateStartTime();
        } else {
            mEventEndDateTime = mEventEndDateTime.withHourOfDay(hours).withMinuteOfHour(minutes);
            updateEndTime();
        }
    }

    @Override
    public void eventInserted(Event event) {
        Utils.scheduleNotification(getApplicationContext(), event);
        Utils.showToast(getApplicationContext(), R.string.event_added);
        finish();
    }

    @Override
    public void eventUpdated(Event event) {
        Utils.scheduleNotification(getApplicationContext(), event);
        Utils.showToast(getApplicationContext(), R.string.event_updated);
        finish();
    }

    @Override
    public void eventsDeleted(int cnt) {

    }

    @Override
    public void gotEvents(List<Event> events) {

    }
}
