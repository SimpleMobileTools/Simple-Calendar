package com.simplemobiletools.calendar.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.simplemobiletools.calendar.Calendar;
import com.simplemobiletools.calendar.CalendarImpl;
import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.MyWidgetProvider;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.Utils;
import com.simplemobiletools.calendar.models.Day;

import org.joda.time.DateTime;

import java.util.List;

import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import yuku.ambilwarna.AmbilWarnaDialog;

public class WidgetConfigureActivity extends AppCompatActivity implements Calendar {
    @BindView(R.id.top_left_arrow) ImageView mLeftArrow;
    @BindView(R.id.top_right_arrow) ImageView mRightArrow;
    @BindView(R.id.top_text) TextView mMonthTV;
    @BindView(R.id.config_bg_color) View mBgColorPicker;
    @BindView(R.id.config_bg_seekbar) SeekBar mBgSeekBar;
    @BindView(R.id.config_text_color) View mTextColorPicker;
    @BindView(R.id.config_calendar) View mWidgetBackground;
    @BindView(R.id.config_save) Button mSaveBtn;
    @BindView(R.id.calendar_fab) View mFab;

    @BindDimen(R.dimen.day_text_size) float mDayTextSize;
    @BindDimen(R.dimen.today_text_size) float mTodayTextSize;

    private static CalendarImpl mCalendar;
    private static Resources mRes;
    private static String mPackageName;
    private List<Day> mDays;

    private float mBgAlpha;
    private int mWidgetId;
    private int mBgColorWithoutTransparency;
    private int mBgColor;
    private int mTextColorWithoutTransparency;
    private int mTextColor;
    private int mWeakTextColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_config);
        ButterKnife.bind(this);
        initVariables();

        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null)
            mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            finish();
    }

    private void initVariables() {
        mRes = getResources();
        mPackageName = getPackageName();
        mDayTextSize /= mRes.getDisplayMetrics().density;
        mTodayTextSize /= mRes.getDisplayMetrics().density;

        final SharedPreferences prefs = initPrefs(this);
        mTextColorWithoutTransparency = prefs.getInt(Constants.WIDGET_TEXT_COLOR, getResources().getColor(R.color.colorPrimary));
        updateTextColors();

        mBgColor = prefs.getInt(Constants.WIDGET_BG_COLOR, 1);
        if (mBgColor == 1) {
            mBgColor = Color.BLACK;
            mBgAlpha = .2f;
        } else {
            mBgAlpha = Color.alpha(mBgColor) / (float) 255;
        }

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor));
        mBgSeekBar.setOnSeekBarChangeListener(bgSeekbarChangeListener);
        mBgSeekBar.setProgress((int) (mBgAlpha * 100));
        updateBgColor();

        mCalendar = new CalendarImpl(this, getApplicationContext());
        mCalendar.updateCalendar(new DateTime());

        mFab.setVisibility(View.GONE);
    }

    private SharedPreferences initPrefs(Context context) {
        return context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE);
    }

    @OnClick(R.id.config_save)
    public void saveConfig() {
        storeWidgetColors();
        requestWidgetUpdate();

        final Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void storeWidgetColors() {
        final SharedPreferences prefs = getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE);
        prefs.edit().putInt(Constants.WIDGET_BG_COLOR, mBgColor).apply();
        prefs.edit().putInt(Constants.WIDGET_TEXT_COLOR, mTextColorWithoutTransparency).apply();
    }

    @OnClick(R.id.config_bg_color)
    public void pickBackgroundColor() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, mBgColorWithoutTransparency, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mBgColorWithoutTransparency = color;
                updateBgColor();
            }
        });

        dialog.show();
    }

    @OnClick(R.id.config_text_color)
    public void pickTextColor() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, mTextColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                mTextColorWithoutTransparency = color;
                updateTextColors();
                updateDays();
            }
        });

        dialog.show();
    }

    private void requestWidgetUpdate() {
        final Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mWidgetId});
        sendBroadcast(intent);
    }

    private void updateTextColors() {
        mTextColor = Utils.adjustAlpha(mTextColorWithoutTransparency, Constants.HIGH_ALPHA);
        mWeakTextColor = Utils.adjustAlpha(mTextColorWithoutTransparency, Constants.LOW_ALPHA);

        mLeftArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        mRightArrow.getDrawable().mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        mMonthTV.setTextColor(mTextColor);
        mTextColorPicker.setBackgroundColor(mTextColor);
        mSaveBtn.setTextColor(mTextColor);
        updateLabels();
    }

    private void updateBgColor() {
        mBgColor = Utils.adjustAlpha(mBgColorWithoutTransparency, mBgAlpha);
        mWidgetBackground.setBackgroundColor(mBgColor);
        mBgColorPicker.setBackgroundColor(mBgColor);
        mSaveBtn.setBackgroundColor(mBgColor);
    }

    private void updateDays() {
        final int len = mDays.size();
        for (int i = 0; i < len; i++) {
            final Day day = mDays.get(i);
            final TextView dayTV = (TextView) findViewById(mRes.getIdentifier("day_" + i, "id", mPackageName));
            int curTextColor = mWeakTextColor;
            float curTextSize = mDayTextSize;

            if (day.getIsThisMonth()) {
                curTextColor = mTextColor;
            }

            if (day.getIsToday()) {
                curTextSize = mTodayTextSize;
            }

            dayTV.setText(String.valueOf(day.getValue()));
            dayTV.setTextColor(curTextColor);
            dayTV.setTextSize(curTextSize);
        }
    }

    private SeekBar.OnSeekBarChangeListener bgSeekbarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mBgAlpha = (float) progress / (float) 100;
            updateBgColor();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @OnClick(R.id.top_left_arrow)
    public void leftArrowClicked() {
        mCalendar.getPrevMonth();
    }

    @OnClick(R.id.top_right_arrow)
    public void rightArrowClicked() {
        mCalendar.getNextMonth();
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        this.mDays = days;
        updateMonth(month);
        updateDays();
    }

    private void updateMonth(String month) {
        mMonthTV.setText(month);
    }

    private void updateLabels() {
        for (int i = 0; i < 7; i++) {
            final TextView dayTV = (TextView) findViewById(mRes.getIdentifier("label_" + i, "id", mPackageName));
            dayTV.setTextSize(mDayTextSize);
            dayTV.setTextColor(mWeakTextColor);
        }
    }
}
