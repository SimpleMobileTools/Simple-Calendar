package com.simplemobiletools.calendar.adapters

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.R.id.event_item_holder
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import com.simplemobiletools.commons.extensions.adjustAlpha
import org.joda.time.DateTime
import java.util.*
import kotlin.comparisons.compareBy

class EventListWidgetAdapter(val context: Context, val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    val ITEM_EVENT = 0
    val ITEM_HEADER = 1

    val appWidgetId: Int
    var events: List<ListItem>
    val textColor: Int

    init {
        textColor = Config.newInstance(context).widgetTextColor.adjustAlpha(HIGH_ALPHA)
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
                setTextViewText(R.id.event_item_start, Formatter.getTimeFromTS(context, item.startTS))
                setTextViewText(R.id.event_item_end, Formatter.getTimeFromTS(context, item.endTS))

                setInt(R.id.event_item_title, "setTextColor", textColor)
                setInt(R.id.event_item_description, "setTextColor", textColor)
                setInt(R.id.event_item_start, "setTextColor", textColor)
                setInt(R.id.event_item_end, "setTextColor", textColor)

                val intent = Intent()
                intent.putExtra(EVENT_ID, item.id)
                setOnClickFillInIntent(event_item_holder, intent)
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
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun onDataSetChanged() {
        val fromTS = (DateTime().millis / 1000).toInt()
        val toTS = (DateTime().plusMonths(6).millis / 1000).toInt()
        DBHelper(context).getEventsInBackground(fromTS, toTS, object : DBHelper.GetEventsListener {
            override fun gotEvents(events: MutableList<Event>) {
                val listItems = ArrayList<ListItem>(events.size)
                val sorted = events.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { it.description }))
                val sublist = sorted.subList(0, Math.min(sorted.size, 50))
                var prevCode = ""
                sublist.forEach {
                    val code = Formatter.getDayCodeFromTS(it.startTS)
                    if (code != prevCode) {
                        val day = Formatter.getDayTitle(context, code)
                        listItems.add(ListSection(day))
                        prevCode = code
                    }
                    listItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description))
                }

                this@EventListWidgetAdapter.events = listItems
            }
        })
    }

    override fun hasStableIds() = true

    override fun getCount() = events.size

    override fun onDestroy() {
    }
}
