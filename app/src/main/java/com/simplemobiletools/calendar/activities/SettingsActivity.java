package com.simplemobiletools.calendar.activities;

import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.SwitchCompat;

import com.simplemobiletools.calendar.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingsActivity extends SimpleActivity {
    @BindView(R.id.settings_dark_theme) SwitchCompat mDarkThemeSwitch;
    @BindView(R.id.settings_sunday_first) SwitchCompat mSundayFirstSwitch;
    @BindView(R.id.settings_week_numbers) SwitchCompat mWeekNumbersSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        setupDarkTheme();
        setupSundayFirst();
        setupWeekNumbers();
    }

    private void setupDarkTheme() {
        mDarkThemeSwitch.setChecked(mConfig.getIsDarkTheme());
    }

    private void setupSundayFirst() {
        mSundayFirstSwitch.setChecked(mConfig.getIsSundayFirst());
    }

    private void setupWeekNumbers() {
        mWeekNumbersSwitch.setChecked(mConfig.getDisplayWeekNumbers());
    }

    @OnClick(R.id.settings_dark_theme_holder)
    public void handleDarkTheme() {
        mDarkThemeSwitch.setChecked(!mDarkThemeSwitch.isChecked());
        mConfig.setIsDarkTheme(mDarkThemeSwitch.isChecked());
        restartActivity();
    }

    @OnClick(R.id.settings_sunday_first_holder)
    public void handleSundayFirst() {
        mSundayFirstSwitch.setChecked(!mSundayFirstSwitch.isChecked());
        mConfig.setIsSundayFirst(mSundayFirstSwitch.isChecked());
    }

    @OnClick(R.id.settings_week_numbers_holder)
    public void handleWeekNumbers() {
        mWeekNumbersSwitch.setChecked(!mWeekNumbersSwitch.isChecked());
        mConfig.setDisplayWeekNumbers(mWeekNumbersSwitch.isChecked());
    }

    private void restartActivity() {
        TaskStackBuilder.create(getApplicationContext()).addNextIntentWithParentStack(getIntent()).startActivities();
    }
}
