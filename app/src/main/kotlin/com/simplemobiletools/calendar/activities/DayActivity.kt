package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.DBHelper
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.models.Event
import java.util.*

class DayActivity : SimpleActivity(), DBHelper.DBOperationsListener, AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {
    /*@BindView(R.id.month_value) internal var mDateTV: TextView? = null
    @BindView(R.id.day_events) internal var mEventsList: ListView? = null
    @BindView(R.id.day_coordinator) internal var mCoordinatorLayout: CoordinatorLayout? = null
    @BindView(R.id.top_left_arrow) internal var mLeftArrow: ImageView? = null
    @BindView(R.id.top_right_arrow) internal var mRightArrow: ImageView? = null

    @BindDimen(R.dimen.activity_margin) internal var mActivityMargin: Int = 0*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day)
        //ButterKnife.bind(this)

        val intent = intent ?: return

        mDayCode = intent.getStringExtra(Constants.DAY_CODE)
        if (mDayCode == null || mDayCode!!.isEmpty())
            return

        val date = Formatter.getEventDate(applicationContext, mDayCode)
        /*mDateTV!!.text = date
        mToBeDeleted = ArrayList<Int>()

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

    /*@OnClick(R.id.day_fab)
    fun fabClicked(view: View) {
        val intent = Intent(applicationContext, EventActivity::class.java)
        intent.putExtra(Constants.DAY_CODE, mDayCode)
        startActivity(intent)
    }

    @OnClick(R.id.top_left_arrow)
    fun leftArrowClicked() {
        val dateTime = Formatter.getDateTimeFromCode(mDayCode)
        val yesterdayCode = Formatter.getDayCodeFromDateTime(dateTime.minusDays(1))
        switchToDay(yesterdayCode)
    }

    @OnClick(R.id.top_right_arrow)
    fun rightArrowClicked() {
        val dateTime = Formatter.getDateTimeFromCode(mDayCode)
        val tomorrowCode = Formatter.getDayCodeFromDateTime(dateTime.plusDays(1))
        switchToDay(tomorrowCode)
    }

    @OnClick(R.id.month_value)
    fun pickDay() {
        val theme = if (mConfig.isDarkTheme) R.style.MyAlertDialog_Dark else R.style.MyAlertDialog
        val alertDialog = AlertDialog.Builder(this, theme)
        val view = layoutInflater.inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById(R.id.date_picker) as DatePicker

        val dateTime = Formatter.getDateTimeFromCode(mDayCode)
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth, null)

        alertDialog.setView(view)
        alertDialog.setNegativeButton(R.string.cancel, null)
        alertDialog.setPositiveButton(R.string.ok) { dialog, id ->
            val month = datePicker.month + 1
            val year = datePicker.year
            val day = datePicker.dayOfMonth
            val newDateTime = dateTime.withDayOfMonth(day).withMonthOfYear(month).withYear(year)
            val newDayCode = Formatter.getDayCodeFromDateTime(newDateTime)
            switchToDay(newDayCode)
        }

        alertDialog.show()
    }*/

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

    private fun updateEvents(events: MutableList<Event>) {
        mEvents = ArrayList(events)
        /*val eventsToShow = getEventsToShow(events)
        val adapter = EventsAdapter(this, eventsToShow)
        mEventsList!!.adapter = adapter
        mEventsList!!.onItemClickListener = this
        mEventsList!!.setMultiChoiceModeListener(this)*/
    }

    private fun getEventsToShow(events: MutableList<Event>): List<Event> {
        val cnt = events.size
        for (i in cnt - 1 downTo 0) {
            if (mToBeDeleted!!.contains(events[i].id)) {
                events.removeAt(i)
            }
        }
        return events
    }

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
        updateEvents(events)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        //editEvent(getEventsToShow(mEvents)[position])
    }

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
        if (checked) {
            mSelectedItemsCnt++
        } else {
            mSelectedItemsCnt--
        }

        mode.title = mSelectedItemsCnt.toString()
        mode.invalidate()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        checkDeleteEvents()
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.menu_day_cab, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                prepareDeleteEvents()
                mode.finish()
                return true
            }
            else -> return false
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

    override fun onDestroyActionMode(mode: ActionMode) {
        mSelectedItemsCnt = 0
    }

    companion object {
        private val EDIT_EVENT = 1
        val DELETED_ID = "deleted_id"

        private var mDayCode: String? = null
        private var mEvents: MutableList<Event>? = null
        private var mSelectedItemsCnt: Int = 0
        private var mSnackbar: Snackbar? = null
        private var mToBeDeleted: MutableList<Int>? = null
    }
}
