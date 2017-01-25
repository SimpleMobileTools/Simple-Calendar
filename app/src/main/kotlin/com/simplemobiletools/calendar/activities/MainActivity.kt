package com.simplemobiletools.calendar.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.simplemobiletools.calendar.BuildConfig
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.MyMonthPagerAdapter
import com.simplemobiletools.calendar.adapters.MyWeekPagerAdapter
import com.simplemobiletools.calendar.adapters.MyYearPagerAdapter
import com.simplemobiletools.calendar.dialogs.ChangeViewDialog
import com.simplemobiletools.calendar.extensions.*
import com.simplemobiletools.calendar.fragments.EventListFragment
import com.simplemobiletools.calendar.fragments.WeekFragment
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.views.MyScrollView
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.checkWhatsNew
import com.simplemobiletools.commons.extensions.hasReadStoragePermission
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.helpers.LICENSE_JODA
import com.simplemobiletools.commons.helpers.LICENSE_KOTLIN
import com.simplemobiletools.commons.helpers.LICENSE_STETHO
import com.simplemobiletools.commons.models.Release
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

class MainActivity : SimpleActivity(), EventListFragment.DeleteListener {
    private val PREFILLED_MONTHS = 73
    private val PREFILLED_YEARS = 21
    private val PREFILLED_WEEKS = 41
    private val STORAGE_PERMISSION = 1

    private var mIsMonthSelected = false
    private var mSnackbar: Snackbar? = null
    private var mEventListFragment: EventListFragment? = null
    private var mStoredTextColor = 0
    private var mStoredBackgroundColor = 0
    private var mStoredPrimaryColor = 0
    private var mStoredIsSundayFirst = false

    companion object {
        var mWeekScrollY = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        calendar_fab.setOnClickListener { addNewEvent() }
        updateViewPager()
        checkWhatsNewDialog()
    }

    override fun onResume() {
        super.onResume()
        if (mStoredTextColor != config.textColor || mStoredBackgroundColor != config.backgroundColor || mStoredPrimaryColor != config.primaryColor)
            updateViewPager()

        mStoredTextColor = config.textColor
        mStoredPrimaryColor = config.primaryColor
        mStoredBackgroundColor = config.backgroundColor

        if (mStoredIsSundayFirst != config.isSundayFirst && config.storedView == WEEKLY_VIEW)
            fillWeeklyViewPager()

        updateWidgets()
        updateTextColors(calendar_coordinator)
    }

    override fun onPause() {
        super.onPause()
        checkDeleteEvents()
        mStoredTextColor = config.textColor
        mStoredIsSundayFirst = config.isSundayFirst
        mStoredBackgroundColor = config.backgroundColor
        mStoredPrimaryColor = config.primaryColor
    }

    override fun onDestroy() {
        super.onDestroy()
        config.isFirstRun = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.change_view -> showViewDialog()
            R.id.import_file -> tryImportFile()
            R.id.settings -> launchSettings()
            R.id.about -> launchAbout()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        if (mIsMonthSelected && config.storedView == YEARLY_VIEW) {
            updateView(YEARLY_VIEW)
        } else {
            super.onBackPressed()
        }
    }

    private fun showViewDialog() {
        ChangeViewDialog(this) {
            updateView(it)
        }
    }

    private fun updateView(view: Int) {
        calendar_fab.visibility = if (view == YEARLY_VIEW) View.GONE else View.VISIBLE
        mIsMonthSelected = view == MONTHLY_VIEW
        config.storedView = view
        updateViewPager()
    }

    private fun updateViewPager() {
        resetTitle()
        if (config.storedView == YEARLY_VIEW) {
            fillYearlyViewPager()
        } else if (config.storedView == EVENTS_LIST_VIEW) {
            fillEventsList()
        } else if (config.storedView == WEEKLY_VIEW) {
            fillWeeklyViewPager()
        } else {
            val targetDay = DateTime().toString(Formatter.DAYCODE_PATTERN)
            fillMonthlyViewPager(targetDay)
        }

        mWeekScrollY = 0
    }

