package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.MyMonthPagerAdapter
import com.simplemobiletools.calendar.adapters.MyYearPagerAdapter
import com.simplemobiletools.calendar.dialogs.ChangeViewDialog
import com.simplemobiletools.calendar.extensions.updateWidget
import com.simplemobiletools.calendar.fragments.EventListFragment
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

class MainActivity : SimpleActivity(), EventListFragment.DeleteListener, ChangeViewDialog.ChangeViewListener {
    private val PREFILLED_MONTHS = 73
    private val PREFILLED_YEARS = 21

    private var mIsMonthSelected = false
    private var mSnackbar: Snackbar? = null
    private var mEventListFragment: EventListFragment? = null

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

    override fun onPause() {
        super.onPause()
        checkDeleteEvents()
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
        if (mIsMonthSelected && mConfig.storedView == YEARLY_VIEW) {
            updateView(YEARLY_VIEW)
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
        mIsMonthSelected = view == MONTHLY_VIEW
        mConfig.storedView = view
        updateViewPager()
    }

    private fun updateViewPager() {
        if (mConfig.storedView == YEARLY_VIEW) {
            fillYearlyViewPager()
        } else if (mConfig.storedView == EVENTS_LIST_VIEW) {
            fillEventsList()
        } else {
            val targetDay = DateTime().toString(Formatter.DAYCODE_PATTERN)
            fillMonthlyViewPager(targetDay)
        }
    }

    private fun addNewEvent() {
        val tomorrowCode = Formatter.getDayCodeFromDateTime(DateTime(DateTimeZone.getDefault()).plusDays(1))
        Intent(applicationContext, EventActivity::class.java).apply {
            putExtra(DAY_CODE, tomorrowCode)
            startActivity(this)
        }
    }

    private fun fillMonthlyViewPager(targetDay: String) {
        val codes = getMonths(targetDay)
        val monthlyAdapter = MyMonthPagerAdapter(supportFragmentManager, codes, this)

        main_view_pager.apply {
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

        main_view_pager.apply {
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

    fun checkDeleteEvents() {
        if (mSnackbar != null && mSnackbar!!.isShown) {
            deleteEvents()
        } else {
            undoDeletion()
        }
    }

    private fun deleteEvents() {
        mSnackbar!!.dismiss()
        mEventListFragment?.deleteEvents()
    }

    private val undoDeletion = View.OnClickListener { undoDeletion() }

    private fun undoDeletion() {
        if (mSnackbar != null) {
            mSnackbar!!.dismiss()
            mEventListFragment?.undoDeletion()
        }
    }

    private fun fillEventsList() {
        title = getString(R.string.app_launcher_name)
        main_view_pager.adapter = null
        main_view_pager.visibility = View.GONE
        calendar_event_list_holder.visibility = View.VISIBLE

        if (mEventListFragment == null)
            mEventListFragment = EventListFragment()
        supportFragmentManager.beginTransaction().replace(R.id.calendar_event_list_holder, mEventListFragment, "").commit()
    }

    override fun goLeft() {
        main_view_pager.currentItem = main_view_pager.currentItem - 1
    }

    override fun goRight() {
        main_view_pager.currentItem = main_view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        fillMonthlyViewPager(Formatter.getDayCodeFromDateTime(dateTime))
        mIsMonthSelected = true
    }

    override fun notifyDeletion(cnt: Int) {
        val msg = resources.getQuantityString(R.plurals.events_deleted, cnt, cnt)
        mSnackbar = Snackbar.make(calendar_coordinator, msg, Snackbar.LENGTH_LONG)
        mSnackbar!!.apply {
            setCallback(object : Snackbar.Callback() {
                override fun onDismissed(snackbar: Snackbar?, event: Int) {
                    super.onDismissed(snackbar, event)
                    mEventListFragment?.deleteEvents()
                }
            })
            setAction(resources.getString(R.string.undo), undoDeletion)
            setActionTextColor(Color.WHITE)
            show()
        }
    }
}
