package com.simplemobiletools.calendar.adapters

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.adjustAlpha
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.HIGH_ALPHA
import com.simplemobiletools.calendar.helpers.PREFS_KEY
import com.simplemobiletools.calendar.helpers.WIDGET_TEXT_COLOR
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import org.joda.time.DateTime
import java.util.*

class EventListWidgetAdapter(val context: Context, val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    val ITEM_EVENT = 0
    val ITEM_HEADER = 1

    val appWidgetId: Int
    var events: List<ListItem>
    val prefs: SharedPreferences
    val textColor: Int

    init {
        prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        textColor = prefs.getInt(WIDGET_TEXT_COLOR, Color.WHITE).adjustAlpha(HIGH_ALPHA)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        events = ArrayList<ListItem>()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val type = getItemViewType(position)
        val remoteView: RemoteViews
        if (type == ITEM_EVENT) {
            val item = events[position] as ListEvent
            remoteView = RemoteViews(context.packageName, R.layout.event_list_item_widget)
            remoteView.apply {
                setTextViewText(R.id.event_item_title, item.title)
                setTextViewText(R.id.event_item_description, item.description)
                setTextViewText(R.id.event_item_start, Formatter.getTime(item.startTS))
                setTextViewText(R.id.event_item_end, Formatter.getTime(item.endTS))

                setInt(R.id.event_item_title, "setTextColor", textColor)
                setInt(R.id.event_item_description, "setTextColor", textColor)
                setInt(R.id.event_item_start, "setTextColor", textColor)
                setInt(R.id.event_item_end, "setTextColor", textColor)
            }
        } else {
            val item = events[position] as ListSection
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_widget)
            remoteView.apply {
                setTextViewText(R.id.event_item_title, item.title)
                setInt(R.id.event_item_title, "setTextColor", textColor)
            }
        }

        return remoteView
    }

    fun getItemViewType(position: Int) = if (events[position] is ListEvent) ITEM_EVENT else ITEM_HEADER

    override fun getLoadingView() = null

    override fun getViewTypeCount() = 2

    override fun onCreate() {
        events = getListItems()
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun onDataSetChanged() {
    }

    override fun hasStableIds() = true

    override fun getCount() = events.size

    override fun onDestroy() {
    }

    private fun getListItems(): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>(10)
        var dateTime = DateTime.now().withTime(0, 0, 0, 0).plusDays(1)
        var code = Formatter.getDayCodeFromTS((dateTime.millis / 1000).toInt())
        var day = Formatter.getDayTitle(context, code)
        listItems.add(ListSection(day))

        var time = dateTime.withHourOfDay(7)
        listItems.add(ListEvent(1, (time.millis / 1000).toInt(), (time.plusMinutes(30).millis / 1000).toInt(), "Workout", "Leg day"))
        time = dateTime.withHourOfDay(8)
        listItems.add(ListEvent(2, (time.millis / 1000).toInt(), (time.plusHours(1).millis / 1000).toInt(), "Meeting with John", "In Rockstone Garden"))

        dateTime = dateTime.plusDays(1)
        code = Formatter.getDayCodeFromTS((dateTime.millis / 1000).toInt())
        day = Formatter.getDayTitle(context, code)
        listItems.add(ListSection(day))

        time = dateTime.withHourOfDay(13)
        listItems.add(ListEvent(3, (time.millis / 1000).toInt(), (time.plusHours(1).millis / 1000).toInt(), "Lunch with Mary", "In the Plaza"))
        time = dateTime.withHourOfDay(18)
        listItems.add(ListEvent(4, (time.millis / 1000).toInt(), (time.plusMinutes(10).millis / 1000).toInt(), "Coffee time", ""))

        return listItems
    }
}
