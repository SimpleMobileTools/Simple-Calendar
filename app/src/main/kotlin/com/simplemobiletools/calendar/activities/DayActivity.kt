package com.simplemobiletools.calendar.activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.NavigationListener
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.MyDayPagerAdapter
import com.simplemobiletools.calendar.models.Event
import kotlinx.android.synthetic.main.activity_day.*
import org.joda.time.DateTime
import java.util.*

class DayActivity : SimpleActivity(), NavigationListener {

    private val PREFILLED_DAYS = 61
    private var mDayCode: String? = null
    private var mEvents: MutableList<Event>? = null
    private var mSelectedItemsCnt: Int = 0
    private var mSnackbar: Snackbar? = null
    private var mToBeDeleted: MutableList<Int>? = null

    companion object {
        val DELETED_ID = "deleted_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day)

        val intent = intent ?: return
        mDayCode = intent.getStringExtra(Constants.DAY_CODE)
        if (mDayCode == null || mDayCode!!.isEmpty())
            return

        fillViewPager(mDayCode!!)

        day_fab.setOnClickListener {
            val eventIntent = Intent(applicationContext, EventActivity::class.java)
            eventIntent.putExtra(Constants.DAY_CODE, mDayCode)
            startActivity(eventIntent)
        }

        //mToBeDeleted = ArrayList<Int>()
    }

    override fun onPause() {
        super.onPause()
        checkDeleteEvents()
    }

    private fun fillViewPager(targetDay: String) {
        val codes = getDays(targetDay)
        val adapter = MyDayPagerAdapter(supportFragmentManager, codes, this)
        view_pager.adapter = adapter
        view_pager.currentItem = codes.size / 2
    }

    private fun getDays(code: String): List<String> {
        val days = ArrayList<String>(PREFILLED_DAYS)
        val today = Formatter.getDateTimeFromCode(code)
        for (i in -PREFILLED_DAYS / 2..PREFILLED_DAYS / 2) {
            days.add(Formatter.getDayCodeFromDateTime(today.plusDays(i)))
        }

        return days
    }

    private fun switchToDay(dayCode: String) {
        val intent = Intent(applicationContext, DayActivity::class.java)
        intent.putExtra(Constants.DAY_CODE, dayCode)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    /*private fun updateEvents(events: MutableList<Event>) {
        mEvents = ArrayList(events)
        val eventsToShow = getEventsToShow(events)
        val adapter = EventsAdapter(this, eventsToShow)
        mEventsList!!.adapter = adapter
        mEventsList!!.onItemClickListener = this
        mEventsList!!.setMultiChoiceModeListener(this)
    }

    private fun getEventsToShow(events: MutableList<Event>): List<Event> {
        val cnt = events.size
        for (i in cnt - 1 downTo 0) {
            if (mToBeDeleted!!.contains(events[i].id)) {
                events.removeAt(i)
            }
        }
        return events
    }*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /*if (requestCode == EDIT_EVENT && resultCode == Activity.RESULT_OK && data != null) {
            val deletedId = data.getIntExtra(DELETED_ID, -1)
            if (deletedId != -1) {
                mToBeDeleted!!.clear()
                mToBeDeleted!!.add(deletedId)
                notifyEventDeletion(1)
            }
        }*/
    }

    private fun checkDeleteEvents() {
        if (mSnackbar != null && mSnackbar!!.isShown) {
            deleteEvents()
        } else {
            undoDeletion()
        }
    }

    private fun deleteEvents() {
        /*mSnackbar!!.dismiss()

        val cnt = mToBeDeleted!!.size
        val eventIDs = arrayOfNulls<String>(cnt)
        for (i in 0..cnt - 1) {
            eventIDs[i] = mToBeDeleted!![i].toString()
        }

        DBHelper.newInstance(applicationContext, this).deleteEvents(eventIDs)*/
    }

    private val undoDeletion = View.OnClickListener { undoDeletion() }

    private fun undoDeletion() {
        if (mSnackbar != null) {
            mToBeDeleted!!.clear()
            mSnackbar!!.dismiss()
            //updateEvents(mEvents)
        }
    }

    private fun prepareDeleteEvents() {
        /*val checked = mEventsList!!.checkedItemPositions
        for (i in mEvents!!.indices) {
            if (checked.get(i)) {
                val event = mEvents!![i]
                mToBeDeleted!!.add(event.id)
            }
        }

        notifyEventDeletion(mToBeDeleted!!.size)*/
    }

    private fun notifyEventDeletion(cnt: Int) {
        /*val res = resources
        val msg = res.getQuantityString(R.plurals.events_deleted, cnt, cnt)
        mSnackbar = Snackbar.make(mCoordinatorLayout!!, msg, Snackbar.LENGTH_INDEFINITE)
        mSnackbar!!.setAction(res.getString(R.string.undo), undoDeletion)
        mSnackbar!!.setActionTextColor(Color.WHITE)
        mSnackbar!!.show()
        updateEvents(mEvents)*/
    }

    override fun goLeft() {
        view_pager.currentItem = view_pager.currentItem - 1
    }

    override fun goRight() {
        view_pager.currentItem = view_pager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {

    }
}
