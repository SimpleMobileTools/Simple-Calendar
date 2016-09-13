package com.simplemobiletools.calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.SystemClock;
import android.widget.Toast;

import com.simplemobiletools.calendar.models.Event;
import com.simplemobiletools.calendar.receivers.NotificationReceiver;

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

    public static void scheduleNotification(Context context, Event event) {
        if (event.getReminderMinutes() == -1) {
            return;
        }

        scheduleEventIn(context, event.getStartTS(), event);
    }

    public static void scheduleEventIn(Context context, int notifTS, Event event) {
        final long delayFromNow = (long) notifTS * 1000 - event.getReminderMinutes() * 60000 - System.currentTimeMillis();
        if (delayFromNow < 0)
            return;

        final long notifInMs = SystemClock.elapsedRealtime() + delayFromNow;
        final PendingIntent pendingIntent = getNotificationIntent(context, event.getId());
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, notifInMs, pendingIntent);
    }

    public static PendingIntent getNotificationIntent(Context context, int eventId) {
        final Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(NotificationReceiver.EVENT_ID, eventId);
        return PendingIntent.getBroadcast(context, eventId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static int[] getLetterIDs() {
        return new int[]{R.string.sunday_letter, R.string.monday_letter, R.string.tuesday_letter, R.string.wednesday_letter,
                R.string.thursday_letter, R.string.friday_letter, R.string.saturday_letter};
    }
}
