package com.simplemobiletools.calendar;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.RemoteViews;

import com.simplemobiletools.calendar.activities.DayActivity;
import com.simplemobiletools.calendar.activities.MainActivity;
import com.simplemobiletools.calendar.models.Day;

import org.joda.time.DateTime;

import java.util.List;

public class MyWidgetProvider extends AppWidgetProvider implements Calendar {
    private static final String PREV = "prev";
    private static final String NEXT = "next";

    private static RemoteViews mRemoteViews;
    private static AppWidgetManager mWidgetManager;
    private static Intent mIntent;
    private static Context mContext;
    private static CalendarImpl mCalendar;
    private static Resources mRes;

    private static float mDayTextSize;
    private static float mTodayTextSize;
    private static int mTextColor;
    private static int mWeakTextColor;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        initVariables(context);
        updateWidget();
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void initVariables(Context context) {
        mContext = context;
        mRes = mContext.getResources();

        final SharedPreferences prefs = initPrefs(mContext);
        final int storedTextColor = prefs.getInt(Constants.WIDGET_TEXT_COLOR, Color.WHITE);
        mTextColor = Utils.adjustAlpha(storedTextColor, Constants.HIGH_ALPHA);
        mWeakTextColor = Utils.adjustAlpha(storedTextColor, Constants.LOW_ALPHA);

        mDayTextSize = mRes.getDimension(R.dimen.day_text_size) / mRes.getDisplayMetrics().density;
        mTodayTextSize = mRes.getDimension(R.dimen.today_text_size) / mRes.getDisplayMetrics().density;
        mWidgetManager = AppWidgetManager.getInstance(mContext);

        mRemoteViews = new RemoteViews(mContext.getPackageName(), R.layout.calendar_layout);
        mIntent = new Intent(mContext, MyWidgetProvider.class);
        setupButtons();
        updateLabelColor();
        updateTopViews();

        final int bgColor = prefs.getInt(Constants.WIDGET_BG_COLOR, Color.BLACK);
        mRemoteViews.setInt(R.id.calendar_holder, "setBackgroundColor", bgColor);

        mCalendar = new CalendarImpl(this, mContext);
        mCalendar.updateCalendar(new DateTime());
    }

    private void updateWidget() {
        final ComponentName thisWidget = new ComponentName(mContext, MyWidgetProvider.class);
        AppWidgetManager.getInstance(mContext).updateAppWidget(thisWidget, mRemoteViews);
    }

    private void setupIntent(String action, int id) {
        mIntent.setAction(action);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, mIntent, 0);
        mRemoteViews.setOnClickPendingIntent(id, pendingIntent);
    }

    private void setupAppOpenIntent(int id) {
        final Intent intent = new Intent(mContext, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        mRemoteViews.setOnClickPendingIntent(id, pendingIntent);
    }

    private void setupDayOpenIntent(int id, final String dayCode) {
        final Intent intent = new Intent(mContext, DayActivity.class);
        intent.putExtra(Constants.DAY_CODE, dayCode);
        final PendingIntent pendingIntent = PendingIntent.getActivity(mContext, Integer.parseInt(dayCode), intent, 0);
        mRemoteViews.setOnClickPendingIntent(id, pendingIntent);
    }

    private void setupButtons() {
        setupIntent(PREV, R.id.top_left_arrow);
        setupIntent(NEXT, R.id.top_right_arrow);
        setupAppOpenIntent(R.id.top_text);
    }

    private SharedPreferences initPrefs(Context context) {
        return context.getSharedPreferences(Constants.PREFS_KEY, Context.MODE_PRIVATE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mRemoteViews == null || mWidgetManager == null || mCalendar == null || mContext == null)
            initVariables(context);

        final String action = intent.getAction();
        switch (action) {
            case PREV:
                mCalendar.getPrevMonth();
                break;
            case NEXT:
                mCalendar.getNextMonth();
                break;
            default:
                super.onReceive(context, intent);
        }
    }

    public void updateDays(List<Day> days) {
        final String packageName = mContext.getPackageName();
        final int len = days.size();
        for (int i = 0; i < len; i++) {
            final Day day = days.get(i);
            final int id = mRes.getIdentifier("day_" + i, "id", packageName);
            int curTextColor = mWeakTextColor;
            float curTextSize = mDayTextSize;

            if (day.getIsThisMonth()) {
                curTextColor = mTextColor;
            }

            if (day.getIsToday()) {
                curTextSize = mTodayTextSize;
            }

            final String text = String.valueOf(day.getValue());
            if (day.getHasEvent()) {
                final SpannableString boldText = new SpannableString(text);
                boldText.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
                mRemoteViews.setTextViewText(id, boldText);
            } else {
                mRemoteViews.setTextViewText(id, text);
            }
            mRemoteViews.setInt(id, "setTextColor", curTextColor);
            mRemoteViews.setFloat(id, "setTextSize", curTextSize);
            setupDayOpenIntent(id, day.getCode());
        }
    }

    private void updateTopViews() {
        mRemoteViews.setInt(R.id.top_text, "setTextColor", mTextColor);

        Bitmap bmp = getColoredIcon(mContext, mTextColor, R.mipmap.arrow_left);
        mRemoteViews.setImageViewBitmap(R.id.top_left_arrow, bmp);

        bmp = getColoredIcon(mContext, mTextColor, R.mipmap.arrow_right);
        mRemoteViews.setImageViewBitmap(R.id.top_right_arrow, bmp);
    }

    public void updateMonth(String month) {
        mRemoteViews.setTextViewText(R.id.top_text, month);
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        updateMonth(month);
        updateDays(days);
        updateWidget();
    }

    private void updateLabelColor() {
        final String packageName = mContext.getPackageName();
        for (int i = 0; i < 7; i++) {
            final int id = mRes.getIdentifier("label_" + i, "id", packageName);
            mRemoteViews.setInt(id, "setTextColor", mWeakTextColor);
        }
    }

    private Bitmap getColoredIcon(Context context, int newTextColor, int id) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        final Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), id, options);
        final Paint paint = new Paint();
        final ColorFilter filter = new LightingColorFilter(newTextColor, 1);
        paint.setColorFilter(filter);
        final Canvas canvas = new Canvas(bmp);
        canvas.drawBitmap(bmp, 0, 0, paint);
        return bmp;
    }
}
