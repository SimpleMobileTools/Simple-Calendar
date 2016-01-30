package calendar.simplemobiletools.com;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.text.DateFormatSymbols;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements MyDatePickerDialog.DatePickedListener {
    public static final String DATE = "date";
    private static final String DATE_PATTERN = "ddMMYYYY";
    private static final String YEAR_PATTERN = "YYYY";

    @Bind(R.id.left_arrow) ImageView leftArrow;
    @Bind(R.id.right_arrow) ImageView rightArrow;
    @Bind(R.id.table_month) TextView monthTV;
    @Bind(R.id.table_holder) LinearLayout tableHolder;
    @BindColor(R.color.darkGrey) int darkGrey;
    @BindColor(R.color.lightGrey) int lightGrey;
    @BindDimen(R.dimen.day_text_size) float dayTextSize;
    @BindDimen(R.dimen.today_text_size) float todayTextSize;

    private DateTime targetDate;
    private String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        leftArrow.getDrawable().mutate().setColorFilter(darkGrey, PorterDuff.Mode.SRC_ATOP);
        rightArrow.getDrawable().mutate().setColorFilter(darkGrey, PorterDuff.Mode.SRC_ATOP);
        targetDate = new DateTime();
        today = targetDate.toString(DATE_PATTERN);

        createDays();
        fillCalendar();
    }

    private void createDays() {
        final String[] days = {"M", "T", "W", "T", "F", "S", "S"};
        final LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < 7; i++) {
            final LinearLayout row = (LinearLayout) inflater.inflate(R.layout.table_row, tableHolder, false);
            tableHolder.addView(row);
            for (int j = 0; j < 7; j++) {
                final TextView day = (TextView) inflater.inflate(R.layout.table_day, row, false);
                if (i == 0) {
                    day.setText(days[j]);
                    day.setTextSize(dayTextSize);
                }
                row.addView(day);
            }
        }
    }

    private void fillCalendar() {
        monthTV.setText(getMonthName());
        final int currMonthDays = targetDate.dayOfMonth().getMaximumValue();
        final int firstDayIndex = targetDate.withDayOfMonth(1).getDayOfWeek() - 1;
        final int prevMonthDays = targetDate.minusMonths(1).dayOfMonth().getMaximumValue();
        final int prevMonthStart = prevMonthDays - firstDayIndex + 1;

        int cur = 0;
        int thisMonthDays = 1;
        int nextMonthsDay = 1;

        for (int i = 1; i < 7; i++) {
            final LinearLayout row = (LinearLayout) tableHolder.getChildAt(i);
            for (int j = 0; j < 7; j++) {
                final TextView day = (TextView) row.getChildAt(j);
                day.setTextSize(dayTextSize);

                int currDate = thisMonthDays;
                if (cur < firstDayIndex) {
                    currDate = prevMonthStart + cur;
                    day.setTextColor(lightGrey);
                } else if (currDate <= currMonthDays) {
                    if (targetDate.withDayOfMonth(thisMonthDays).toString(DATE_PATTERN).equals(today)) {
                        day.setTextSize(todayTextSize);
                    }

                    thisMonthDays++;
                    day.setTextColor(darkGrey);
                } else {
                    currDate = nextMonthsDay++;
                    day.setTextColor(lightGrey);
                }

                day.setText(String.valueOf(currDate));
                cur++;
            }
        }
    }

    @OnClick(R.id.left_arrow)
    public void leftArrowClicked() {
        targetDate = targetDate.minusMonths(1);
        fillCalendar();
    }

    @OnClick(R.id.right_arrow)
    public void rightArrowClicked() {
        targetDate = targetDate.plusMonths(1);
        fillCalendar();
    }

    @OnClick(R.id.table_month)
    public void pickMonth() {
        final MyDatePickerDialog dialog = new MyDatePickerDialog();
        final Bundle bundle = new Bundle();
        bundle.putString(DATE, targetDate.toString());
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "datepicker");
    }

    private String getMonthName() {
        final String[] months = new DateFormatSymbols().getMonths();
        final StringBuilder sb = new StringBuilder();
        sb.append(months[targetDate.getMonthOfYear() - 1]);

        final String targetYear = targetDate.toString(YEAR_PATTERN);
        if (!targetYear.equals(new DateTime().toString(YEAR_PATTERN))) {
            sb.append(" ");
            sb.append(targetYear);
        }
        return sb.toString();
    }

    @Override
    public void onDatePicked(DateTime dateTime) {
        targetDate = dateTime;
        fillCalendar();
    }
}
