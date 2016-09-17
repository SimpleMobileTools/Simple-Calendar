package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import com.simplemobiletools.calendar.*
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.adapters.MyDayPagerAdapter
import com.simplemobiletools.calendar.models.Event
import kotlinx.android.synthetic.main.activity_day.*
import org.joda.time.DateTime
import java.util.*

class DayActivity : SimpleActivity(), NavigationListener, DBHelper.DBOperationsListener {

    private val PREFILLED_DAYS = 61
    private var mDayCode: String? = null
    private var mEvents: MutableList<Event>? = null
    private var mSelectedItemsCnt: Int = 0
    private var mSnackbar: Snackbar? = null
    private var mToBeDeleted: MutableList<Int>? = null
    private val EDIT_EVENT = 1

    companion object {
        val DELETED_ID = "deleted_id"
    }

    /*@BindView(R.id.month_value) internal var mDateTV: TextView? = null
    @BindView(R.id.day_events) internal var mEventsList: ListView? = null
    @BindView(R.id.day_coordinator) internal var mCoordinatorLayout: CoordinatorLayout? = null
    @BindView(R.id.top_left_arrow) internal var mLeftArrow: ImageView? = null
    @BindView(R.id.top_right_arrow) internal var mRightArrow: ImageView? = null

    @BindDimen(R.dimen.activity_margin) internal var mActivityMargin: Int = 0*/

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

        /*mToBeDeleted = ArrayList<Int>()

        val baseColor = if (mConfig.isDarkTheme) Color.WHITE else Color.BLACK
        val textColor = Utils.adjustAlpha(baseColor, Constants.HIGH_ALPHA)
        mLeftArrow!!.drawable.mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP)
        mRightArrow!!.drawable.mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_ATOP)

        var params = mLeftArrow!!.layoutParams as RelativeLayout.LayoutParams
        params.setMargins(mActivityMargin, params.topMargin, params.rightMargin, params.bottomMargin)

        params = mRightArrow!!.layoutParams as RelativeLayout.LayoutParams
        params.setMargins(params.leftMargin, params.topMargin, mActivityMargin, params.bottomMargin)*/
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
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

    private fun editEvent(event: Event) {
        val intent = Intent(applicationContext, EventActivity::class.java)
        intent.putExtra(Constants.EVENT, event)
        startActivityForResult(intent, EDIT_EVENT)
    }

    private fun checkEvents() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        DBHelper.newInstance(applicationContext, this).getEvents(startTS, endTS)
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
        if (requestCode == EDIT_EVENT && resultCode == Activity.RESULT_OK && data != null) {
            val deletedId = data.getIntExtra(DELETED_ID, -1)
            if (deletedId != -1) {
                mToBeDeleted!!.clear()
                mToBeDeleted!!.add(deletedId)
                notifyEventDeletion(1)
            }
        }
    }

    override fun eventInserted(event: Event) {

    }

    override fun eventUpdated(event: Event) {

    }

    override fun eventsDeleted(cnt: Int) {
        checkEvents()
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

        val cnt = mToBeDeleted!!.size
        val eventIDs = arrayOfNulls<String>(cnt)
        for (i in 0..cnt - 1) {
            eventIDs[i] = mToBeDeleted!![i].toString()
        }

        DBHelper.newInstance(applicationContext, this).deleteEvents(eventIDs)
    }

    private val undoDeletion = View.OnClickListener { undoDeletion() }

    private fun undoDeletion() {
        if (mSnackbar != null) {
            mToBeDeleted!!.clear()
            mSnackbar!!.dismiss()
            //updateEvents(mEvents)
        }
    }

    override fun gotEvents(events: MutableList<Event>) {
        //updateEvents(events)
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
