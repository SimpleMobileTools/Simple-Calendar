package com.simplemobiletools.calendar.pro.adapters

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.R.id.event_item_holder
import com.simplemobiletools.calendar.pro.R.id.event_section_title
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.extensions.getWidgetFontSize
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.ListEvent
import com.simplemobiletools.calendar.pro.models.ListItem
import com.simplemobiletools.calendar.pro.models.ListSection
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.setBackgroundColor
import com.simplemobiletools.commons.extensions.setText
import com.simplemobiletools.commons.extensions.setTextSize
import org.joda.time.DateTime
import java.util.*

class EventListWidgetAdapter(val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private val ITEM_EVENT = 0
    private val ITEM_HEADER = 1

    private val allDayString = context.resources.getString(R.string.all_day)
    private var events = ArrayList<ListItem>()
    private var textColor = context.config.widgetTextColor
    private var weakTextColor = textColor.adjustAlpha(LOW_ALPHA)
    private val replaceDescription = context.config.replaceDescription
    private val dimPastEvents = context.config.dimPastEvents
    private var mediumFontSize = context.getWidgetFontSize()

    override fun getViewAt(position: Int): RemoteViews? {
        val type = getItemViewType(position)
        val remoteView: RemoteViews

        if (type == ITEM_EVENT) {
            val event = events[position] as ListEvent
            val layout = getItemViewLayout(event)
            remoteView = RemoteViews(context.packageName, layout)
            setupListEvent(remoteView, event)
        } else {
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_widget)
            val section = events.getOrNull(position) as? ListSection
            if (section != null) {
                setupListSection(remoteView, section)
            }
        }

        return remoteView
    }

    private fun getItemViewLayout(event: ListEvent): Int {
        val detailField = if (replaceDescription) event.location else event.description
        return if (detailField.isNotEmpty()) {
            R.layout.event_list_item_widget
        } else if (event.startTS == event.endTS) {
            R.layout.event_list_item_widget_simple
        } else if (event.isAllDay) {
            val startCode = Formatter.getDayCodeFromTS(event.startTS)
            val endCode = Formatter.getDayCodeFromTS(event.endTS)
            if (startCode == endCode) {
                R.layout.event_list_item_widget_simple
            } else {
                R.layout.event_list_item_widget
            }
        } else {
            R.layout.event_list_item_widget
        }
    }

    private fun setupListEvent(remoteView: RemoteViews, item: ListEvent) {
        var curTextColor = textColor
        remoteView.apply {
            setText(R.id.event_item_title, item.title)
            setText(R.id.event_item_description, if (replaceDescription) item.location else item.description)
            setText(R.id.event_item_start, if (item.isAllDay) allDayString else Formatter.getTimeFromTS(context, item.startTS))
            setBackgroundColor(R.id.event_item_color_bar, item.color)

            if (item.startTS == item.endTS) {
                setViewVisibility(R.id.event_item_end, View.INVISIBLE)
            } else {
                setViewVisibility(R.id.event_item_end, View.VISIBLE)
                var endString = Formatter.getTimeFromTS(context, item.endTS)
                val startCode = Formatter.getDayCodeFromTS(item.startTS)
                val endCode = Formatter.getDayCodeFromTS(item.endTS)

                if (startCode != endCode) {
                    if (item.isAllDay) {
                        endString = Formatter.getDateFromCode(context, endCode, true)
                    } else {
                        endString += " (${Formatter.getDateFromCode(context, endCode, true)})"
                    }
                } else if (item.isAllDay) {
                    setViewVisibility(R.id.event_item_end, View.INVISIBLE)
                }
                setText(R.id.event_item_end, endString)
            }

            if (dimPastEvents && item.isPastEvent) {
                curTextColor = weakTextColor
            }

            setTextColor(R.id.event_item_title, curTextColor)
            setTextColor(R.id.event_item_description, curTextColor)
            setTextColor(R.id.event_item_start, curTextColor)
            setTextColor(R.id.event_item_end, curTextColor)

            setTextSize(R.id.event_item_title, mediumFontSize)
            setTextSize(R.id.event_item_description, mediumFontSize)
            setTextSize(R.id.event_item_start, mediumFontSize)
            setTextSize(R.id.event_item_end, mediumFontSize)

            Intent().apply {
                putExtra(EVENT_ID, item.id)
                putExtra(EVENT_OCCURRENCE_TS, item.startTS)
                setOnClickFillInIntent(event_item_holder, this)
            }
        }
    }

    private fun setupListSection(remoteView: RemoteViews, item: ListSection) {
        var curTextColor = textColor
        if (dimPastEvents && item.isPastSection) {
            curTextColor = weakTextColor
        }

        remoteView.apply {
            setTextColor(event_section_title, curTextColor)
            setTextSize(event_section_title, mediumFontSize)
            setText(event_section_title, item.title)

            Intent().apply {
                putExtra(DAY_CODE, item.code)
                putExtra(VIEW_TO_OPEN, context.config.listWidgetViewToOpen)
                setOnClickFillInIntent(event_section_title, this)
            }
        }
    }

    private fun getItemViewType(position: Int) = if (events.getOrNull(position) is ListEvent) ITEM_EVENT else ITEM_HEADER

    override fun getLoadingView() = null

    override fun getViewTypeCount() = 3

    override fun onCreate() {}

    override fun getItemId(position: Int) = position.toLong()

    override fun onDataSetChanged() {
        textColor = context.config.widgetTextColor
        weakTextColor = textColor.adjustAlpha(LOW_ALPHA)
        mediumFontSize = context.getWidgetFontSize()
        val fromTS = DateTime().seconds() - context.config.displayPastEvents * 60
        val toTS = DateTime().plusYears(1).seconds()
        context.eventsHelper.getEventsSync(fromTS, toTS, applyTypeFilter = true) {
            val listItems = ArrayList<ListItem>(it.size)
            val replaceDescription = context.config.replaceDescription
            val sorted = it.sortedWith(compareBy<Event> {
                if (it.getIsAllDay()) {
                    Formatter.getDayStartTS(Formatter.getDayCodeFromTS(it.startTS)) - 1
                } else {
                    it.startTS
                }
            }.thenBy {
                if (it.getIsAllDay()) {
                    Formatter.getDayEndTS(Formatter.getDayCodeFromTS(it.endTS))
                } else {
                    it.endTS
                }
            }.thenBy { it.title }.thenBy { if (replaceDescription) it.location else it.description })

            var prevCode = ""
            val now = getNowSeconds()
            val today = Formatter.getDayTitle(context, Formatter.getDayCodeFromTS(now))

            sorted.forEach {
                val code = Formatter.getDayCodeFromTS(it.startTS)
                if (code != prevCode) {
                    val day = Formatter.getDayTitle(context, code)
                    val isToday = day == today
                    val listSection = ListSection(day, code, isToday, !isToday && it.startTS < now)
                    listItems.add(listSection)
                    prevCode = code
                }

                val listEvent = ListEvent(it.id!!, it.startTS, it.endTS, it.title, it.description, it.getIsAllDay(), it.color, it.location, it.isPastEvent, it.repeatInterval > 0)
                listItems.add(listEvent)
            }

            this@EventListWidgetAdapter.events = listItems
        }
    }

    override fun hasStableIds() = true

    override fun getCount() = events.size

    override fun onDestroy() {}
}
