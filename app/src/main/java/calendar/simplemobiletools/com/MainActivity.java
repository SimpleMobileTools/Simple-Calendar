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

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    @Bind(R.id.left_arrow) ImageView leftArrow;
    @Bind(R.id.right_arrow) ImageView rightArrow;
    @Bind(R.id.table_month) TextView monthTV;
    @Bind(R.id.table_holder) TableLayout tableHolder;
    @BindColor(R.color.darkGrey) int grey;

    private DateTime targetDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        leftArrow.getDrawable().mutate().setColorFilter(grey, PorterDuff.Mode.SRC_ATOP);
        rightArrow.getDrawable().mutate().setColorFilter(grey, PorterDuff.Mode.SRC_ATOP);

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

    @OnClick(R.id.left_arrow)
    public void leftArrowClicked() {

    }

    @OnClick(R.id.right_arrow)
    public void rightArrowClicked() {

    }

    private String getMonthName() {
        final String[] months = new DateFormatSymbols().getMonths();
        return months[targetDate.getMonthOfYear() - 1];
    }
}
