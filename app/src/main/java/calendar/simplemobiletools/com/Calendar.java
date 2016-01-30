package calendar.simplemobiletools.com;

import java.util.List;

public interface Calendar {
    void updateDays(List<Day> days);

    void updateMonth(String month);
}
