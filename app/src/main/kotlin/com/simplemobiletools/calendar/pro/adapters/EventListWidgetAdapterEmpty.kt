package com.simplemobiletools.calendar.pro.adapters

import android.content.Context
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simplemobiletools.calendar.pro.R

class EventListWidgetAdapterEmpty(val context: Context) : RemoteViewsService.RemoteViewsFactory {
    override fun getViewAt(position: Int) = RemoteViews(context.packageName, R.layout.event_list_section_widget)

    override fun getLoadingView() = null

    override fun getViewTypeCount() = 1

    override fun onCreate() {}

    override fun getItemId(position: Int) = 0L

    override fun onDataSetChanged() {}

    override fun hasStableIds() = true

    override fun getCount() = 0

    override fun onDestroy() {}
}
