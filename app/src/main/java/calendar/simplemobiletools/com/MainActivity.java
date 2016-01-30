package calendar.simplemobiletools.com;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.List;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements MyDatePickerDialog.DatePickedListener, Calendar {
    public static final String DATE = "date";

    @Bind(R.id.left_arrow) ImageView leftArrow;
    @Bind(R.id.right_arrow) ImageView rightArrow;
    @Bind(R.id.table_month) TextView monthTV;
    @BindColor(R.color.darkGrey) int darkGrey;
    @BindColor(R.color.lightGrey) int lightGrey;
    @BindDimen(R.dimen.day_text_size) float dayTextSize;
    @BindDimen(R.dimen.today_text_size) float todayTextSize;

    private CalendarImpl calendar;
    private Resources res;
    private String packageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        leftArrow.getDrawable().mutate().setColorFilter(darkGrey, PorterDuff.Mode.SRC_ATOP);
        rightArrow.getDrawable().mutate().setColorFilter(darkGrey, PorterDuff.Mode.SRC_ATOP);

        res = getResources();
        packageName = getPackageName();
        dayTextSize /= getResources().getDisplayMetrics().density;
        todayTextSize /= getResources().getDisplayMetrics().density;

        DateTime now = new DateTime();
        calendar = new CalendarImpl(this, now);
        calendar.updateCalendar(now);

        setupLabelSizes();
    }

    private void fillCalendar(List<Day> days) {
        final int len = days.size();

        for (int i = 0; i < len; i++) {
            final Day day = days.get(i);
            final TextView dayTV = (TextView) findViewById(res.getIdentifier("day_" + i, "id", packageName));
            dayTV.setText(String.valueOf(day.getValue()));

            if (day.getIsThisMonth()) {
                dayTV.setTextColor(darkGrey);
            } else {
                dayTV.setTextColor(lightGrey);
            }

            if (day.getIsToday()) {
                dayTV.setTextSize(todayTextSize);
            } else {
                dayTV.setTextSize(dayTextSize);
            }
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
        bundle.putString(DATE, calendar.getTargetDate().toString());
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "datepicker");
    }

    @Override
    public void onDatePicked(DateTime dateTime) {
        calendar.updateCalendar(dateTime);
    }

    @Override
    public void updateDays(List<Day> days) {
        fillCalendar(days);
    }

    @Override
    public void updateMonth(String month) {
        monthTV.setText(month);
    }

    private void setupLabelSizes() {
        for (int i = 0; i < 7; i++) {
            final TextView dayTV = (TextView) findViewById(res.getIdentifier("label_" + i, "id", packageName));
            dayTV.setTextSize(dayTextSize);
        }
    }
}
