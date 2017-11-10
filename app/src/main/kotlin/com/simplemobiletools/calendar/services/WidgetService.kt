package com.simplemobiletools.calendar.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.simplemobiletools.calendar.adapters.EventListWidgetAdapter

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapter(applicationContext)
}
