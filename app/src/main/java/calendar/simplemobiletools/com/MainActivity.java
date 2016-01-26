package calendar.simplemobiletools.com;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.widget.TableLayout;
import android.widget.TableRow;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TableLayout tableHolder = (TableLayout) findViewById(R.id.table_holder);
        LayoutInflater inflater = getLayoutInflater();
        TableRow row = (TableRow) inflater.inflate(R.layout.table_row, tableHolder, false);
        tableHolder.addView(row);
    }
}
