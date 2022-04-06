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
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.adapters.EventListAdapter
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.Config
import com.simplemobiletools.calendar.pro.helpers.DAY_CODE
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.Formatter.YEAR_PATTERN
import com.simplemobiletools.calendar.pro.helpers.MonthlyCalendarImpl
import com.simplemobiletools.calendar.pro.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener
import com.simplemobiletools.calendar.pro.models.DayMonthly
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.ListEvent
import com.simplemobiletools.commons.extensions.areSystemAnimationsEnabled
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import kotlinx.android.synthetic.main.fragment_month_day.*
import kotlinx.android.synthetic.main.fragment_month_day.view.*
import org.joda.time.DateTime

class MonthDayFragment : Fragment(), MonthlyCalendar, RefreshRecyclerViewListener {
    private var mSundayFirst = false
    private var mShowWeekNumbers = false
    private var mDayCode = ""
    private var mSelectedDayCode = ""
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
        mPackageName = requireActivity().packageName
        mHolder = view.month_day_calendar_holder
        mDayCode = requireArguments().getString(DAY_CODE)!!

        val shownMonthDateTime = Formatter.getDateTimeFromCode(mDayCode)
        mHolder.month_day_selected_day_label.apply {
            text = getMonthLabel(shownMonthDateTime)
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
        }

        mConfig = requireContext().config
        storeStateVariables()
        setupButtons()
        mCalendar = MonthlyCalendarImpl(this, requireContext())
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
                mSelectedDayCode = it.code
                updateVisibleEvents()
            }
        }

        refreshItems()
    }

    private fun updateVisibleEvents() {
        if (activity == null) {
            return
        }

        val filtered = mListEvents.filter {
            if (mSelectedDayCode.isEmpty()) {
                val shownMonthDateTime = Formatter.getDateTimeFromCode(mDayCode)
                val startDateTime = Formatter.getDateTimeFromTS(it.startTS)
                shownMonthDateTime.year == startDateTime.year && shownMonthDateTime.monthOfYear == startDateTime.monthOfYear
            } else {
                val selectionDate = Formatter.getDateTimeFromCode(mSelectedDayCode).toLocalDate()
                val startDate = Formatter.getDateFromTS(it.startTS)
                val endDate = Formatter.getDateFromTS(it.endTS)
                selectionDate in startDate..endDate
            }
        }

        val listItems = requireActivity().getEventListItems(filtered, mSelectedDayCode.isEmpty(), false)
        if (mSelectedDayCode.isNotEmpty()) {
            mHolder.month_day_selected_day_label.text = Formatter.getDateFromCode(requireActivity(), mSelectedDayCode, false)
        }

        activity?.runOnUiThread {
            if (activity != null) {
                mHolder.month_day_events_list.beVisibleIf(listItems.isNotEmpty())
                mHolder.month_day_no_events_placeholder.beVisibleIf(listItems.isEmpty())

                val currAdapter = mHolder.month_day_events_list.adapter
                if (currAdapter == null) {
                    EventListAdapter(activity as SimpleActivity, listItems, true, this, month_day_events_list) {
                        if (it is ListEvent) {
                            activity?.editEvent(it)
                        }
                    }.apply {
                        month_day_events_list.adapter = this
                    }

                    if (requireContext().areSystemAnimationsEnabled) {
                        month_day_events_list.scheduleLayoutAnimation()
                    }
                } else {
                    (currAdapter as EventListAdapter).updateListItems(listItems)
                }
            }
        }
    }

    private fun setupButtons() {
        val textColor = requireContext().getProperTextColor()
        mHolder.apply {
            month_day_selected_day_label.setTextColor(textColor)
            month_day_no_events_placeholder.setTextColor(textColor)
        }
    }

    fun printCurrentView() {}

    fun getNewEventDayCode() = if (mSelectedDayCode.isEmpty()) null else mSelectedDayCode

    private fun getMonthLabel(shownMonthDateTime: DateTime): String {
        var month = Formatter.getMonthName(requireActivity(), shownMonthDateTime.monthOfYear)
        val targetYear = shownMonthDateTime.toString(YEAR_PATTERN)
        if (targetYear != DateTime().toString(YEAR_PATTERN)) {
            month += " $targetYear"
        }
        return month
    }

    override fun refreshItems() {
        val startDateTime = Formatter.getLocalDateTimeFromCode(mDayCode).minusWeeks(1)
        val endDateTime = startDateTime.plusWeeks(7)
        activity?.eventsHelper?.getEvents(startDateTime.seconds(), endDateTime.seconds()) { events ->
            mListEvents = events
            activity?.runOnUiThread {
                updateVisibleEvents()
            }
        }
    }
}
