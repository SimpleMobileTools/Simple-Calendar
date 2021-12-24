package com.simplemobiletools.calendar.pro.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.simplemobiletools.calendar.pro.adapters.EventListWidgetAdapter

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) = EventListWidgetAdapter(applicationContext, intent)
}
