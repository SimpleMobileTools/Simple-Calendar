package com.simplemobiletools.calendar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DetailsActivity extends AppCompatActivity {
    @BindView(R.id.details_date) TextView mDateTV;

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

    @OnClick(R.id.details_fab)
    public void fabClicked(View view) {
        final Intent intent = new Intent(getApplicationContext(), EventActivity.class);
        intent.putExtra(Constants.DAY_CODE, dayCode);
        startActivity(intent);
    }
}
