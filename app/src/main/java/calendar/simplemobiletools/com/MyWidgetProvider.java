package calendar.simplemobiletools.com;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        initVariables(context);
    }

    private void initVariables(Context context) {
        cxt = context;
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

    @Override
    public void updateDays(List<Day> days) {

    }

    @Override
    public void updateMonth(String month) {
        remoteViews.setTextViewText(R.id.table_month, month);
        widgetManager.updateAppWidget(widgetIds, remoteViews);
    }
}
