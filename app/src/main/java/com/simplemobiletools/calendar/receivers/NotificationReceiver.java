package com.simplemobiletools.calendar.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import com.simplemobiletools.calendar.Constants;
import com.simplemobiletools.calendar.DBHelper;
import com.simplemobiletools.calendar.Formatter;
import com.simplemobiletools.calendar.R;
import com.simplemobiletools.calendar.activities.EventActivity;
import com.simplemobiletools.calendar.models.Event;

public class NotificationReceiver extends BroadcastReceiver {
    public static String EVENT_ID = "event_id";

    public void onReceive(Context context, Intent intent) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int id = intent.getIntExtra(EVENT_ID, -1);
        if (id == -1)
            return;

        final Event event = DBHelper.newInstance(context, null).getEvent(id);
        if (event == null || event.getReminderMinutes() == -1)
            return;

        final PendingIntent pendingIntent = getPendingIntent(context, event);
        final String startTime = Formatter.getTime(event.getStartTS());
        final String endTime = Formatter.getTime(event.getEndTS());
        final String title = event.getTitle();
        final Notification notification = getNotification(context, pendingIntent, startTime + " - " + endTime + " " + title);
        notificationManager.notify(id, notification);
    }

    private PendingIntent getPendingIntent(Context context, Event event) {
        final Intent intent = new Intent(context, EventActivity.class);
        intent.putExtra(Constants.EVENT, event);
        return PendingIntent.getActivity(context, event.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification getNotification(Context context, PendingIntent pendingIntent, String content) {
        final Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        final Notification.Builder builder = new Notification.Builder(context);
        builder.setContentTitle(context.getResources().getString(R.string.app_name));
        builder.setContentText(content);
        builder.setSmallIcon(R.mipmap.calendar);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.setSound(soundUri);
        return builder.build();
    }
}
