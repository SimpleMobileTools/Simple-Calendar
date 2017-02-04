package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.activities.MainActivity
import com.simplemobiletools.calendar.adapters.WeekEventsAdapter
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.extensions.secondsInWeek
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.interfaces.WeeklyCalendar
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.views.MyScrollView
import kotlinx.android.synthetic.main.fragment_week.*
import kotlinx.android.synthetic.main.fragment_week.view.*
import org.joda.time.DateTime
import org.joda.time.Days
import kotlin.comparisons.compareBy

class WeekFragment : Fragment(), WeeklyCalendar {
    private var mListener: WeekScrollListener? = null
    private var mWeekTimestamp = 0
    private var mRowHeight = 0
    private var minScrollY = -1
    private var maxScrollY = -1
    private var mWasDestroyed = false
    private var primaryColor = 0
    private var isFragmentVisible = false
    private var wasFragmentInit = false
    private var wasExtraHeightAdded = false
    private var oneDP = 1

    lateinit var mView: View
    lateinit var mCalendar: WeeklyCalendarImpl
    lateinit var mRes: Resources

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRowHeight = (context.resources.getDimension(R.dimen.weekly_view_row_height)).toInt()
        minScrollY = mRowHeight * context.config.startWeeklyAt
        mWeekTimestamp = arguments.getInt(WEEK_START_TIMESTAMP)
        primaryColor = context.config.primaryColor
        mRes = resources
        oneDP = mRes.displayMetrics.density.toInt()

