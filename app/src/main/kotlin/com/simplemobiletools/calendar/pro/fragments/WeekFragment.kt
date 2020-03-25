package com.simplemobiletools.calendar.pro.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Range
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.collection.LongSparseArray
import androidx.fragment.app.Fragment
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.EventActivity
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.interfaces.WeekFragmentListener
import com.simplemobiletools.calendar.pro.interfaces.WeeklyCalendar
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.EventWeeklyView
import com.simplemobiletools.calendar.pro.views.MyScrollView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.DAY_SECONDS
import com.simplemobiletools.commons.helpers.WEEK_SECONDS
import kotlinx.android.synthetic.main.fragment_week.*
import kotlinx.android.synthetic.main.fragment_week.view.*
import org.joda.time.DateTime
import org.joda.time.Days
import java.util.*

class WeekFragment : Fragment(), WeeklyCalendar {
    private val PLUS_FADEOUT_DELAY = 5000L
    private val MIN_SCALE_FACTOR = 0.3f
    private val MAX_SCALE_FACTOR = 5f
    private val MIN_SCALE_DIFFERENCE = 0.02f
    private val SCALE_RANGE = MAX_SCALE_FACTOR - MIN_SCALE_FACTOR

    var listener: WeekFragmentListener? = null
    private var weekTimestamp = 0L
    private var rowHeight = 0f
    private var todayColumnIndex = -1
    private var primaryColor = 0
    private var lastHash = 0
    private var prevScaleSpanY = 0f
    private var scaleCenterPercent = 0f
    private var defaultRowHeight = 0f
    private var screenHeight = 0
    private var rowHeightsAtScale = 0f
    private var prevScaleFactor = 0f
    private var mWasDestroyed = false
    private var isFragmentVisible = false
    private var wasFragmentInit = false
    private var wasExtraHeightAdded = false
    private var dimPastEvents = true
    private var wasScaled = false
    private var selectedGrid: View? = null
    private var currentTimeView: ImageView? = null
    private var allDayHolders = ArrayList<RelativeLayout>()
    private var allDayRows = ArrayList<HashSet<Int>>()
    private var currEvents = ArrayList<Event>()
    private var eventTypeColors = LongSparseArray<Int>()
    private var eventTimeRanges = LinkedHashMap<String, ArrayList<EventWeeklyView>>()

    private lateinit var inflater: LayoutInflater
    private lateinit var mView: View
    private lateinit var scrollView: MyScrollView
    private lateinit var res: Resources
    private lateinit var config: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        res = context!!.resources
        config = context!!.config
        rowHeight = context!!.getWeeklyViewItemHeight()
        defaultRowHeight = res.getDimension(R.dimen.weekly_view_row_height)
        weekTimestamp = arguments!!.getLong(WEEK_START_TIMESTAMP)
        dimPastEvents = config.dimPastEvents
        primaryColor = context!!.getAdjustedPrimaryColor()
        allDayRows.add(HashSet())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.inflater = inflater

        val fullHeight = context!!.getWeeklyViewItemHeight().toInt() * 24
        mView = inflater.inflate(R.layout.fragment_week, container, false).apply {
            scrollView = week_events_scrollview
            week_horizontal_grid_holder.layoutParams.height = fullHeight
            week_events_columns_holder.layoutParams.height = fullHeight

            val scaleDetector = getViewScaleDetector()
            scrollView.setOnTouchListener { view, motionEvent ->
                scaleDetector.onTouchEvent(motionEvent)
                if (motionEvent.action == MotionEvent.ACTION_UP && wasScaled) {
                    scrollView.isScrollable = true
                    wasScaled = false
                    true
                } else {
                    false
                }
            }
        }

