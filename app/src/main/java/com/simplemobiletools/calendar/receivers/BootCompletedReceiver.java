package com.simplemobiletools.calendar.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.simplemobiletools.calendar.DBHelper;
import com.simplemobiletools.calendar.Utils;
import com.simplemobiletools.calendar.models.Event;

import java.util.List;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {
        final List<Event> events = DBHelper.newInstance(context, null).getEventsAtReboot();
        for (Event event : events) {
            Utils.scheduleNotification(context, event);
        }
    }
}
