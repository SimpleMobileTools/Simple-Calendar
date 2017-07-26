package com.simplemobiletools.calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.asynctasks.FetchGoogleEventsTask

class GoogleSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, arg1: Intent) {
        FetchGoogleEventsTask(context).execute()
    }
}
