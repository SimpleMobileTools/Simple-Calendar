package com.simplemobiletools.calendar.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.MainActivity
import com.simplemobiletools.calendar.adapters.MyWeekPagerAdapter
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.WEEK_START_DATE_TIME
import com.simplemobiletools.calendar.interfaces.WeekFragmentListener
import com.simplemobiletools.calendar.views.MyScrollView
import com.simplemobiletools.commons.extensions.updateActionBarSubtitle
import com.simplemobiletools.commons.extensions.updateActionBarTitle
import com.simplemobiletools.commons.helpers.WEEK_SECONDS
import kotlinx.android.synthetic.main.fragment_week_holder.*
import kotlinx.android.synthetic.main.fragment_week_holder.view.*
import org.joda.time.DateTime

class WeekFragmentsHolder : MyFragmentHolder(), WeekFragmentListener {
    private val PREFILLED_WEEKS = 151

    private var weekHolder: ViewGroup? = null
    private var defaultWeeklyPage = 0
    private var thisWeekTS = 0
    private var currentWeekTS = 0
    private var isGoToTodayVisible = false
    private var weekScrollY = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateTimeString = arguments?.getString(WEEK_START_DATE_TIME) ?: ""
        currentWeekTS = (DateTime.parse(dateTimeString) ?: DateTime()).seconds()
        thisWeekTS = currentWeekTS
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        weekHolder = inflater.inflate(R.layout.fragment_week_holder, container, false) as ViewGroup
        weekHolder!!.background = ColorDrawable(context!!.config.backgroundColor)
        setupFragment()
        return weekHolder
    }

    private fun setupFragment() {
        val weekTSs = getWeekTimestamps(currentWeekTS)
        val weeklyAdapter = MyWeekPagerAdapter(activity!!.supportFragmentManager, weekTSs, this)

        val textColor = context!!.config.textColor
        weekHolder!!.week_view_hours_holder.removeAllViews()
        val hourDateTime = DateTime().withDate(2000, 1, 1).withTime(0, 0, 0, 0)
        for (i in 1..23) {
            val formattedHours = Formatter.getHours(context!!, hourDateTime.withHourOfDay(i))
            (layoutInflater.inflate(R.layout.weekly_view_hour_textview, null, false) as TextView).apply {
                text = formattedHours
                setTextColor(textColor)
                weekHolder!!.week_view_hours_holder.addView(this)
            }
        }

        defaultWeeklyPage = weekTSs.size / 2
        weekHolder!!.week_view_view_pager.apply {
            adapter = weeklyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    currentWeekTS = weekTSs[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }

                    setupWeeklyActionbarTitle(weekTSs[position])
                }
            })
            currentItem = defaultWeeklyPage
        }

        weekHolder!!.week_view_hours_scrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                weekScrollY = y
                weeklyAdapter.updateScrollY(week_view_view_pager.currentItem, y)
            }
        })
        weekHolder!!.week_view_hours_scrollview.setOnTouchListener { view, motionEvent -> true }
        updateActionBarTitle()
    }

    private fun getWeekTimestamps(targetSeconds: Int): List<Int> {
        val weekTSs = ArrayList<Int>(PREFILLED_WEEKS)
        for (i in -PREFILLED_WEEKS / 2..PREFILLED_WEEKS / 2) {
            weekTSs.add(Formatter.getDateTimeFromTS(targetSeconds).plusWeeks(i).seconds())
        }
        return weekTSs
    }

    private fun setupWeeklyActionbarTitle(timestamp: Int) {
        val startDateTime = Formatter.getDateTimeFromTS(timestamp)
        val endDateTime = Formatter.getDateTimeFromTS(timestamp + WEEK_SECONDS)
        val startMonthName = Formatter.getMonthName(context!!, startDateTime.monthOfYear)
        if (startDateTime.monthOfYear == endDateTime.monthOfYear) {
            var newTitle = startMonthName
            if (startDateTime.year != DateTime().year) {
                newTitle += " - ${startDateTime.year}"
            }
            (activity as MainActivity).updateActionBarTitle(newTitle)
        } else {
            val endMonthName = Formatter.getMonthName(context!!, endDateTime.monthOfYear)
            (activity as MainActivity).updateActionBarTitle("$startMonthName - $endMonthName")
        }
        (activity as MainActivity).updateActionBarSubtitle("${getString(R.string.week)} ${startDateTime.plusDays(3).weekOfWeekyear}")
    }

    override fun goToToday() {
        currentWeekTS = thisWeekTS
        setupFragment()
    }

    override fun refreshEvents() {
        val viewPager = weekHolder?.week_view_view_pager
        (viewPager?.adapter as? MyWeekPagerAdapter)?.updateCalendars(viewPager.currentItem)
    }

    override fun shouldGoToTodayBeVisible() = currentWeekTS != thisWeekTS

    override fun updateActionBarTitle() {
        setupWeeklyActionbarTitle(currentWeekTS)
    }

    override fun getNewEventDayCode() = Formatter.getDayCodeFromTS(currentWeekTS)

    override fun scrollTo(y: Int) {
        weekHolder!!.week_view_hours_scrollview.scrollY = y
        weekScrollY = y
    }

    override fun updateHoursTopMargin(margin: Int) {
        weekHolder?.week_view_hours_divider?.layoutParams?.height = margin
        weekHolder?.week_view_hours_scrollview?.requestLayout()
    }

    override fun getCurrScrollY() = weekScrollY
}
