package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.view.View
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.MyDayPagerAdapter
import com.simplemobiletools.calendar.fragments.DayFragment
import com.simplemobiletools.calendar.helpers.Formatter
import kotlinx.android.synthetic.main.activity_day.*
import org.joda.time.DateTime
import java.util.*

class DayActivity : SimpleActivity(), DayFragment.DeleteListener, ViewPager.OnPageChangeListener {
    private val PREFILLED_DAYS = 121
    private var mDayCode: String? = null
    private var mSnackbar: Snackbar? = null
    private var mPagerDays: MutableList<String>? = null
    private var mPagerPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day)

        val intent = intent ?: return
        mDayCode = intent.getStringExtra(Constants.DAY_CODE)
        if (mDayCode == null || mDayCode!!.isEmpty())
            return

        fillViewPager(mDayCode!!)

        day_fab.setOnClickListener { addNewEvent() }
    }

    override fun onPause() {
        super.onPause()
        checkDeleteEvents()
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
        val eventIntent = Intent(applicationContext, EventActivity::class.java)
        eventIntent.putExtra(Constants.DAY_CODE, mPagerDays?.get(view_pager.currentItem))
        startActivity(eventIntent)
    }

    private fun getDays(code: String) {
        mPagerDays = ArrayList<String>(PREFILLED_DAYS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_DAYS / 2..PREFILLED_DAYS / 2) {
            mPagerDays!!.add(Formatter.getDayCodeFromDateTime(today.plusDays(i)))
        }
    }

    private fun checkDeleteEvents() {
        if (mSnackbar != null && mSnackbar!!.isShown) {
            deleteEvents()
        } else {
            undoDeletion()
        }
    }

    private fun deleteEvents() {
        mSnackbar!!.dismiss()
        (view_pager.adapter as MyDayPagerAdapter).deleteItems(mPagerPos)
    }

    private val undoDeletion = View.OnClickListener { undoDeletion() }

    private fun undoDeletion() {
        if (mSnackbar != null) {
            mSnackbar!!.dismiss()
            (view_pager.adapter as MyDayPagerAdapter).undoDeletion(view_pager.currentItem)
        }
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

    }

    override fun onPageSelected(position: Int) {
        checkDeleteEvents()
        mPagerPos = position
    }

    override fun goLeft() {
        checkDeleteEvents()
        view_pager.currentItem = view_pager.currentItem - 1
    }

    override fun goRight() {
        checkDeleteEvents()
        view_pager.currentItem = view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {
        checkDeleteEvents()
        fillViewPager(Formatter.getDayCodeFromDateTime(dateTime))
    }

    override fun notifyDeletion(cnt: Int) {
        val msg = resources.getQuantityString(R.plurals.events_deleted, cnt, cnt)
        mSnackbar = Snackbar.make(day_coordinator, msg, Snackbar.LENGTH_INDEFINITE)
        mSnackbar!!.apply {
            setAction(resources.getString(R.string.undo), undoDeletion)
            setActionTextColor(Color.WHITE)
            show()
        }
    }
}