        scrollView.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                checkScrollLimits(y)
            }
        })

        scrollView.onGlobalLayout {
            if (fullHeight < scrollView.height) {
                scrollView.layoutParams.height = fullHeight - context!!.resources.getDimension(R.dimen.one_dp).toInt()
            }

            val initialScrollY = (rowHeight * config.startWeeklyAt).toInt()
            updateScrollY(Math.max(listener?.getCurrScrollY() ?: 0, initialScrollY))
        }

        wasFragmentInit = true
        return mView
    }

    override fun onResume() {
        super.onResume()
        context!!.eventsHelper.getEventTypes(activity!!, false) {
            it.map {
                eventTypeColors.put(it.id!!, it.color)
            }
        }

        setupDayLabels()
        updateCalendar()
    }

    override fun onPause() {
        super.onPause()
        wasExtraHeightAdded = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mWasDestroyed = true
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        isFragmentVisible = menuVisible
        if (isFragmentVisible && wasFragmentInit) {
            listener?.updateHoursTopMargin(mView.week_top_holder.height)
            checkScrollLimits(scrollView.scrollY)

            // fix some glitches like at swiping from a fully scaled out fragment will all-day events to an empty one
            val fullFragmentHeight = (listener?.getFullFragmentHeight() ?: 0) - mView.week_top_holder.height
            if (scrollView.height < fullFragmentHeight) {
                config.weeklyViewItemHeightMultiplier = fullFragmentHeight / 24 / defaultRowHeight
                updateViewScale()
                listener?.updateRowHeight(rowHeight.toInt())
            }
        }
    }

    fun updateCalendar() {
        WeeklyCalendarImpl(this, context!!).updateWeeklyCalendar(weekTimestamp)
    }

    private fun setupDayLabels() {
        var curDay = Formatter.getDateTimeFromTS(weekTimestamp)
        val textColor = config.textColor
        val todayCode = Formatter.getDayCodeFromDateTime(DateTime())
        for (i in 0..6) {
            val dayCode = Formatter.getDayCodeFromDateTime(curDay)
            val dayLetters = res.getStringArray(R.array.week_day_letters).toMutableList() as ArrayList<String>
            val dayLetter = dayLetters[curDay.dayOfWeek - 1]

            mView.findViewById<TextView>(res.getIdentifier("week_day_label_$i", "id", context!!.packageName)).apply {
                text = "$dayLetter\n${curDay.dayOfMonth}"
                setTextColor(if (todayCode == dayCode) primaryColor else textColor)
                if (todayCode == dayCode) {
                    todayColumnIndex = i
                }
            }
            curDay = curDay.plusDays(1)
        }
    }

    private fun checkScrollLimits(y: Int) {
        if (isFragmentVisible) {
            listener?.scrollTo(y)
        }
    }

    private fun initGrid() {
        (0..6).map { getColumnWithId(it) }
                .forEachIndexed { index, layout ->
                    layout.removeAllViews()
                    val gestureDetector = getViewGestureDetector(layout, index)

                    layout.setOnTouchListener { view, motionEvent ->
                        gestureDetector.onTouchEvent(motionEvent)
                        true
                    }
                }
    }

    private fun getViewGestureDetector(view: ViewGroup, index: Int): GestureDetector {
        return GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(event: MotionEvent): Boolean {
                selectedGrid?.animation?.cancel()
                selectedGrid?.beGone()

                val hour = (event.y / rowHeight).toInt()
                selectedGrid = (inflater.inflate(R.layout.week_grid_item, null, false) as ImageView).apply {
                    view.addView(this)
                    background = ColorDrawable(primaryColor)
                    layoutParams.width = view.width
                    layoutParams.height = rowHeight.toInt()
                    y = hour * rowHeight
                    applyColorFilter(primaryColor.getContrastColor())

                    setOnClickListener {
                        val timestamp = weekTimestamp + index * DAY_SECONDS + hour * 60 * 60
                        Intent(context, EventActivity::class.java).apply {
                            putExtra(NEW_EVENT_START_TS, timestamp)
                            putExtra(NEW_EVENT_SET_HOUR_DURATION, true)
                            startActivity(this)
                        }
                    }

                    animate().setStartDelay(PLUS_FADEOUT_DELAY).alpha(0f).withEndAction {
                        beGone()
                    }
                }
                return super.onSingleTapUp(event)
            }
        })
    }

    private fun getViewScaleDetector(): ScaleGestureDetector {
        return ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val percent = (prevScaleSpanY - detector.currentSpanY) / screenHeight
                prevScaleSpanY = detector.currentSpanY

                val wantedFactor = config.weeklyViewItemHeightMultiplier - (SCALE_RANGE * percent)
                var newFactor = Math.max(Math.min(wantedFactor, MAX_SCALE_FACTOR), MIN_SCALE_FACTOR)
                if (scrollView.height > defaultRowHeight * newFactor * 24) {
                    newFactor = scrollView.height / 24f / defaultRowHeight
                }

                if (Math.abs(newFactor - prevScaleFactor) > MIN_SCALE_DIFFERENCE) {
                    prevScaleFactor = newFactor
                    config.weeklyViewItemHeightMultiplier = newFactor
                    updateViewScale()
                    listener?.updateRowHeight(rowHeight.toInt())

                    val targetY = rowHeightsAtScale * rowHeight - scaleCenterPercent * getVisibleHeight()
                    scrollView.scrollTo(0, targetY.toInt())
                }
                return super.onScale(detector)
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                scaleCenterPercent = detector.focusY / scrollView.height
                rowHeightsAtScale = (scrollView.scrollY + scaleCenterPercent * getVisibleHeight()) / rowHeight
                scrollView.isScrollable = false
                prevScaleSpanY = detector.currentSpanY
                prevScaleFactor = config.weeklyViewItemHeightMultiplier
                wasScaled = true
                screenHeight = context!!.realScreenSize.y
                return super.onScaleBegin(detector)
            }
        })
    }

    private fun getVisibleHeight(): Float {
        val fullContentHeight = rowHeight * 24
        val visibleRatio = scrollView.height / fullContentHeight
        return fullContentHeight * visibleRatio
    }

    override fun updateWeeklyCalendar(events: ArrayList<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash || mWasDestroyed || context == null) {
            return
        }

        lastHash = newHash

        activity!!.runOnUiThread {
            if (context != null && activity != null && isAdded) {
                val replaceDescription = config.replaceDescription
                val sorted = events.sortedWith(
                        compareBy<Event> { it.startTS }.thenBy { it.endTS }.thenBy { it.title }.thenBy { if (replaceDescription) it.location else it.description }
                ).toMutableList() as ArrayList<Event>

                currEvents = sorted
                addEvents(sorted)
            }
        }
    }

    private fun updateViewScale() {
        rowHeight = context?.getWeeklyViewItemHeight() ?: return

        val oneDp = context!!.resources.getDimension(R.dimen.one_dp).toInt()
        val fullHeight = Math.max(rowHeight.toInt() * 24, scrollView.height + oneDp)
        scrollView.layoutParams.height = fullHeight - oneDp
        mView.week_horizontal_grid_holder.layoutParams.height = fullHeight
        mView.week_events_columns_holder.layoutParams.height = fullHeight
        addEvents(currEvents)
    }

    private fun addEvents(events: ArrayList<Event>) {
        initGrid()
        allDayHolders.clear()
        allDayRows.clear()
        eventTimeRanges.clear()
        allDayRows.add(HashSet())
        week_all_day_holder?.removeAllViews()

        addNewLine()

        val minuteHeight = rowHeight / 60
        val minimalHeight = res.getDimension(R.dimen.weekly_view_minimal_event_height).toInt()
        val density = Math.round(res.displayMetrics.density)

        var hadAllDayEvent = false

        for (event in events) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            if (!event.getIsAllDay() && Formatter.getDayCodeFromDateTime(startDateTime) == Formatter.getDayCodeFromDateTime(endDateTime)) {
                val startMinutes = startDateTime.minuteOfDay
                val duration = endDateTime.minuteOfDay - startMinutes
                val range = Range(startMinutes, startMinutes + duration)
                val eventWeekly = EventWeeklyView(event.id!!, range)

                val dayCode = Formatter.getDayCodeFromDateTime(startDateTime)
                if (!eventTimeRanges.containsKey(dayCode)) {
                    eventTimeRanges[dayCode] = ArrayList()
                }

                eventTimeRanges[dayCode]?.add(eventWeekly)
            }
        }

        for (event in events) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            if (event.getIsAllDay() || Formatter.getDayCodeFromDateTime(startDateTime) != Formatter.getDayCodeFromDateTime(endDateTime)) {
                hadAllDayEvent = true
                addAllDayEvent(event)
            } else {
                val dayOfWeek = startDateTime.plusDays(if (config.isSundayFirst) 1 else 0).dayOfWeek - 1
                val layout = getColumnWithId(dayOfWeek)

                val startMinutes = startDateTime.minuteOfDay
                val duration = endDateTime.minuteOfDay - startMinutes
                val range = Range(startMinutes, startMinutes + duration)

                val dayCode = Formatter.getDayCodeFromDateTime(startDateTime)
                var overlappingEvents = 0
                var currentEventOverlapIndex = 0
                var foundCurrentEvent = false

                eventTimeRanges[dayCode]!!.forEachIndexed { index, eventWeeklyView ->
                    if (eventWeeklyView.range.touch(range)) {
                        overlappingEvents++

                        if (eventWeeklyView.id == event.id) {
                            foundCurrentEvent = true
                        }

                        if (!foundCurrentEvent) {
                            currentEventOverlapIndex++
                        }
                    }
                }

                (inflater.inflate(R.layout.week_event_marker, null, false) as TextView).apply {
                    var backgroundColor = eventTypeColors.get(event.eventType, primaryColor)
                    var textColor = backgroundColor.getContrastColor()
                    if (dimPastEvents && event.isPastEvent) {
                        backgroundColor = backgroundColor.adjustAlpha(LOW_ALPHA)
                        textColor = textColor.adjustAlpha(LOW_ALPHA)
                    }

                    background = ColorDrawable(backgroundColor)
                    setTextColor(textColor)
                    text = event.title
                    contentDescription = text
                    layout.addView(this)
                    y = startMinutes * minuteHeight
                    (layoutParams as RelativeLayout.LayoutParams).apply {
                        width = layout.width - 1
                        width /= Math.max(overlappingEvents, 1)
                        if (overlappingEvents > 1) {
                            x = width * currentEventOverlapIndex.toFloat()
                            if (currentEventOverlapIndex != 0) {
                                x += density
                            }

                            width -= density
                            if (currentEventOverlapIndex + 1 != overlappingEvents) {
                                if (currentEventOverlapIndex != 0) {
                                    width -= density
                                }
                            }
                        }

                        minHeight = if (event.startTS == event.endTS) {
                            minimalHeight
                        } else {
                            (duration * minuteHeight).toInt() - 1
                        }
                    }
                    setOnClickListener {
                        Intent(context, EventActivity::class.java).apply {
                            putExtra(EVENT_ID, event.id!!)
                            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
                            startActivity(this)
                        }
                    }
                }
            }
        }

        if (!hadAllDayEvent) {
            checkTopHolderHeight()
        }

        addCurrentTimeIndicator(minuteHeight)
    }

    private fun addNewLine() {
        val allDaysLine = inflater.inflate(R.layout.all_day_events_holder_line, null, false) as RelativeLayout
        week_all_day_holder.addView(allDaysLine)
        allDayHolders.add(allDaysLine)
    }

    private fun addCurrentTimeIndicator(minuteHeight: Float) {
        if (todayColumnIndex != -1) {
            val minutes = DateTime().minuteOfDay
            val todayColumn = getColumnWithId(todayColumnIndex)
            if (currentTimeView != null) {
                mView.week_events_holder.removeView(currentTimeView)
            }

            currentTimeView = (inflater.inflate(R.layout.week_now_marker, null, false) as ImageView).apply {
                applyColorFilter(primaryColor)
                mView.week_events_holder.addView(this, 0)
                val extraWidth = (todayColumn.width * 0.3).toInt()
                val markerHeight = res.getDimension(R.dimen.weekly_view_now_height).toInt()
                (layoutParams as RelativeLayout.LayoutParams).apply {
                    width = todayColumn.width + extraWidth
                    height = markerHeight
                }
                x = todayColumn.x - extraWidth / 2
                y = minutes * minuteHeight - markerHeight / 2
            }
        }
    }

    private fun checkTopHolderHeight() {
        mView.week_top_holder.onGlobalLayout {
            if (isFragmentVisible && activity != null && !mWasDestroyed) {
                listener?.updateHoursTopMargin(mView.week_top_holder.height)
            }
        }
    }

    private fun addAllDayEvent(event: Event) {
        (inflater.inflate(R.layout.week_all_day_event_marker, null, false) as TextView).apply {
            var backgroundColor = eventTypeColors.get(event.eventType, primaryColor)
            var textColor = backgroundColor.getContrastColor()
            if (dimPastEvents && event.isPastEvent) {
                backgroundColor = backgroundColor.adjustAlpha(LOW_ALPHA)
                textColor = textColor.adjustAlpha(LOW_ALPHA)
            }
            background = ColorDrawable(backgroundColor)

            setTextColor(textColor)
            text = event.title
            contentDescription = text

            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)

            val minTS = Math.max(startDateTime.seconds(), weekTimestamp)
            val maxTS = Math.min(endDateTime.seconds(), weekTimestamp + WEEK_SECONDS)

            // fix a visual glitch with all-day events or events lasting multiple days starting at midnight on monday, being shown the previous week too
            if (minTS == maxTS && (minTS - weekTimestamp == WEEK_SECONDS.toLong())) {
                return
            }

            val isStartTimeDay = Formatter.getDateTimeFromTS(maxTS) == Formatter.getDateTimeFromTS(maxTS).withTimeAtStartOfDay()
            val numDays = Days.daysBetween(Formatter.getDateTimeFromTS(minTS).toLocalDate(), Formatter.getDateTimeFromTS(maxTS).toLocalDate()).days
            val daysCnt = if (numDays == 1 && isStartTimeDay) 0 else numDays
            val startDateTimeInWeek = Formatter.getDateTimeFromTS(minTS)
            val firstDayIndex = (startDateTimeInWeek.dayOfWeek - if (config.isSundayFirst) 0 else 1) % 7

            var doesEventFit: Boolean
            val cnt = allDayRows.size - 1
            var wasEventHandled = false
            var drawAtLine = 0
            for (index in 0..cnt) {
                doesEventFit = true
                drawAtLine = index
                val row = allDayRows[index]
                for (i in firstDayIndex..firstDayIndex + daysCnt) {
                    if (row.contains(i)) {
                        doesEventFit = false
                    }
                }

                for (dayIndex in firstDayIndex..firstDayIndex + daysCnt) {
                    if (doesEventFit) {
                        row.add(dayIndex)
                        wasEventHandled = true
                    } else if (index == cnt) {
                        if (allDayRows.size == index + 1) {
                            allDayRows.add(HashSet())
                            addNewLine()
                            drawAtLine++
                            wasEventHandled = true
                        }
                        allDayRows.last().add(dayIndex)
                    }
                }
                if (wasEventHandled) {
                    break
                }
            }

            allDayHolders[drawAtLine].addView(this)
            (layoutParams as RelativeLayout.LayoutParams).apply {
                leftMargin = getColumnWithId(firstDayIndex).x.toInt()
                bottomMargin = 1
                width = getColumnWithId(Math.min(firstDayIndex + daysCnt, 6)).right - leftMargin - 1
            }

            calculateExtraHeight()

            setOnClickListener {
                Intent(context, EventActivity::class.java).apply {
                    putExtra(EVENT_ID, event.id)
                    putExtra(EVENT_OCCURRENCE_TS, event.startTS)
                    startActivity(this)
                }
            }
        }
    }

    private fun calculateExtraHeight() {
        mView.week_top_holder.onGlobalLayout {
            if (activity != null && !mWasDestroyed) {
                if (isFragmentVisible) {
                    listener?.updateHoursTopMargin(mView.week_top_holder.height)
                }

                if (!wasExtraHeightAdded) {
                    wasExtraHeightAdded = true
                }
            }
        }
    }

    private fun getColumnWithId(id: Int) = mView.findViewById<ViewGroup>(res.getIdentifier("week_column_$id", "id", context!!.packageName))

    fun updateScrollY(y: Int) {
        if (wasFragmentInit) {
            scrollView.scrollY = y
        }
    }

    fun updateNotVisibleViewScaleLevel() {
        if (!isFragmentVisible) {
            updateViewScale()
        }
    }
}
