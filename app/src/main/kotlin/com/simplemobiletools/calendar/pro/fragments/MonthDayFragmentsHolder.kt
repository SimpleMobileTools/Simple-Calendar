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
import com.simplemobiletools.calendar.pro.adapters.MyMonthDayPagerAdapter
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.getMonthCode
import com.simplemobiletools.calendar.pro.helpers.ANNIVERSARIES_COUNTER
import com.simplemobiletools.calendar.pro.helpers.BIRTHDAY_COUNTER
import com.simplemobiletools.calendar.pro.helpers.DAY_CODE
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.getDialogTheme
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.updateActionBarTitle
import com.simplemobiletools.commons.views.MyViewPager
import kotlinx.android.synthetic.main.fragment_months_days_holder.view.*
import org.joda.time.DateTime

class MonthDayFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private val PREFILLED_MONTHS = 251

    private var viewPager: MyViewPager? = null
    private var defaultMonthlyPage = 0
    private var todayDayCode = ""
    private var currentDayCode = ""
    private var isGoToTodayVisible = false
    private var ageCounter = HashMap<Long?,Long>()
    private var anniversariesCounter = HashMap<Long?,Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDayCode = arguments?.getString(DAY_CODE) ?: ""
        todayDayCode = Formatter.getTodayCode()
        ageCounter = arguments?.getSerializable(BIRTHDAY_COUNTER) as HashMap<Long?, Long>
        anniversariesCounter = arguments?.getSerializable(ANNIVERSARIES_COUNTER) as HashMap<Long?, Long>
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_months_days_holder, container, false)
        view.background = ColorDrawable(context!!.config.backgroundColor)
        viewPager = view.fragment_months_days_viewpager
        viewPager!!.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return view
    }

    private fun setupFragment() {
        val codes = getMonths(currentDayCode)
        val monthlyDailyAdapter = MyMonthDayPagerAdapter(activity!!.supportFragmentManager, codes, this)
        defaultMonthlyPage = codes.size / 2
        monthlyDailyAdapter.ageCounter = this.ageCounter
        monthlyDailyAdapter.anniversariesCounter = this.anniversariesCounter
        viewPager!!.apply {
            adapter = monthlyDailyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    currentDayCode = codes[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }
                }
            })
            currentItem = defaultMonthlyPage
        }
        updateActionBarTitle()
    }

    private fun getMonths(code: String): List<String> {
        val months = ArrayList<String>(PREFILLED_MONTHS)
        val today = Formatter.getDateTimeFromCode(code).withDayOfMonth(1)
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)))
        }

        return months
    }

    override fun goLeft() {
        viewPager!!.currentItem = viewPager!!.currentItem - 1
    }

    override fun goRight() {
        viewPager!!.currentItem = viewPager!!.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        currentDayCode = Formatter.getDayCodeFromDateTime(dateTime)
        setupFragment()
    }

    override fun goToToday() {
        currentDayCode = todayDayCode
        setupFragment()
    }

    override fun showGoToDateDialog() {
        activity!!.setTheme(context!!.getDialogTheme())
        val view = layoutInflater.inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById<DatePicker>(R.id.date_picker)
        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()

        val dateTime = DateTime(Formatter.getDateTimeFromCode(currentDayCode).toString())
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        AlertDialog.Builder(context!!)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { dialog, which -> datePicked(dateTime, datePicker) }
            .create().apply {
                activity?.setupDialogStuff(view, this)
            }
    }

    private fun datePicked(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val newDateTime = dateTime.withDate(year, month, 1)
        goToDateTime(newDateTime)
    }

    override fun refreshEvents() {
        (viewPager?.adapter as? MyMonthDayPagerAdapter)?.updateCalendars(viewPager?.currentItem ?: 0)
    }

    override fun shouldGoToTodayBeVisible() = currentDayCode.getMonthCode() != todayDayCode.getMonthCode()

    override fun updateActionBarTitle() {
        (activity as? MainActivity)?.updateActionBarTitle(getString(R.string.app_launcher_name))
    }

    override fun getNewEventDayCode() = (viewPager?.adapter as? MyMonthDayPagerAdapter)?.getNewEventDayCode(viewPager?.currentItem ?: 0)
        ?: if (shouldGoToTodayBeVisible()) {
            currentDayCode
        } else {
            todayDayCode
        }

    override fun printView() {}
}