    private fun tryImportFile() {
        if (hasReadStoragePermission()) {
            importFile()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION)
        }
    }

    private fun importFile() {
        FilePickerDialog(this) {

        }
    }

    private fun launchSettings() {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
    }

    private fun launchAbout() {
        startAboutActivity(R.string.app_name, LICENSE_KOTLIN or LICENSE_JODA or LICENSE_STETHO, BuildConfig.VERSION_NAME)
    }

    private fun addNewEvent() {
        val tomorrowCode = Formatter.getDayCodeFromDateTime(DateTime(DateTimeZone.getDefault()).plusDays(1))
        Intent(applicationContext, EventActivity::class.java).apply {
            putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(tomorrowCode))
            startActivity(this)
        }
    }

    private fun resetTitle() {
        title = getString(R.string.app_launcher_name)
        supportActionBar?.subtitle = ""
    }

    private fun fillMonthlyViewPager(targetDay: String) {
        main_weekly_scrollview.visibility = View.GONE
        calendar_fab.visibility = View.VISIBLE
        val codes = getMonths(targetDay)
        val monthlyAdapter = MyMonthPagerAdapter(supportFragmentManager, codes, this)

        main_view_pager.apply {
            clearOnPageChangeListeners()
            adapter = monthlyAdapter
            currentItem = codes.size / 2
            visibility = View.VISIBLE
        }
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

    private fun fillWeeklyViewPager() {
        val firstDayIndex = if (config.isSundayFirst) 7 else 1
        val thisweek = DateTime().withDayOfWeek(firstDayIndex).withTime(0, 0, 0, 0).seconds()
        val weekTSs = getWeekTimestamps(thisweek)
        val weeklyAdapter = MyWeekPagerAdapter(supportFragmentManager, weekTSs, object : WeekFragment.WeekScrollListener {
            override fun scrollTo(y: Int) {
                week_view_hours_scrollview.scrollY = y
                mWeekScrollY = y
            }
        })
        main_view_pager.visibility = View.GONE
        calendar_event_list_holder.visibility = View.GONE
        main_weekly_scrollview.visibility = View.VISIBLE

        week_view_hours_holder.removeAllViews()
        for (i in 1..23) {
            val value = i.toString()
            (layoutInflater.inflate(R.layout.weekly_view_hour_textview, null, false) as TextView).apply {
                text = if (value.length == 2) value else "0$value"
                setTextColor(mStoredTextColor)
                week_view_hours_holder.addView(this)
            }
        }

        week_view_view_pager.apply {
            adapter = weeklyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    setupActionbarTitle(weekTSs[position])
                }
            })
            currentItem = weekTSs.size / 2
        }

        week_view_hours_scrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                mWeekScrollY = y
                weeklyAdapter.updateScrollY(week_view_view_pager.currentItem, y)
            }
        })
    }

    private fun getWeekTimestamps(targetWeekTS: Int): List<Int> {
        val weekTSs = ArrayList<Int>(PREFILLED_WEEKS)
        for (i in -PREFILLED_WEEKS / 2..PREFILLED_WEEKS / 2) {
            weekTSs.add(targetWeekTS + i * secondsInWeek)
        }
        return weekTSs
    }

    private fun setupActionbarTitle(timestamp: Int) {
        val startDateTime = Formatter.getDateTimeFromTS(timestamp)
        val endDateTime = Formatter.getDateTimeFromTS(timestamp + secondsInWeek)
        val startMonthName = Formatter.getMonthName(this, startDateTime.monthOfYear)
        if (startDateTime.monthOfYear == endDateTime.monthOfYear) {
            var newTitle = startMonthName
            if (startDateTime.year != DateTime().year)
                newTitle += " - ${startDateTime.year}"
            title = newTitle
        } else {
            val endMonthName = Formatter.getMonthName(this, endDateTime.monthOfYear)
            title = "$startMonthName - $endMonthName"
        }
        supportActionBar?.subtitle = "${getString(R.string.week)} ${startDateTime.plusDays(3).weekOfWeekyear}"
    }

    private fun fillYearlyViewPager() {
        main_weekly_scrollview.visibility = View.GONE
        calendar_fab.visibility = View.GONE
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
        years += targetYear - PREFILLED_YEARS / 2..targetYear + PREFILLED_YEARS / 2
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
        main_view_pager.adapter = null
        main_view_pager.visibility = View.GONE
        main_weekly_scrollview.visibility = View.GONE
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tryImportFile()
            }
        }
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

    private fun checkWhatsNewDialog() {
        arrayListOf<Release>().apply {
            add(Release(39, R.string.release_39))
            add(Release(40, R.string.release_40))
            add(Release(42, R.string.release_42))
            add(Release(44, R.string.release_44))
            add(Release(46, R.string.release_46))
            add(Release(48, R.string.release_48))
            add(Release(49, R.string.release_49))
            checkWhatsNew(this, BuildConfig.VERSION_CODE)
        }
    }
}
