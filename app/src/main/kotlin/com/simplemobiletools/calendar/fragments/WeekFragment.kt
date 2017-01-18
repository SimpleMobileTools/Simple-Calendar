package com.simplemobiletools.calendar.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.MainActivity
import com.simplemobiletools.calendar.adapters.WeekEventsAdapter
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.WEEK_START_TIMESTAMP
import com.simplemobiletools.calendar.helpers.WeeklyCalendarImpl
import com.simplemobiletools.calendar.interfaces.WeeklyCalendar
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.views.MyScrollView
import kotlinx.android.synthetic.main.fragment_week.view.*

class WeekFragment : Fragment(), WeeklyCalendar {
    private var mListener: WeekScrollListener? = null
    private var mWeekTimestamp = 0
    lateinit var mView: View
    lateinit var mCalendar: WeeklyCalendarImpl
    lateinit var mRes: Resources

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_week, container, false).apply {

            week_events_scrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
                override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                    mListener?.scrollTo(y)
                }
            })

            week_events_scrollview.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    updateScrollY(MainActivity.mWeekScrollY)
                    mView.week_events_scrollview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })

            week_events_grid.adapter = WeekEventsAdapter(context)
        }

        mRes = resources
        mWeekTimestamp = arguments.getInt(WEEK_START_TIMESTAMP)
        mCalendar = WeeklyCalendarImpl(this, context)
        setupDayLabels()
        return mView
    }

    private fun setupDayLabels() {
        var curDay = Formatter.getDateTimeFromTS(mWeekTimestamp)
        for (i in 0..6) {
            val view = mView.findViewById(mRes.getIdentifier("week_day_label_$i", "id", context.packageName)) as TextView
            view.text = curDay.dayOfMonth.toString()
            curDay = curDay.plusDays(1)
        }
    }

    override fun onResume() {
        super.onResume()
        mCalendar.updateWeeklyCalendar(mWeekTimestamp)
    }

    override fun updateWeeklyCalendar(events: List<Event>) {
        val fullHeight = mRes.getDimension(R.dimen.weekly_view_events_height)
        val minuteHeight = fullHeight / (24 * 60)
        val eventColor = context.config.primaryColor
        val sideMargin = mRes.displayMetrics.density.toInt()
        for (event in events) {
            val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
            val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
            val dayOfWeek = startDateTime.dayOfWeek - if (context.config.isSundayFirst) 0 else 1
            val layout = mView.findViewById(mRes.getIdentifier("week_column_$dayOfWeek", "id", context.packageName)) as LinearLayout

            val startMinutes = startDateTime.minuteOfDay
            val duration = endDateTime.minuteOfDay - startMinutes

            LayoutInflater.from(context).inflate(R.layout.week_event_marker, null, false).apply {
                background = ColorDrawable(eventColor)
                activity.runOnUiThread {
                    layout.addView(this)
                    (layoutParams as LinearLayout.LayoutParams).apply {
                        rightMargin = sideMargin
                        topMargin = (startMinutes * minuteHeight).toInt()
                        height = (duration * minuteHeight).toInt() - sideMargin
                    }
                }
            }
        }
    }

    fun setListener(listener: WeekScrollListener) {
        mListener = listener
    }

    fun updateScrollY(y: Int) {
        mView.week_events_scrollview.scrollY = y
    }

    interface WeekScrollListener {
        fun scrollTo(y: Int)
    }
}
