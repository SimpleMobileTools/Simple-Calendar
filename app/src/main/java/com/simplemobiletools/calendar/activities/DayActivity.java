package com.simplemobiletools.calendar.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.DBHelper;
import com.simplemobiletools.calendar.EventsAdapter;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.Utils;
import com.simplemobiletools.calendar.models.Event;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DayActivity extends AppCompatActivity
        implements DBHelper.DBOperationsListener, AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {
    @BindView(R.id.top_text) TextView mDateTV;
    @BindView(R.id.day_events) ListView mEventsList;
    @BindView(R.id.day_coordinator) CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.top_left_arrow) ImageView mLeftArrow;
    @BindView(R.id.top_right_arrow) ImageView mRightArrow;

    private static final int EDIT_EVENT = 1;
    public static final String DELETED_ID = "deleted_id";

    private static String mDayCode;
    private static List<Event> mEvents;
    private static int mSelectedItemsCnt;
    private static Snackbar mSnackbar;
    private static List<Integer> mToBeDeleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day);
        ButterKnife.bind(this);

        final Intent intent = getIntent();
        if (intent == null)
            return;

        mDayCode = intent.getStringExtra(Constants.DAY_CODE);
        if (mDayCode == null || mDayCode.isEmpty())
            return;

        final String date = Formatter.getEventDate(mDayCode);
        mDateTV.setText(date);
        mToBeDeleted = new ArrayList<>();

        final int textColor = Utils.adjustAlpha(Color.BLACK, Constants.HIGH_ALPHA);
        mLeftArrow.getDrawable().mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
        mRightArrow.getDrawable().mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkEvents();
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkDeleteEvents();
    }

    @OnClick(R.id.day_fab)
    public void fabClicked(View view) {
        final Intent intent = new Intent(getApplicationContext(), EventActivity.class);
        intent.putExtra(Constants.DAY_CODE, mDayCode);
        startActivity(intent);
    }

    @OnClick(R.id.top_left_arrow)
    public void leftArrowClicked() {
        final DateTime dateTime = Formatter.getDateTimeFromCode(mDayCode);
        final String yesterdayCode = Formatter.getDayCodeFromDateTime(dateTime.minusDays(1));
        switchToDay(yesterdayCode);
    }

    @OnClick(R.id.top_right_arrow)
    public void rightArrowClicked() {
        final DateTime dateTime = Formatter.getDateTimeFromCode(mDayCode);
        final String tomorrowCode = Formatter.getDayCodeFromDateTime(dateTime.plusDays(1));
        switchToDay(tomorrowCode);
    }

    @OnClick(R.id.top_text)
    public void pickDay() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this, R.style.MyAlertDialog);
        final View view = getLayoutInflater().inflate(R.layout.date_picker, null);
        final DatePicker datePicker = (DatePicker) view.findViewById(R.id.date_picker);

        final DateTime dateTime = Formatter.getDateTimeFromCode(mDayCode);
        datePicker.init(dateTime.getYear(), dateTime.getMonthOfYear() - 1, dateTime.getDayOfMonth(), null);

        alertDialog.setView(view);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final int month = datePicker.getMonth() + 1;
                final int year = datePicker.getYear();
                final int day = datePicker.getDayOfMonth();
                final DateTime newDateTime = dateTime.withDayOfMonth(day).withMonthOfYear(month).withYear(year);
                String newDayCode = Formatter.getDayCodeFromDateTime(newDateTime);
                switchToDay(newDayCode);
            }
        });

        alertDialog.show();
    }

    private void switchToDay(String dayCode) {
        final Intent intent = new Intent(getApplicationContext(), DayActivity.class);
        intent.putExtra(Constants.DAY_CODE, dayCode);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void editEvent(Event event) {
        final Intent intent = new Intent(getApplicationContext(), EventActivity.class);
        intent.putExtra(Constants.EVENT, event);
        startActivityForResult(intent, EDIT_EVENT);
    }

    private void checkEvents() {
        final int startTS = Formatter.getDayStartTS(mDayCode);
        final int endTS = Formatter.getDayEndTS(mDayCode);
        DBHelper.newInstance(getApplicationContext(), this).getEvents(startTS, endTS);
    }

    private void updateEvents(List<Event> events) {
        mEvents = new ArrayList<>(events);
        final List<Event> eventsToShow = getEventsToShow(events);
        final EventsAdapter adapter = new EventsAdapter(this, eventsToShow);
        mEventsList.setAdapter(adapter);
        mEventsList.setOnItemClickListener(this);
        mEventsList.setMultiChoiceModeListener(this);
    }

    private List<Event> getEventsToShow(List<Event> events) {
        final int cnt = events.size();
        for (int i = cnt - 1; i >= 0; i--) {
            if (mToBeDeleted.contains(events.get(i).getId())) {
                events.remove(i);
            }
        }
        return events;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDIT_EVENT && resultCode == RESULT_OK && data != null) {
            final int deletedId = data.getIntExtra(DELETED_ID, -1);
            if (deletedId != -1) {
                mToBeDeleted.clear();
                mToBeDeleted.add(deletedId);
                notifyEventDeletion(1);
            }
        }
    }

    @Override
    public void eventInserted(Event event) {

    }

    @Override
    public void eventUpdated(Event event) {

    }

    @Override
    public void eventsDeleted(int cnt) {
        checkEvents();
    }

    private void checkDeleteEvents() {
        if (mSnackbar != null && mSnackbar.isShown()) {
            deleteEvents();
        } else {
            undoDeletion();
        }
    }

    private void deleteEvents() {
        mSnackbar.dismiss();

        final int cnt = mToBeDeleted.size();
        final String[] eventIDs = new String[cnt];
        for (int i = 0; i < cnt; i++) {
            eventIDs[i] = String.valueOf(mToBeDeleted.get(i));
        }

        DBHelper.newInstance(getApplicationContext(), this).deleteEvents(eventIDs);
    }

    private View.OnClickListener undoDeletion = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            undoDeletion();
        }
    };

    private void undoDeletion() {
        if (mSnackbar != null) {
            mToBeDeleted.clear();
            mSnackbar.dismiss();
            updateEvents(mEvents);
        }
    }

    @Override
    public void gotEvents(List<Event> events) {
        updateEvents(events);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        editEvent(getEventsToShow(mEvents).get(position));
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked) {
            mSelectedItemsCnt++;
        } else {
            mSelectedItemsCnt--;
        }

        mode.setTitle(String.valueOf(mSelectedItemsCnt));
        mode.invalidate();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        checkDeleteEvents();
        final MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_day_cab, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                prepareDeleteEvents();
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private void prepareDeleteEvents() {
        final SparseBooleanArray checked = mEventsList.getCheckedItemPositions();
        for (int i = 0; i < mEvents.size(); i++) {
            if (checked.get(i)) {
                final Event event = mEvents.get(i);
                mToBeDeleted.add(event.getId());
            }
        }

        notifyEventDeletion(mToBeDeleted.size());
    }

    private void notifyEventDeletion(int cnt) {
        final Resources res = getResources();
        final String msg = res.getQuantityString(R.plurals.events_deleted, cnt, cnt);
        mSnackbar = Snackbar.make(mCoordinatorLayout, msg, Snackbar.LENGTH_INDEFINITE);
        mSnackbar.setAction(res.getString(R.string.undo), undoDeletion);
        mSnackbar.setActionTextColor(Color.WHITE);
        mSnackbar.show();
        updateEvents(mEvents);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mSelectedItemsCnt = 0;
    }
}
