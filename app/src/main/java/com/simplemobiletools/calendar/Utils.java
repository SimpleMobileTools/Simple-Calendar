package com.simplemobiletools.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.SystemClock;
import android.widget.Toast;

import com.simplemobiletools.calendar.helpers.Formatter;
import com.simplemobiletools.calendar.models.Event;
import com.simplemobiletools.calendar.receivers.NotificationReceiver;

import org.joda.time.DateTime;

public class Utils {

    public static int adjustAlpha(int color, float factor) {
        final int alpha = Math.round(Color.alpha(color) * factor);
        final int red = Color.red(color);
        final int green = Color.green(color);
        final int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static void showToast(Context context, int resId) {
        Toast.makeText(context, context.getResources().getString(resId), Toast.LENGTH_SHORT).show();
    }

    public static void scheduleNextEvent(Context context, Event event) {
        int startTS = event.getStartTS() - event.getReminderMinutes() * 60;
        int newTS = startTS;
        if (event.getRepeatInterval() == Constants.DAY || event.getRepeatInterval() == Constants.WEEK || event.getRepeatInterval() == Constants.BIWEEK) {
            while (startTS < System.currentTimeMillis() / 1000 + 5) {
                startTS += event.getRepeatInterval();
            }
            newTS = startTS;
        } else if (event.getRepeatInterval() == Constants.MONTH) {
            newTS = getNewTS(startTS, true);
        } else if (event.getRepeatInterval() == Constants.YEAR) {
            newTS = getNewTS(startTS, false);
        }

        if (newTS != 0)
            Utils.scheduleEventIn(context, newTS, event);
    }

    private static int getNewTS(int ts, boolean isMonthly) {
        DateTime dateTime = Formatter.INSTANCE.getDateTimeFromTS(ts);
        while (dateTime.isBeforeNow()) {
            dateTime = isMonthly ? dateTime.plusMonths(1) : dateTime.plusYears(1);
        }
        return (int) (dateTime.getMillis() / 1000);
    }

    public static void scheduleNotification(Context context, Event event) {
        if (event.getReminderMinutes() == -1)
            return;

        scheduleNextEvent(context, event);
    }

    private static void scheduleEventIn(Context context, int notifTS, Event event) {
        final long delayFromNow = (long) notifTS * 1000 - System.currentTimeMillis();
        if (delayFromNow < 0)
            return;

        final long notifInMs = SystemClock.elapsedRealtime() + delayFromNow;
        final PendingIntent pendingIntent = getNotificationIntent(context, event.getId());
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, notifInMs, pendingIntent);
    }

    private static PendingIntent getNotificationIntent(Context context, int eventId) {
        final Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.Companion.getEVENT_ID(), eventId);
        return PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static int[] getLetterIDs() {
        return new int[]{R.string.sunday_letter, R.string.monday_letter, R.string.tuesday_letter, R.string.wednesday_letter,
                R.string.thursday_letter, R.string.friday_letter, R.string.saturday_letter};
    }
}
