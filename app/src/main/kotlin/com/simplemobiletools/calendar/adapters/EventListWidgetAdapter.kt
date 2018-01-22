package com.simplemobiletools.calendar.adapters

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.R.id.event_item_holder
import com.simplemobiletools.calendar.R.id.event_section_title
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.DAY_CODE
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.calendar.helpers.EVENT_OCCURRENCE_TS
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import com.simplemobiletools.commons.extensions.getColoredBitmap
import com.simplemobiletools.commons.extensions.setText
import com.simplemobiletools.commons.extensions.setTextSize
import org.joda.time.DateTime
import java.util.*

class EventListWidgetAdapter(val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private val ITEM_EVENT = 0
    private val ITEM_HEADER = 1

    private val allDayString = context.resources.getString(R.string.all_day)
    private var events = ArrayList<ListItem>()
    private val textColor = context.config.widgetTextColor
    private val replaceDescription = context.config.replaceDescription
    private var mediumFontSize = context.config.getFontSize()

    override fun getViewAt(position: Int): RemoteViews? {
        val type = getItemViewType(position)
        val remoteView: RemoteViews

        if (type == ITEM_EVENT) {
            val item = events[position] as ListEvent
            remoteView = RemoteViews(context.packageName, R.layout.event_list_item_widget).apply {
                setText(R.id.event_section_title, item.title)
                setText(R.id.event_item_description, if (replaceDescription) item.location else item.description)
                setText(R.id.event_item_start, if (item.isAllDay) allDayString else Formatter.getTimeFromTS(context, item.startTS))
                setImageViewBitmap(R.id.event_item_color, context.resources.getColoredBitmap(R.drawable.monthly_event_dot, item.color))

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

                setTextColor(R.id.event_section_title, textColor)
                setTextColor(R.id.event_item_description, textColor)
                setTextColor(R.id.event_item_start, textColor)
                setTextColor(R.id.event_item_end, textColor)

                setTextSize(R.id.event_section_title, mediumFontSize)
                setTextSize(R.id.event_item_description, mediumFontSize)
                setTextSize(R.id.event_item_start, mediumFontSize)
                setTextSize(R.id.event_item_end, mediumFontSize)

                Intent().apply {
                    putExtra(EVENT_ID, item.id)
                    putExtra(EVENT_OCCURRENCE_TS, item.startTS)
                    setOnClickFillInIntent(event_item_holder, this)
                }
            }
        } else {
            val item = events[position] as ListSection
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_widget).apply {
                setTextColor(R.id.event_section_title, textColor)
                setTextSize(R.id.event_section_title, mediumFontSize)
                setText(R.id.event_section_title, item.title)

                Intent().apply {
                    putExtra(DAY_CODE, item.code)
                    setOnClickFillInIntent(event_section_title, this)
                }
            }
        }

        return remoteView
    }

    private fun getItemViewType(position: Int) = if (events[position] is ListEvent) ITEM_EVENT else ITEM_HEADER

    override fun getLoadingView() = null

    override fun getViewTypeCount() = 2

    override fun onCreate() {}

    override fun getItemId(position: Int) = position.toLong()

    override fun onDataSetChanged() {
        mediumFontSize = context.config.getFontSize()
        val fromTS = DateTime().seconds() - context.config.displayPastEvents * 60
        val toTS = DateTime().plusYears(1).seconds()
        context.dbHelper.getEventsInBackground(fromTS, toTS) {
            val listItems = ArrayList<ListItem>(it.size)
            val replaceDescription = context.config.replaceDescription
            val sorted = it.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { if (replaceDescription) it.location else it.description }))
            val sublist = sorted.subList(0, Math.min(sorted.size, 100))
            var prevCode = ""
            sublist.forEach {
                val code = Formatter.getDayCodeFromTS(it.startTS)
                if (code != prevCode) {
                    val day = Formatter.getDayTitle(context, code)
                    listItems.add(ListSection(day, code))
                    prevCode = code
                }
                listItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description, it.getIsAllDay(), it.color, it.location))
            }

            this@EventListWidgetAdapter.events = listItems
        }
    }

    override fun hasStableIds() = true

    override fun getCount() = events.size

    override fun onDestroy() {}
}
