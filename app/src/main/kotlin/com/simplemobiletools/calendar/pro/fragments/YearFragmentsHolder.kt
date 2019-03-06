package com.simplemobiletools.calendar.pro.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.appcompat.app.AlertDialog
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.adapters.MyYearPagerAdapter
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.getDialogTheme
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateActionBarTitle
import com.simplemobiletools.commons.views.MyViewPager
import kotlinx.android.synthetic.main.fragment_years_holder.view.*
import org.joda.time.DateTime

class YearFragmentsHolder : MyFragmentHolder() {
    private val PREFILLED_YEARS = 61

    private var viewPager: MyViewPager? = null
    private var defaultYearlyPage = 0
    private var todayYear = 0
    private var currentYear = 0
    private var isGoToTodayVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentYear = DateTime().toString(Formatter.YEAR_PATTERN).toInt()
        todayYear = currentYear
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_years_holder, container, false)
        view.background = ColorDrawable(context!!.config.backgroundColor)
        viewPager = view.fragment_years_viewpager
        viewPager!!.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return view
    }

    private fun setupFragment() {
        val years = getYears(currentYear)
        val yearlyAdapter = MyYearPagerAdapter(activity!!.supportFragmentManager, years)
        defaultYearlyPage = years.size / 2

        viewPager?.apply {
            adapter = yearlyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    currentYear = years[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }

                    if (position < years.size) {
                        (activity as? MainActivity)?.updateActionBarTitle("${getString(R.string.app_launcher_name)} - ${years[position]}")
                    }
                }
            })
            currentItem = defaultYearlyPage
        }
        updateActionBarTitle()
    }

    private fun getYears(targetYear: Int): List<Int> {
        val years = ArrayList<Int>(PREFILLED_YEARS)
        years += targetYear - PREFILLED_YEARS / 2..targetYear + PREFILLED_YEARS / 2
        return years
    }

    override fun goToToday() {
        currentYear = todayYear
        setupFragment()
    }

    override fun showGoToDateDialog() {
        activity!!.setTheme(context!!.getDialogTheme())
        val view = layoutInflater.inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById<DatePicker>(R.id.date_picker)
        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()
        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("month", "id", "android")).beGone()

        val dateTime = DateTime(Formatter.getDateTimeFromCode("${currentYear}0523").toString())
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        AlertDialog.Builder(context!!)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { dialog, which -> datePicked(datePicker) }
                .create().apply {
                    activity?.setupDialogStuff(view, this)
                }
    }

    private fun datePicked(datePicker: DatePicker) {
        val pickedYear = datePicker.year
        if (currentYear != pickedYear) {
            currentYear = datePicker.year
            setupFragment()
        }
    }

    override fun refreshEvents() {
        (viewPager?.adapter as? MyYearPagerAdapter)?.updateCalendars(viewPager?.currentItem ?: 0)
    }

    override fun shouldGoToTodayBeVisible() = currentYear != todayYear

    override fun updateActionBarTitle() {
        (activity as? MainActivity)?.updateActionBarTitle("${getString(R.string.app_launcher_name)} - $currentYear")
    }

    override fun getNewEventDayCode() = Formatter.getTodayCode()
}
