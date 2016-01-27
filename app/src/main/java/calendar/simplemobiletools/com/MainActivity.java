package calendar.simplemobiletools.com;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.text.DateFormatSymbols;

public class MainActivity extends AppCompatActivity {
    private DateTime targetDate;
    private int grey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        grey = getResources().getColor(R.color.darkGrey);

        final ImageView leftArrow = (ImageView) findViewById(R.id.left_arrow);
        leftArrow.getDrawable().mutate().setColorFilter(grey, PorterDuff.Mode.SRC_ATOP);

        final ImageView rightArrow = (ImageView) findViewById(R.id.right_arrow);
        rightArrow.getDrawable().mutate().setColorFilter(grey, PorterDuff.Mode.SRC_ATOP);

        final TextView monthTV = (TextView) findViewById(R.id.table_month);

        final TableLayout tableHolder = (TableLayout) findViewById(R.id.table_holder);
        final LayoutInflater inflater = getLayoutInflater();

        final DateTime now = new DateTime();
        targetDate = now;
        monthTV.setText(getMonthName());
        final int currMonthDays = now.dayOfMonth().getMaximumValue();

        final int firstDayIndex = now.withDayOfMonth(1).getDayOfWeek() - 1;
        final int prevMonthDays = now.minusMonths(1).dayOfMonth().getMaximumValue();
        final int prevMonthStart = prevMonthDays - firstDayIndex + 1;

        int cur = 0;
        int thisMonthDays = 1;
        int nextMonthsDay = 1;

        for (int i = 0; i < 6; i++) {
            final TableRow row = (TableRow) inflater.inflate(R.layout.table_row, tableHolder, false);
            for (int j = 0; j < 7; j++) {
                final TextView day = (TextView) inflater.inflate(R.layout.table_day, row, false);
                int currDate = thisMonthDays;
                if (cur < firstDayIndex) {
                    currDate = prevMonthStart + cur;
                } else if (currDate <= currMonthDays) {
                    thisMonthDays++;
                    day.setTextColor(grey);
                } else {
                    currDate = nextMonthsDay++;
                }

                day.setText(String.valueOf(currDate));
                row.addView(day);
                cur++;
            }

            tableHolder.addView(row);
        }
    }

    private String getMonthName() {
        final String[] months = new DateFormatSymbols().getMonths();
        return months[targetDate.getMonthOfYear() - 1];
    }
}
