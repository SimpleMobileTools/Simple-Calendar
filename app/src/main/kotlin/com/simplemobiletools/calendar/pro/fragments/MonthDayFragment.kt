package com.simplemobiletools.calendar.pro.fragments

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.adapters.EventListAdapter
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.Config
import com.simplemobiletools.calendar.pro.helpers.DAY_CODE
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.MonthlyCalendarImpl
import com.simplemobiletools.calendar.pro.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener
import com.simplemobiletools.calendar.pro.models.DayMonthly
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.ListEvent
import kotlinx.android.synthetic.main.fragment_month_day.*
import kotlinx.android.synthetic.main.fragment_month_day.view.*
import org.joda.time.DateTime

class MonthDayFragment : Fragment(), MonthlyCalendar {
    private var mTextColor = 0
    private var mSundayFirst = false
    private var mShowWeekNumbers = false
    private var mDayCode = ""
    private var mCurrentDayCode = ""
    private var mPackageName = ""
    private var mLastHash = 0L
    private var mCalendar: MonthlyCalendarImpl? = null
    private var mListEvents = ArrayList<Event>()

    var listener: NavigationListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: ConstraintLayout
    lateinit var mConfig: Config

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_month_day, container, false)
        mRes = resources
        mPackageName = activity!!.packageName
        mHolder = view.month_day_calendar_holder
        mDayCode = arguments!!.getString(DAY_CODE)!!

        val shownMonthDateTime = Formatter.getDateTimeFromCode(mDayCode)
        val todayCode = Formatter.getTodayCode()
        val todayDateTime = Formatter.getDateTimeFromCode(todayCode)
        if (todayDateTime.year == shownMonthDateTime.year && todayDateTime.monthOfYear == shownMonthDateTime.monthOfYear) {
            mCurrentDayCode = todayCode
        }

        mConfig = context!!.config
        storeStateVariables()
        setupButtons()
        mCalendar = MonthlyCalendarImpl(this, context!!)
        return view
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        if (mConfig.showWeekNumbers != mShowWeekNumbers) {
            mLastHash = -1L
        }

        mCalendar!!.apply {
            mTargetDate = Formatter.getDateTimeFromCode(mDayCode)
            getDays(false)    // prefill the screen asap, even if without events
        }

        storeStateVariables()
        updateCalendar()
    }

    private fun storeStateVariables() {
        mConfig.apply {
            mSundayFirst = isSundayFirst
            mShowWeekNumbers = showWeekNumbers
        }
    }

    fun updateCalendar() {
        mCalendar?.updateMonthlyCalendar(Formatter.getDateTimeFromCode(mDayCode))
    }

    override fun updateMonthlyCalendar(context: Context, month: String, days: ArrayList<DayMonthly>, checkedEvents: Boolean, currTargetDate: DateTime) {
        val newHash = month.hashCode() + days.hashCode().toLong()
        if ((mLastHash != 0L && !checkedEvents) || mLastHash == newHash) {
            return
        }

        mLastHash = newHash

        activity?.runOnUiThread {
            mHolder.month_day_view_wrapper.updateDays(days, false) {
                mCurrentDayCode = it.code
                updateVisibleEvents()
            }
        }

        val startDateTime = Formatter.getLocalDateTimeFromCode(mDayCode).minusWeeks(1)
        val endDateTime = startDateTime.plusWeeks(6)
        context.eventsHelper.getEvents(startDateTime.seconds(), endDateTime.seconds()) { events ->
            mListEvents = events
            updateVisibleEvents()
        }
    }

    private fun updateVisibleEvents() {
        val filtered = mListEvents.filter {
            Formatter.getDayCodeFromTS(it.startTS) == mCurrentDayCode
        }

        val listItems = context!!.getEventListItems(filtered, false)
        activity?.runOnUiThread {
            EventListAdapter(activity as SimpleActivity, listItems, true, null, month_day_events_list, false) {
                if (it is ListEvent) {
                    activity?.editEvent(it)
                }
            }.apply {
                month_day_events_list.adapter = this
            }
        }
    }

    private fun setupButtons() {
        mTextColor = mConfig.textColor
    }

    fun printCurrentView() {}
}
