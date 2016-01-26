package calendar.simplemobiletools.com;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TableLayout tableHolder = (TableLayout) findViewById(R.id.table_holder);
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < 5; i++) {
            final TableRow row = (TableRow) inflater.inflate(R.layout.table_row, tableHolder, false);
            for (int j = 1; j < 8; j++) {
                final TextView day = (TextView) inflater.inflate(R.layout.table_day, row, false);
                day.setText("" + (i * 7 + j));
                row.addView(day);
            }

            tableHolder.addView(row);
        }
    }
}