        mView = inflater.inflate(R.layout.fragment_week, container, false).apply {
            week_events_scrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
                override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                    checkScrollLimits(y)
                }
            })

            week_events_scrollview.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    week_events_scrollview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    updateScrollY(Math.max(MainActivity.mWeekScrollY, minScrollY))
                }
            })

            week_events_grid.adapter = WeekEventsAdapter(context, mWeekTimestamp) {
                Intent(context, EventActivity::class.java).apply {
                    putExtra(NEW_EVENT_START_TS, it)
                    startActivity(this)
                }
            }
        }

        mCalendar = WeeklyCalendarImpl(this, context)
        wasFragmentInit = true
        return mView
    }

    override fun setMenuVisibility(menuVisible: Boolean) {
        super.setMenuVisibility(menuVisible)
        isFragmentVisible = menuVisible
        if (isFragmentVisible && wasFragmentInit) {
            (activity as MainActivity).updateHoursTopMargin(mView.week_top_holder.height)
            checkScrollLimits(mView.week_events_scrollview.scrollY)
        }
    }

    override fun onPause() {
        super.onPause()
        wasExtraHeightAdded = true
    }

    override fun onResume() {
        super.onResume()
        setupDayLabels()
        mCalendar.updateWeeklyCalendar(mWeekTimestamp)

        mView.week_events_scrollview.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (context == null)
                    return

                mView.week_events_scrollview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                minScrollY = mRowHeight * context.config.startWeeklyAt
                maxScrollY = mRowHeight * context.config.endWeeklyAt

                val bounds = Rect()
                week_events_holder.getGlobalVisibleRect(bounds)
                maxScrollY -= bounds.bottom - bounds.top
                if (minScrollY > maxScrollY)
                    maxScrollY = -1

                checkScrollLimits(mView.week_events_scrollview.scrollY)
            }
        })
    }

    private fun setupDayLabels() {
        var curDay = Formatter.getDateTimeFromTS(mWeekTimestamp)
        val textColor = context.config.textColor
        val todayCode = Formatter.getDayCodeFromDateTime(DateTime())
        for (i in 0..6) {
            val dayCode = Formatter.getDayCodeFromDateTime(curDay)
            val dayLetter = getDayLetter(curDay.dayOfWeek)
            (mView.findViewById(mRes.getIdentifier("week_day_label_$i", "id", context.packageName)) as TextView).apply {
                text = "$dayLetter\n${curDay.dayOfMonth}"
                setTextColor(if (todayCode == dayCode) primaryColor else textColor)
            }
            curDay = curDay.plusDays(1)
        }
    }

    private fun getDayLetter(pos: Int): String {
        return mRes.getString(when (pos) {
            1 -> R.string.monday_letter
            2 -> R.string.tuesday_letter
            3 -> R.string.wednesday_letter
            4 -> R.string.thursday_letter
            5 -> R.string.friday_letter
            6 -> R.string.saturday_letter
            else -> R.string.sunday_letter
        })
    }

    private fun checkScrollLimits(y: Int) {
        if (minScrollY != -1 && y < minScrollY) {
            mView.week_events_scrollview.scrollY = minScrollY
        } else if (maxScrollY != -1 && y > maxScrollY) {
            mView.week_events_scrollview.scrollY = maxScrollY
        } else {
            if (isFragmentVisible)
                mListener?.scrollTo(y)
        }
    }

    override fun updateWeeklyCalendar(events: List<Event>) {
        if (mWasDestroyed)
            return

        val fullHeight = mRes.getDimension(R.dimen.weekly_view_events_height)
        val minuteHeight = fullHeight / (24 * 60)
        val minimalHeight = mRes.getDimension(R.dimen.weekly_view_minimal_event_height).toInt()
        (0..6).map { getColumnWithId(it) }
                .forEach { activity.runOnUiThread { it.removeAllViews() } }

        activity.runOnUiThread { mView.week_all_day_holder.removeAllViews() }

        val sorted = events.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { it.description }))
        for (event in sorted) {
            if (event.isAllDay() || Formatter.getDayCodeFromTS(event.startTS) != Formatter.getDayCodeFromTS(event.endTS)) {
                addAllDayEvent(event)
            } else {
                val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
                val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
                val dayOfWeek = startDateTime.plusDays(if (context.config.isSundayFirst) 1 else 0).dayOfWeek - 1
                val layout = getColumnWithId(dayOfWeek)

                val startMinutes = startDateTime.minuteOfDay
                val duration = endDateTime.minuteOfDay - startMinutes

                (LayoutInflater.from(context).inflate(R.layout.week_event_marker, null, false) as TextView).apply {
                    background = ColorDrawable(primaryColor)
                    text = event.title
                    activity.runOnUiThread {
                        layout.addView(this)
                        (layoutParams as RelativeLayout.LayoutParams).apply {
                            rightMargin = oneDP
                            topMargin = (startMinutes * minuteHeight).toInt()
                            width = layout.width
                            minHeight = if (event.startTS == event.endTS) minimalHeight else (duration * minuteHeight).toInt() - oneDP
                        }
                    }
                    setOnClickListener {
                        Intent(activity.applicationContext, EventActivity::class.java).apply {
                            putExtra(EVENT_ID, event.id)
                            startActivity(this)
                        }
                    }
                }
            }
        }
    }

    private fun addAllDayEvent(event: Event) {
        (LayoutInflater.from(context).inflate(R.layout.week_all_day_event_marker, null, false) as TextView).apply {
            background = ColorDrawable(primaryColor)
            text = event.title

            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)

            val minTS = Math.max(startDateTime.withTimeAtStartOfDay().seconds(), mWeekTimestamp)
            val maxTS = Math.min(endDateTime.withTimeAtStartOfDay().seconds(), mWeekTimestamp + context.secondsInWeek)
            val startDateTimeInWeek = Formatter.getDateTimeFromTS(minTS)
            val firstDayIndex = startDateTimeInWeek.dayOfWeek - if (context.config.isSundayFirst) 0 else 1
            val daysCnt = Days.daysBetween(Formatter.getDateTimeFromTS(minTS), Formatter.getDateTimeFromTS(maxTS)).days

            activity.runOnUiThread {
                val dayColumnWidth = getColumnWithId(1).width
                mView.week_all_day_holder.addView(this)
                (layoutParams as LinearLayout.LayoutParams).apply {
                    topMargin = mRes.getDimension(R.dimen.tiny_margin).toInt()
                    bottomMargin = oneDP
                    leftMargin = firstDayIndex % 7 * dayColumnWidth
                    width = (daysCnt + 1) * dayColumnWidth - oneDP
                }

                mView.week_top_holder.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        mView.week_top_holder.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        if (isFragmentVisible) {
                            (activity as MainActivity).updateHoursTopMargin(mView.week_top_holder.height)
                        }

                        if (!wasExtraHeightAdded) {
                            maxScrollY += mView.week_all_day_holder.height
                            wasExtraHeightAdded = true
                        }
                    }
                })
            }
            setOnClickListener {
                Intent(activity.applicationContext, EventActivity::class.java).apply {
                    putExtra(EVENT_ID, event.id)
                    startActivity(this)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mWasDestroyed = true
    }

    private fun getColumnWithId(id: Int) = mView.findViewById(mRes.getIdentifier("week_column_$id", "id", context.packageName)) as RelativeLayout

    fun setListener(listener: WeekScrollListener) {
        mListener = listener
    }

    fun updateScrollY(y: Int) {
        if (wasFragmentInit)
            mView.week_events_scrollview.scrollY = y
    }

    interface WeekScrollListener {
        fun scrollTo(y: Int)
    }
}
