package com.simplemobiletools.calendar.pro.fragments

import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Range
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.collection.LongSparseArray
import androidx.fragment.app.Fragment
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.EventActivity
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.extensions.touch
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
    private val CLICK_DURATION_THRESHOLD = 150
    private val PLUS_FADEOUT_DELAY = 5000L

    var mListener: WeekFragmentListener? = null
    private var mWeekTimestamp = 0L
    private var mRowHeight = 0f
    private var minScrollY = -1
    private var maxScrollY = -1
    private var todayColumnIndex = -1
    private var clickStartTime = 0L
    private var primaryColor = 0
    private var lastHash = 0
    private var mWasDestroyed = false
    private var isFragmentVisible = false
    private var wasFragmentInit = false
    private var wasExtraHeightAdded = false
    private var dimPastEvents = true
    private var selectedGrid: View? = null
    private var currentTimeView: ImageView? = null
    private var events = ArrayList<Event>()
    private var allDayHolders = ArrayList<RelativeLayout>()
    private var allDayRows = ArrayList<HashSet<Int>>()
    private var eventTypeColors = LongSparseArray<Int>()
    private var eventTimeRanges = LinkedHashMap<String, ArrayList<EventWeeklyView>>()

    private lateinit var inflater: LayoutInflater
    private lateinit var mView: View
    private lateinit var mScrollView: MyScrollView
    private lateinit var mCalendar: WeeklyCalendarImpl
    private lateinit var mRes: Resources
    private lateinit var mConfig: Config

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRes = context!!.resources
        mConfig = context!!.config
        mRowHeight = mRes.getDimension(R.dimen.weekly_view_row_height)
        minScrollY = (mRowHeight * mConfig.startWeeklyAt).toInt()
        mWeekTimestamp = arguments!!.getLong(WEEK_START_TIMESTAMP)
        dimPastEvents = mConfig.dimPastEvents
        primaryColor = context!!.getAdjustedPrimaryColor()
        allDayRows.add(HashSet())
        mCalendar = WeeklyCalendarImpl(this, context!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        this.inflater = inflater

        mView = inflater.inflate(R.layout.fragment_week, container, false)
        mScrollView = mView.week_events_scrollview
        mScrollView.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                checkScrollLimits(y)
            }
        })

        mScrollView.onGlobalLayout {
            updateScrollY(Math.max(mListener?.getCurrScrollY() ?: 0, minScrollY))
        }

        wasFragmentInit = true
        return mView
    }

    override fun onResume() {
        super.onResume()
        context!!.eventsHelper.getEventTypes(activity!!, false) {
            it.map { eventTypeColors.put(it.id!!, it.color) }
        }

        setupDayLabels()
        updateCalendar()

        mScrollView.onGlobalLayout {
            if (context == null) {
                return@onGlobalLayout
            }

            minScrollY = (mRowHeight * mConfig.startWeeklyAt).toInt()
            maxScrollY = (mRowHeight * mConfig.endWeeklyAt).toInt()

            val bounds = Rect()
            week_events_holder.getGlobalVisibleRect(bounds)
            maxScrollY -= bounds.bottom - bounds.top
            if (minScrollY > maxScrollY)
                maxScrollY = -1

            checkScrollLimits(mScrollView.scrollY)
        }
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
            mListener?.updateHoursTopMargin(mView.week_top_holder.height)
            checkScrollLimits(mScrollView.scrollY)
        }
    }

    fun updateCalendar() {
        mCalendar.updateWeeklyCalendar(mWeekTimestamp)
    }

    private fun setupDayLabels() {
        var curDay = Formatter.getDateTimeFromTS(mWeekTimestamp)
        val textColor = mConfig.textColor
        val todayCode = Formatter.getDayCodeFromDateTime(DateTime())
        for (i in 0..6) {
            val dayCode = Formatter.getDayCodeFromDateTime(curDay)
            val dayLetters = mRes.getStringArray(R.array.week_day_letters).toMutableList() as ArrayList<String>
            val dayLetter = dayLetters[curDay.dayOfWeek - 1]

            mView.findViewById<TextView>(mRes.getIdentifier("week_day_label_$i", "id", context!!.packageName)).apply {
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
        if (minScrollY != -1 && y < minScrollY) {
            mScrollView.scrollY = minScrollY
        } else if (maxScrollY != -1 && y > maxScrollY) {
            mScrollView.scrollY = maxScrollY
        } else if (isFragmentVisible) {
            mListener?.scrollTo(y)
        }
    }

    private fun initGrid() {
        (0..6).map { getColumnWithId(it) }
                .forEachIndexed { index, layout ->
                    layout.removeAllViews()
                    layout.setOnTouchListener { view, motionEvent ->
                        checkGridClick(motionEvent, index, layout)
                        true
                    }
                }
    }

    private fun checkGridClick(event: MotionEvent, index: Int, view: ViewGroup) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> clickStartTime = System.currentTimeMillis()
            MotionEvent.ACTION_UP -> {
                if (System.currentTimeMillis() - clickStartTime < CLICK_DURATION_THRESHOLD) {
                    selectedGrid?.animation?.cancel()
                    selectedGrid?.beGone()

                    val hour = (event.y / mRowHeight).toInt()
                    selectedGrid = (inflater.inflate(R.layout.week_grid_item, null, false) as ImageView).apply {
                        view.addView(this)
                        background = ColorDrawable(primaryColor)
                        layoutParams.width = view.width
                        layoutParams.height = mRowHeight.toInt()
                        y = hour * mRowHeight
                        applyColorFilter(primaryColor.getContrastColor())

                        setOnClickListener {
                            val timestamp = mWeekTimestamp + index * DAY_SECONDS + hour * 60 * 60
                            Intent(context, EventActivity::class.java).apply {
                                putExtra(NEW_EVENT_START_TS, timestamp)
                                putExtra(NEW_EVENT_SET_HOUR_DURATION, true)
                                startActivity(this)
                            }
                        }
                        animate().alpha(0f).setStartDelay(PLUS_FADEOUT_DELAY).withEndAction {
                            beGone()
                        }
                    }
                }
            }
        }
    }

    override fun updateWeeklyCalendar(events: ArrayList<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash || mWasDestroyed || context == null) {
            return
        }

        lastHash = newHash
        this.events = events

        activity!!.runOnUiThread {
            if (context != null && activity != null && isAdded) {
                addEvents()
            }
        }
    }

    private fun addEvents() {
        initGrid()
        allDayHolders.clear()
        allDayRows.clear()
        eventTimeRanges.clear()
        allDayRows.add(HashSet())
        week_all_day_holder?.removeAllViews()

        addNewLine()

        val fullHeight = mRes.getDimension(R.dimen.weekly_view_events_height)
        val minuteHeight = fullHeight / (24 * 60)
        val minimalHeight = mRes.getDimension(R.dimen.weekly_view_minimal_event_height).toInt()
        val density = Math.round(context!!.resources.displayMetrics.density)

        var hadAllDayEvent = false
        val replaceDescription = mConfig.replaceDescription
        val sorted = events.sortedWith(compareBy<Event> { it.startTS }.thenBy { it.endTS }.thenBy { it.title }.thenBy { if (replaceDescription) it.location else it.description })
        for (event in sorted) {
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

        for (event in sorted) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            if (event.getIsAllDay() || Formatter.getDayCodeFromDateTime(startDateTime) != Formatter.getDayCodeFromDateTime(endDateTime)) {
                hadAllDayEvent = true
                addAllDayEvent(event)
            } else {
                val dayOfWeek = startDateTime.plusDays(if (mConfig.isSundayFirst) 1 else 0).dayOfWeek - 1
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

                        minHeight = if (event.startTS == event.endTS) minimalHeight else (duration * minuteHeight).toInt() - 1
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

            currentTimeView = (inflater.inflate(R.layout.week_now_marker, null, false) as ImageView)
            currentTimeView!!.apply {
                applyColorFilter(primaryColor)
                mView.week_events_holder.addView(this, 0)
                val extraWidth = (todayColumn.width * 0.3).toInt()
                val markerHeight = mRes.getDimension(R.dimen.weekly_view_now_height).toInt()
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
                mListener?.updateHoursTopMargin(mView.week_top_holder.height)
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

            val minTS = Math.max(startDateTime.seconds(), mWeekTimestamp)
            val maxTS = Math.min(endDateTime.seconds(), mWeekTimestamp + WEEK_SECONDS)

            // fix a visual glitch with all-day events or events lasting multiple days starting at midnight on monday, being shown the previous week too
            if (minTS == maxTS && (minTS - mWeekTimestamp == WEEK_SECONDS.toLong())) {
                return
            }

            val isStartTimeDay = Formatter.getDateTimeFromTS(maxTS) == Formatter.getDateTimeFromTS(maxTS).withTimeAtStartOfDay()
            val numDays = Days.daysBetween(Formatter.getDateTimeFromTS(minTS).toLocalDate(), Formatter.getDateTimeFromTS(maxTS).toLocalDate()).days
            val daysCnt = if (numDays == 1 && isStartTimeDay) 0 else numDays
            val startDateTimeInWeek = Formatter.getDateTimeFromTS(minTS)
            val firstDayIndex = (startDateTimeInWeek.dayOfWeek - if (mConfig.isSundayFirst) 0 else 1) % 7

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
                    mListener?.updateHoursTopMargin(mView.week_top_holder.height)
                }

                if (!wasExtraHeightAdded) {
                    maxScrollY += mView.week_all_day_holder.height
                    wasExtraHeightAdded = true
                }
            }
        }
    }

    private fun getColumnWithId(id: Int) = mView.findViewById<ViewGroup>(mRes.getIdentifier("week_column_$id", "id", context!!.packageName))

    fun updateScrollY(y: Int) {
        if (wasFragmentInit) {
            mScrollView.scrollY = y
        }
    }
}
