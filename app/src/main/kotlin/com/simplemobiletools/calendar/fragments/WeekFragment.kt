package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.MainActivity
import com.simplemobiletools.calendar.adapters.WeekEventsAdapter
import com.simplemobiletools.calendar.helpers.WeeklyCalendarImpl
import com.simplemobiletools.calendar.interfaces.WeeklyCalendar
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.views.MyScrollView
import kotlinx.android.synthetic.main.fragment_week.view.*
import org.joda.time.DateTime

class WeekFragment : Fragment(), WeeklyCalendar {
    private var mListener: WeekScrollListener? = null
    lateinit var mView: View
    lateinit var mCalendar: WeeklyCalendarImpl

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_week, container, false)

        mView.week_events_scrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                mListener?.scrollTo(y)
            }
        })

        mView.week_events_scrollview.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                updateScrollY(MainActivity.mWeekScrollY)
                mView.week_events_scrollview.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        mView.week_events_grid.adapter = WeekEventsAdapter(context)
        mCalendar = WeeklyCalendarImpl(this, context)
        return mView
    }

    override fun onResume() {
        super.onResume()
        mCalendar.updateWeeklyCalendar(DateTime())
    }

    override fun updateWeeklyCalendar(events: List<Event>) {

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
