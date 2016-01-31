package calendar.simplemobiletools.com;

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
import android.widget.ImageView;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.List;

import butterknife.Bind;
import butterknife.BindDimen;
import butterknife.ButterKnife;
import butterknife.OnClick;
import yuku.ambilwarna.AmbilWarnaDialog;

public class MyWidgetConfigure extends AppCompatActivity implements Calendar {
    @Bind(R.id.left_arrow) ImageView leftArrow;
    @Bind(R.id.right_arrow) ImageView rightArrow;
    @Bind(R.id.table_month) TextView monthTV;
    @Bind(R.id.config_text_color) View textColorPicker;
    @BindDimen(R.dimen.day_text_size) float dayTextSize;
    @BindDimen(R.dimen.today_text_size) float todayTextSize;

    private int widgetId;
    private CalendarImpl calendar;
    private Resources res;
    private String packageName;
    private List<Day> days;
    private static int textColorWithoutTransparency;
    private static int textColor;
    private static int weakTextColor;

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
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            finish();
    }

    private void initVariables() {
        res = getResources();
        packageName = getPackageName();
        dayTextSize /= res.getDisplayMetrics().density;
        todayTextSize /= res.getDisplayMetrics().density;

        textColorWithoutTransparency = Color.WHITE;
        updateTextColors();

        calendar = new CalendarImpl(this);
        calendar.updateCalendar(new DateTime());
    }

    @OnClick(R.id.config_save)
    public void saveConfig() {
        storeWidgetColors();
        requestWidgetUpdate();

        final Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void storeWidgetColors() {
        final SharedPreferences prefs = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(Constants.WIDGET_TEXT_COLOR, textColorWithoutTransparency).apply();
    }

    @OnClick(R.id.config_text_color)
    public void pickTextColor() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, textColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                textColorWithoutTransparency = color;
                updateTextColors();
                updateDays();
            }
        });

        dialog.show();
    }

    private void requestWidgetUpdate() {
        final Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{widgetId});
        sendBroadcast(intent);
    }

    private void updateTextColors() {
        textColor = Helpers.adjustAlpha(textColorWithoutTransparency, Constants.HIGH_ALPHA);
        weakTextColor = Helpers.adjustAlpha(textColorWithoutTransparency, Constants.LOW_ALPHA);

        leftArrow.getDrawable().mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
        rightArrow.getDrawable().mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
        monthTV.setTextColor(textColor);
        textColorPicker.setBackgroundColor(textColor);
        updateLabels();
    }

    private void updateDays() {
        final int len = days.size();
        for (int i = 0; i < len; i++) {
            final Day day = days.get(i);
            final TextView dayTV = (TextView) findViewById(res.getIdentifier("day_" + i, "id", packageName));
            int curTextColor = weakTextColor;
            float curTextSize = dayTextSize;

            if (day.getIsThisMonth()) {
                curTextColor = textColor;
            }

            if (day.getIsToday()) {
                curTextSize = todayTextSize;
            }

            dayTV.setText(String.valueOf(day.getValue()));
            dayTV.setTextColor(curTextColor);
            dayTV.setTextSize(curTextSize);
        }
    }

    @OnClick(R.id.left_arrow)
    public void leftArrowClicked() {
        calendar.getPrevMonth();
    }

    @OnClick(R.id.right_arrow)
    public void rightArrowClicked() {
        calendar.getNextMonth();
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        this.days = days;
        updateMonth(month);
        updateDays();
    }

    private void updateMonth(String month) {
        monthTV.setText(month);
    }

    private void updateLabels() {
        for (int i = 0; i < 7; i++) {
            final TextView dayTV = (TextView) findViewById(res.getIdentifier("label_" + i, "id", packageName));
            dayTV.setTextSize(dayTextSize);
            dayTV.setTextColor(weakTextColor);
        }
    }
}
