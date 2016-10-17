package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.calendar.*
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.adapters.MyMonthPagerAdapter
import com.simplemobiletools.calendar.adapters.MyYearPagerAdapter
import com.simplemobiletools.calendar.extensions.updateWidget
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

class MainActivity : SimpleActivity(), NavigationListener {
    private val PREFILLED_MONTHS = 73
    private val PREFILLED_YEARS = 21

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
        Config.newInstance(applicationContext).isFirstRun = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        menu.findItem(R.id.yearly_view).isVisible = mConfig.view == Constants.MONTHLY_VIEW
        menu.findItem(R.id.monthly_view).isVisible = mConfig.view == Constants.YEARLY_VIEW

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.monthly_view -> {
                updateView(Constants.MONTHLY_VIEW)
                true
            }
            R.id.yearly_view -> {
                updateView(Constants.YEARLY_VIEW)
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

    private fun updateView(view: Int) {
        mConfig.view = view
        updateViewPager()
        invalidateOptionsMenu()
    }

    private fun updateViewPager() {
        if (mConfig.view == Constants.MONTHLY_VIEW) {
            val targetDay = DateTime().toString(Formatter.DAYCODE_PATTERN)
            fillMonthlyViewPager(targetDay)
        } else {
            fillYearlyViewPager()
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
        val adapter = MyMonthPagerAdapter(supportFragmentManager, codes, this)
        view_pager.adapter = adapter
        view_pager.currentItem = codes.size / 2
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
        val adapter = MyYearPagerAdapter(supportFragmentManager, years, this)
        view_pager.adapter = adapter
        view_pager.currentItem = years.size / 2
    }

    private fun getYears(targetYear: Int): List<Int> {
        val years = ArrayList<Int>(PREFILLED_YEARS)
        for (i in targetYear - PREFILLED_YEARS / 2..targetYear + PREFILLED_YEARS / 2)
            years.add(i)

        return years
    }

    override fun goLeft() {
        view_pager.currentItem = view_pager.currentItem - 1
    }

    override fun goRight() {
        view_pager.currentItem = view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        fillMonthlyViewPager(Formatter.getDayCodeFromDateTime(dateTime))
    }
}
