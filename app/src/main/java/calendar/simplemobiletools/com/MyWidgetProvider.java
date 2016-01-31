package calendar.simplemobiletools.com;

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
    private static float dayTextSize;
    private static float todayTextSize;
    private static int bgColor;
    private static int textColor;
    private static int weakTextColor;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        initVariables(context);
    }

    private void initVariables(Context context) {
        cxt = context;
        res = cxt.getResources();
        bgColor = Color.BLACK;

        final SharedPreferences prefs = initPrefs(cxt);
        final int storedTextColor = prefs.getInt(Constants.WIDGET_TEXT_COLOR, Color.WHITE);
        textColor = Helpers.adjustAlpha(storedTextColor, Constants.HIGH_ALPHA);
        weakTextColor = Helpers.adjustAlpha(storedTextColor, Constants.LOW_ALPHA);

        dayTextSize = res.getDimension(R.dimen.day_text_size) / res.getDisplayMetrics().density;
        todayTextSize = res.getDimension(R.dimen.today_text_size) / res.getDisplayMetrics().density;

        final ComponentName component = new ComponentName(cxt, MyWidgetProvider.class);
        widgetManager = AppWidgetManager.getInstance(cxt);
        widgetIds = widgetManager.getAppWidgetIds(component);

        remoteViews = new RemoteViews(cxt.getPackageName(), R.layout.activity_main);
        intent = new Intent(cxt, MyWidgetProvider.class);
        setupButtons();
        updateLabelColor();
        updateTopViews();

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

    private SharedPreferences initPrefs(Context context) {
        return context.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
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
            int curTextColor = weakTextColor;
            float curTextSize = dayTextSize;

            if (day.getIsThisMonth()) {
                curTextColor = textColor;
            }

            if (day.getIsToday()) {
                curTextSize = todayTextSize;
            }

            remoteViews.setTextViewText(id, String.valueOf(day.getValue()));
            remoteViews.setInt(id, "setTextColor", curTextColor);
            remoteViews.setFloat(id, "setTextSize", curTextSize);
        }
    }

    private void updateTopViews() {
        remoteViews.setInt(R.id.table_month, "setTextColor", textColor);

        Bitmap bmp = getColoredIcon(cxt, textColor, R.mipmap.arrow_left);
        remoteViews.setImageViewBitmap(R.id.left_arrow, bmp);

        bmp = getColoredIcon(cxt, textColor, R.mipmap.arrow_right);
        remoteViews.setImageViewBitmap(R.id.right_arrow, bmp);
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

    private void updateLabelColor() {
        final String packageName = cxt.getPackageName();
        for (int i = 0; i < 7; i++) {
            final int id = res.getIdentifier("label_" + i, "id", packageName);
            remoteViews.setInt(id, "setTextColor", weakTextColor);
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
