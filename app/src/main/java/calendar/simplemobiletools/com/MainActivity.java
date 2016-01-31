package calendar.simplemobiletools.com;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.List;

import butterknife.Bind;
import butterknife.BindDimen;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements MyDatePickerDialog.DatePickedListener, Calendar {
    @Bind(R.id.left_arrow) ImageView leftArrow;
    @Bind(R.id.right_arrow) ImageView rightArrow;
    @Bind(R.id.table_month) TextView monthTV;
    @BindDimen(R.dimen.day_text_size) float dayTextSize;
    @BindDimen(R.dimen.today_text_size) float todayTextSize;

    private CalendarImpl calendar;
    private Resources res;
    private String packageName;
    private int textColor;
    private int weakTextColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        textColor = Helpers.adjustAlpha(Color.BLACK, Constants.HIGH_ALPHA);
        weakTextColor = Helpers.adjustAlpha(Color.BLACK, Constants.LOW_ALPHA);
        leftArrow.getDrawable().mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);
        rightArrow.getDrawable().mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP);

        res = getResources();
        packageName = getPackageName();
        dayTextSize /= res.getDisplayMetrics().density;
        todayTextSize /= res.getDisplayMetrics().density;
        setupLabels();

        calendar = new CalendarImpl(this);
        calendar.updateCalendar(new DateTime());
    }

    private void updateDays(List<Day> days) {
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

    @OnClick(R.id.table_month)
    public void pickMonth() {
        final MyDatePickerDialog dialog = new MyDatePickerDialog();
        final Bundle bundle = new Bundle();
        bundle.putString(Constants.DATE, calendar.getTargetDate().toString());
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "datepicker");
    }

    @Override
    public void onDatePicked(DateTime dateTime) {
        calendar.updateCalendar(dateTime);
    }

    @Override
    public void updateCalendar(String month, List<Day> days) {
        updateMonth(month);
        updateDays(days);
    }

    private void updateMonth(String month) {
        monthTV.setText(month);
    }

    private void setupLabels() {
        for (int i = 0; i < 7; i++) {
            final TextView dayTV = (TextView) findViewById(res.getIdentifier("label_" + i, "id", packageName));
            dayTV.setTextSize(dayTextSize);
            dayTV.setTextColor(weakTextColor);
        }
    }
}
