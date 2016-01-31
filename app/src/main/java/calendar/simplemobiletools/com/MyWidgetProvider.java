package calendar.simplemobiletools.com;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.widget.RemoteViews;

import org.joda.time.DateTime;

import java.util.List;

public class MyWidgetProvider extends AppWidgetProvider implements Calendar {
    private static final String PREV = "prev";
    private static final String NEXT = "next";
    private static RemoteViews remoteViews;
    private static int[] widgetIds;
    private static AppWidgetManager widgetManager;
    private static Intent intent;
    private static Context cxt;
    private static CalendarImpl calendar;
    private static Resources res;
    private static int lightGrey;
    private static int darkGrey;
    private static float dayTextSize;
    private static float todayTextSize;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        initVariables(context);
    }

    private void initVariables(Context context) {
        cxt = context;
        res = cxt.getResources();
        lightGrey = res.getColor(R.color.lightGrey);
        darkGrey = res.getColor(R.color.darkGrey);

        dayTextSize = res.getDimension(R.dimen.day_text_size) / res.getDisplayMetrics().density;
        todayTextSize = res.getDimension(R.dimen.today_text_size) / res.getDisplayMetrics().density;

        final ComponentName component = new ComponentName(cxt, MyWidgetProvider.class);
        widgetManager = AppWidgetManager.getInstance(cxt);
        widgetIds = widgetManager.getAppWidgetIds(component);

        remoteViews = new RemoteViews(cxt.getPackageName(), R.layout.activity_main);
        remoteViews.setInt(R.id.table_month, "setTextColor", Color.WHITE);

        intent = new Intent(cxt, MyWidgetProvider.class);
        setupButtons();

        calendar = new CalendarImpl(this);
        calendar.updateCalendar(new DateTime());

        widgetManager.updateAppWidget(widgetIds, remoteViews);
    }

    private void setupIntent(String action, int id) {
        intent.setAction(action);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(cxt, 0, intent, 0);
        remoteViews.setOnClickPendingIntent(id, pendingIntent);
    }

    private void setupButtons() {
        setupIntent(PREV, R.id.left_arrow);
        setupIntent(NEXT, R.id.right_arrow);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (remoteViews == null || widgetManager == null || widgetIds == null || calendar == null)
            initVariables(context);

        final String action = intent.getAction();
        switch (action) {
            case PREV:
                calendar.getPrevMonth();
                break;
            case NEXT:
                calendar.getNextMonth();
                break;
            default:
                super.onReceive(context, intent);
        }
    }

    public void updateDays(List<Day> days) {
        final String packageName = cxt.getPackageName();
        final int len = days.size();
        for (int i = 0; i < len; i++) {
            final Day day = days.get(i);
            final int id = res.getIdentifier("day_" + i, "id", packageName);
            int textColor = lightGrey;
            float textSize = dayTextSize;

            if (day.getIsThisMonth()) {
                textColor = darkGrey;
            }

            if (day.getIsToday()) {
                textSize = todayTextSize;
            }

            remoteViews.setTextViewText(id, String.valueOf(day.getValue()));
            remoteViews.setInt(id, "setTextColor", textColor);
            remoteViews.setFloat(id, "setTextSize", textSize);
        }
    }

    public void updateMonth(String month) {
        remoteViews.setTextViewText(R.id.table_month, month);
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        updateMonth(month);
        updateDays(days);
        widgetManager.updateAppWidget(widgetIds, remoteViews);
    }
}
