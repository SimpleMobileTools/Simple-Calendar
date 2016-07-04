package com.simplemobiletools.calendar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
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

public class DetailsActivity extends AppCompatActivity implements DBHelper.DBOperationsListener {
    @BindView(R.id.details_date) TextView mDateTV;
    @BindView(R.id.details_events) ListView mEventsList;

    private String dayCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);

        final Intent intent = getIntent();
        if (intent == null)
            return;

        dayCode = intent.getStringExtra(Constants.DAY_CODE);
        if (dayCode == null || dayCode.isEmpty())
            return;

        final String date = Formatter.getEventDate(dayCode);
        mDateTV.setText(date);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkEvents();
    }

    @OnClick(R.id.details_fab)
    public void fabClicked(View view) {
        final Intent intent = new Intent(getApplicationContext(), EventActivity.class);
        intent.putExtra(Constants.DAY_CODE, dayCode);
        startActivity(intent);
    }

    private void checkEvents() {
        final int startTS = Formatter.getDayStartTS(dayCode);
        final int endTS = Formatter.getDayEndTS(dayCode);
        DBHelper.newInstance(getApplicationContext(), this).getEvents(startTS, endTS);
    }

    private void updateEvents(List<Event> events) {
        final EventsAdapter adapter = new EventsAdapter(this, events);
        mEventsList.setAdapter(adapter);
    }

    @Override
    public void eventInserted() {

    }

    @Override
    public void gotEvents(List<Event> events) {
        updateEvents(events);
    }
}
