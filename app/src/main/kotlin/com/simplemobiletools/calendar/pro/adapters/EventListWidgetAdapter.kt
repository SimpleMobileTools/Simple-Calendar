package com.simplemobiletools.calendar.pro.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.models.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import org.joda.time.DateTime

class EventListWidgetAdapter(val context: Context, val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val ITEM_EVENT = 0
    private val ITEM_SECTION_DAY = 1
    private val ITEM_SECTION_MONTH = 2

    private val allDayString = context.resources.getString(R.string.all_day)
    private var events = ArrayList<ListItem>()
    private var textColor = context.config.widgetTextColor
    private var weakTextColor = textColor.adjustAlpha(MEDIUM_ALPHA)
    private var displayDescription = context.config.displayDescription
    private var replaceDescription = context.config.replaceDescription
    private var dimPastEvents = context.config.dimPastEvents
    private var dimCompletedTasks = context.config.dimCompletedTasks
    private var mediumFontSize = context.getWidgetFontSize()
    private var smallMargin = context.resources.getDimension(com.simplemobiletools.commons.R.dimen.small_margin).toInt()
    private var normalMargin = context.resources.getDimension(com.simplemobiletools.commons.R.dimen.normal_margin).toInt()

    init {
        initConfigValues()
    }

    private fun initConfigValues() {
        textColor = context.config.widgetTextColor
        weakTextColor = textColor.adjustAlpha(MEDIUM_ALPHA)
        displayDescription = context.config.displayDescription
        replaceDescription = context.config.replaceDescription
        dimPastEvents = context.config.dimPastEvents
        dimCompletedTasks = context.config.dimCompletedTasks
        mediumFontSize = context.getWidgetFontSize()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val type = getItemViewType(position)
        val remoteView: RemoteViews

        if (type == ITEM_EVENT) {
            val event = events[position] as ListEvent
            val layout = R.layout.event_list_item_widget
            remoteView = RemoteViews(context.packageName, layout)
            setupListEvent(remoteView, event)
        } else if (type == ITEM_SECTION_DAY) {
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_day_widget)
            val section = events.getOrNull(position) as? ListSectionDay
            if (section != null) {
                setupListSectionDay(remoteView, section)
            }
        } else {
            remoteView = RemoteViews(context.packageName, R.layout.event_list_section_month_widget)
            val section = events.getOrNull(position) as? ListSectionMonth
            if (section != null) {
                setupListSectionMonth(remoteView, section)
            }
        }

        return remoteView
    }

    private fun setupListEvent(remoteView: RemoteViews, item: ListEvent) {
        var curTextColor = textColor
        remoteView.apply {
            setBackgroundColor(R.id.event_item_color_bar, item.color)
            setText(R.id.event_item_title, item.title)

            var timeText = if (item.isAllDay) allDayString else Formatter.getTimeFromTS(context, item.startTS)
            val endText = Formatter.getTimeFromTS(context, item.endTS)
            if (item.startTS != item.endTS) {
                if (!item.isAllDay) {
                    timeText += " - $endText"
                }

                val startCode = Formatter.getDayCodeFromTS(item.startTS)
                val endCode = Formatter.getDayCodeFromTS(item.endTS)
                if (startCode != endCode) {
                    timeText += " (${Formatter.getDateDayTitle(endCode)})"
                }
            }

            setText(R.id.event_item_time, timeText)

            // we cannot change the event_item_color_bar rules dynamically, so do it like this
            val descriptionText = if (replaceDescription) item.location else item.description.replace("\n", " ")
            if (displayDescription && descriptionText.isNotEmpty()) {
                setText(R.id.event_item_time, "$timeText\n$descriptionText")
            }

            if (item.isTask && item.isTaskCompleted && dimCompletedTasks || dimPastEvents && item.isPastEvent) {
                curTextColor = weakTextColor
            }

            setTextColor(R.id.event_item_title, curTextColor)
            setTextColor(R.id.event_item_time, curTextColor)

            setTextSize(R.id.event_item_title, mediumFontSize)
            setTextSize(R.id.event_item_time, mediumFontSize)

            setVisibleIf(R.id.event_item_task_image, item.isTask)
            applyColorFilter(R.id.event_item_task_image, curTextColor)

            if (item.isTask) {
                setViewPadding(R.id.event_item_title, 0, 0, smallMargin, 0)
            } else {
                setViewPadding(R.id.event_item_title, normalMargin, 0, smallMargin, 0)
            }

            if (item.shouldStrikeThrough()) {
                setInt(R.id.event_item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG or Paint.STRIKE_THRU_TEXT_FLAG)
            } else {
                setInt(R.id.event_item_title, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
            }

            Intent().apply {
                putExtra(EVENT_ID, item.id)
                putExtra(EVENT_OCCURRENCE_TS, item.startTS)
                setOnClickFillInIntent(R.id.event_item_holder, this)
            }
        }
    }

    private fun setupListSectionDay(remoteView: RemoteViews, item: ListSectionDay) {
        var curTextColor = textColor
        if (dimPastEvents && item.isPastSection) {
            curTextColor = weakTextColor
        }

        remoteView.apply {
            setTextColor(R.id.event_section_title, curTextColor)
            setTextSize(R.id.event_section_title, mediumFontSize - 3f)
            setText(R.id.event_section_title, item.title)

            Intent().apply {
                putExtra(DAY_CODE, item.code)
                putExtra(VIEW_TO_OPEN, context.config.listWidgetViewToOpen)
                setOnClickFillInIntent(R.id.event_section_title, this)
            }
        }
    }

    private fun setupListSectionMonth(remoteView: RemoteViews, item: ListSectionMonth) {
        val curTextColor = textColor
        remoteView.apply {
            setTextColor(R.id.event_section_title, curTextColor)
            setTextSize(R.id.event_section_title, mediumFontSize)
            setText(R.id.event_section_title, item.title)
        }
    }

    private fun getItemViewType(position: Int) = when {
        events.getOrNull(position) is ListEvent -> ITEM_EVENT
        events.getOrNull(position) is ListSectionDay -> ITEM_SECTION_DAY
        else -> ITEM_SECTION_MONTH
    }

    override fun getLoadingView() = null

    override fun getViewTypeCount() = 3

    override fun onCreate() {}

    override fun getItemId(position: Int) = position.toLong()

    override fun onDataSetChanged() {
        initConfigValues()
        val period = intent.getIntExtra(EVENT_LIST_PERIOD, 0)
        val currentDate = DateTime()
        val fromTS = currentDate.seconds() - context.config.displayPastEvents * 60
        val toTS = when (period) {
            0 -> currentDate.plusYears(1).seconds()
            EVENT_PERIOD_TODAY -> currentDate.withTime(23, 59, 59, 999).seconds()
            else -> currentDate.plusSeconds(period).seconds()
        }
        context.eventsHelper.getEventsSync(fromTS, toTS, applyTypeFilter = true) {
            val listItems = ArrayList<ListItem>(it.size)
            val replaceDescription = context.config.replaceDescription
            val sorted = it.sortedWith(compareBy<Event> { event ->
                if (event.getIsAllDay()) {
                    Formatter.getDayStartTS(Formatter.getDayCodeFromTS(event.startTS)) - 1
                } else {
                    event.startTS
                }
            }.thenBy { event ->
                if (event.getIsAllDay()) {
                    Formatter.getDayEndTS(Formatter.getDayCodeFromTS(event.endTS))
                } else {
                    event.endTS
                }
            }.thenBy { event -> event.title }.thenBy { event -> if (replaceDescription) event.location else event.description })

            var prevCode = ""
            var prevMonthLabel = ""
            val now = getNowSeconds()
            val today = Formatter.getDayTitle(context, Formatter.getDayCodeFromTS(now))

            sorted.forEach { event ->
                val code = Formatter.getDayCodeFromTS(event.startTS)
                val monthLabel = Formatter.getLongMonthYear(context, code)
                if (monthLabel != prevMonthLabel) {
                    val listSectionMonth = ListSectionMonth(monthLabel)
                    listItems.add(listSectionMonth)
                    prevMonthLabel = monthLabel
                }

                if (code != prevCode) {
                    val day = Formatter.getDateDayTitle(code)
                    val isToday = day == today
                    val listSection = ListSectionDay(day, code, isToday, !isToday && event.startTS < now)
                    listItems.add(listSection)
                    prevCode = code
                }

                val listEvent = ListEvent(
                    id = event.id!!,
                    startTS = event.startTS,
                    endTS = event.endTS,
                    title = event.title,
                    description = event.description,
                    isAllDay = event.getIsAllDay(),
                    color = event.color,
                    location = event.location,
                    isPastEvent = event.isPastEvent,
                    isRepeatable = event.repeatInterval > 0,
                    isTask = event.isTask(),
                    isTaskCompleted = event.isTaskCompleted(),
                    isAttendeeInviteDeclined = event.isAttendeeInviteDeclined()
                )
                listItems.add(listEvent)
            }

            this@EventListWidgetAdapter.events = listItems
        }
    }

    override fun hasStableIds() = true

    override fun getCount() = events.size

    override fun onDestroy() {}
}
