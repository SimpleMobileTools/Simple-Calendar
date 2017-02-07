package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.ViewPager
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.MyDayPagerAdapter
import com.simplemobiletools.calendar.extensions.getNewEventTimestampFromCode
import com.simplemobiletools.calendar.helpers.DAY_CODE
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.NEW_EVENT_START_TS
import com.simplemobiletools.calendar.interfaces.NavigationListener
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.activity_day.*
import org.joda.time.DateTime
import java.util.*

class DayActivity : SimpleActivity(), NavigationListener, ViewPager.OnPageChangeListener {
    private val PREFILLED_DAYS = 121
    private var mDayCode = ""
    private var mPagerDays: MutableList<String>? = null
    private var mPagerPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day)

        val intent = intent ?: return
        mDayCode = intent.getStringExtra(DAY_CODE)
        if (mDayCode.isEmpty())
            return

        fillViewPager(mDayCode)

        day_fab.setOnClickListener { addNewEvent() }
        updateTextColors(day_coordinator)
    }

    private fun fillViewPager(targetDay: String) {
        getDays(targetDay)
        val daysAdapter = MyDayPagerAdapter(supportFragmentManager, mPagerDays!!, this)
        mPagerPos = mPagerDays!!.size / 2
        view_pager.apply {
            adapter = daysAdapter
            currentItem = mPagerPos
            addOnPageChangeListener(this@DayActivity)
        }
    }

    private fun addNewEvent() {
        Intent(applicationContext, EventActivity::class.java).apply {
            putExtra(NEW_EVENT_START_TS, getNewEventTimestampFromCode(mPagerDays?.get(view_pager.currentItem).toString()))
            startActivity(this)
        }
    }

    private fun getDays(code: String) {
        mPagerDays = ArrayList<String>(PREFILLED_DAYS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_DAYS / 2..PREFILLED_DAYS / 2) {
            mPagerDays!!.add(Formatter.getDayCodeFromDateTime(today.plusDays(i)))
        }
    }

    fun recheckEvents() {
        (view_pager.adapter as MyDayPagerAdapter).checkDayEvents(mPagerPos)
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        mPagerPos = position
    }

    override fun goLeft() {
        view_pager.currentItem = view_pager.currentItem - 1
    }

    override fun goRight() {
        view_pager.currentItem = view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        fillViewPager(Formatter.getDayCodeFromDateTime(dateTime))
    }
}
