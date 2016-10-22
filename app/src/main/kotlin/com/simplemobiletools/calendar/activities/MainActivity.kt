package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.NavigationListener
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.MyMonthPagerAdapter
import com.simplemobiletools.calendar.adapters.MyYearPagerAdapter
import com.simplemobiletools.calendar.extensions.updateWidget
import com.simplemobiletools.calendar.fragments.EventListFragment
import com.simplemobiletools.calendar.views.dialogs.ChangeViewDialog
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

class MainActivity : SimpleActivity(), NavigationListener, ChangeViewDialog.ChangeViewListener {
    private val PREFILLED_MONTHS = 73
    private val PREFILLED_YEARS = 21

    private var mIsMonthSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateViewPager()

        calendar_fab.setOnClickListener { addNewEvent() }
    }

    override fun onResume() {
        super.onResume()
        updateWidget()
    }

    override fun onDestroy() {
        super.onDestroy()
        mConfig.isFirstRun = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.change_view -> {
                showViewDialog()
                true
            }
            R.id.settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                true
            }
            R.id.about -> {
                startActivity(Intent(applicationContext, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (mIsMonthSelected && mConfig.view == Constants.YEARLY_VIEW) {
            updateView(Constants.YEARLY_VIEW)
        } else {
            super.onBackPressed()
        }
    }

    private fun showViewDialog() {
        ChangeViewDialog(this)
    }

    override fun viewChanged(newView: Int) {
        updateView(newView)
    }

    private fun updateView(view: Int) {
        mIsMonthSelected = view == Constants.MONTHLY_VIEW
        mConfig.view = view
        updateViewPager()
    }

    private fun updateViewPager() {
        if (mConfig.view == Constants.YEARLY_VIEW) {
            fillYearlyViewPager()
        } else if (mConfig.view == Constants.EVENTS_LIST_VIEW) {
            fillEventsList()
        } else {
            val targetDay = DateTime().toString(Formatter.DAYCODE_PATTERN)
            fillMonthlyViewPager(targetDay)
        }
    }

    private fun addNewEvent() {
        val intent = Intent(applicationContext, EventActivity::class.java)
        val tomorrowCode = Formatter.getDayCodeFromDateTime(DateTime(DateTimeZone.getDefault()).plusDays(1))
        intent.putExtra(Constants.DAY_CODE, tomorrowCode)
        startActivity(intent)
    }

    private fun fillMonthlyViewPager(targetDay: String) {
        val codes = getMonths(targetDay)
        val monthlyAdapter = MyMonthPagerAdapter(supportFragmentManager, codes, this)

        view_pager.apply {
            clearOnPageChangeListeners()
            adapter = monthlyAdapter
            currentItem = codes.size / 2
            visibility = View.VISIBLE
        }
        title = getString(R.string.app_launcher_name)
        calendar_event_list_holder.visibility = View.GONE
    }

    private fun getMonths(code: String): List<String> {
        val months = ArrayList<String>(PREFILLED_MONTHS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_MONTHS / 2..PREFILLED_MONTHS / 2) {
            months.add(Formatter.getDayCodeFromDateTime(today.plusMonths(i)))
        }

        return months
    }

    private fun fillYearlyViewPager() {
        val targetYear = DateTime().toString(Formatter.YEAR_PATTERN).toInt()
        val years = getYears(targetYear)
        val yearlyAdapter = MyYearPagerAdapter(supportFragmentManager, years, this)

        view_pager.apply {
            adapter = yearlyAdapter
            currentItem = years.size / 2
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    if (position < years.size)
                        title = "${getString(R.string.app_launcher_name)} - ${years[position]}"
                }
            })
            visibility = View.VISIBLE
        }
        title = "${getString(R.string.app_launcher_name)} - ${years[years.size / 2]}"
        calendar_event_list_holder.visibility = View.GONE
    }

    private fun getYears(targetYear: Int): List<Int> {
        val years = ArrayList<Int>(PREFILLED_YEARS)
        for (i in targetYear - PREFILLED_YEARS / 2..targetYear + PREFILLED_YEARS / 2)
            years.add(i)

        return years
    }

    private fun fillEventsList() {
        title = getString(R.string.app_launcher_name)
        view_pager.adapter = null
        view_pager.visibility = View.GONE
        calendar_event_list_holder.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction().replace(R.id.calendar_event_list_holder, EventListFragment(), "").commit()
    }

    override fun goLeft() {
        view_pager.currentItem = view_pager.currentItem - 1
    }

    override fun goRight() {
        view_pager.currentItem = view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        fillMonthlyViewPager(Formatter.getDayCodeFromDateTime(dateTime))
        mIsMonthSelected = true
    }
}
