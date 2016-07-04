package com.simplemobiletools.calendar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.DBHelper;
import com.simplemobiletools.calendar.EventsAdapter;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.models.Event;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DayActivity extends AppCompatActivity implements DBHelper.DBOperationsListener, AdapterView.OnItemClickListener {
    @BindView(R.id.day_date) TextView mDateTV;
    @BindView(R.id.day_events) ListView mEventsList;

    private static String mDayCode;
    private static List<Event> mEvents;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkEvents();
    }

    @OnClick(R.id.day_fab)
    public void fabClicked(View view) {
        final Intent intent = new Intent(getApplicationContext(), EventActivity.class);
        intent.putExtra(Constants.DAY_CODE, mDayCode);
        startActivity(intent);
    }

    private void editEvent(Event event) {
        final Intent intent = new Intent(getApplicationContext(), EventActivity.class);
        intent.putExtra(Constants.EVENT, event);
        startActivity(intent);
    }

    private void checkEvents() {
        final int startTS = Formatter.getDayStartTS(mDayCode);
        final int endTS = Formatter.getDayEndTS(mDayCode);
        DBHelper.newInstance(getApplicationContext(), this).getEvents(startTS, endTS);
    }

    private void updateEvents(List<Event> events) {
        final EventsAdapter adapter = new EventsAdapter(this, events);
        mEventsList.setAdapter(adapter);
        mEventsList.setOnItemClickListener(this);
        mEvents = events;
    }

    @Override
    public void eventInserted() {

    }

    @Override
    public void eventUpdated() {

    }

    @Override
    public void gotEvents(List<Event> events) {
        updateEvents(events);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        editEvent(mEvents.get(position));
    }
}
