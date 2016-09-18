package com.simplemobiletools.calendar.fragments

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.*
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.DatePicker
import android.widget.RelativeLayout
import com.simplemobiletools.calendar.*
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.adapters.EventsAdapter
import com.simplemobiletools.calendar.models.Event
import kotlinx.android.synthetic.main.day_fragment.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import java.util.*

class DayFragment : Fragment(), DBHelper.DBOperationsListener, AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {
    private val EDIT_EVENT = 1

    private var mTextColor: Int = 0
    private var mSelectedItemsCnt: Int = 0
    private var mDayCode: String = ""
    private var mEvents: MutableList<Event>? = null
    private var mListener: DeleteListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    lateinit var mConfig: Config
    lateinit var mToBeDeleted: MutableList<Int>

    companion object {
        val DELETED_ID = "deleted_id"
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.day_fragment, container, false)
        mRes = resources
        mHolder = view.day_holder

        mConfig = Config.newInstance(context)
        mDayCode = arguments.getString(Constants.DAY_CODE)

        val day = Formatter.getEventDate(activity.applicationContext, mDayCode)
        mHolder.month_value.text = day
        mHolder.month_value.setOnClickListener { pickDay() }

        setupButtons()
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mToBeDeleted = ArrayList<Int>()
        checkEvents()
    }

    private fun setupButtons() {
        val baseColor = if (mConfig.isDarkTheme) Color.WHITE else Color.BLACK
        mTextColor = Utils.adjustAlpha(baseColor, Constants.HIGH_ALPHA)

        mHolder.apply {
            top_left_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
            top_right_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
            top_left_arrow.background = null
            top_right_arrow.background = null

            top_left_arrow.setOnClickListener {
                mListener?.goLeft()
            }

            top_right_arrow.setOnClickListener {
                mListener?.goRight()
            }
        }
    }

    fun setListener(listener: DeleteListener) {
        mListener = listener
    }

    fun pickDay() {
        val theme = if (mConfig.isDarkTheme) R.style.MyAlertDialog_Dark else R.style.MyAlertDialog
        val alertDialog = AlertDialog.Builder(context, theme)
        val view = getLayoutInflater(arguments).inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById(R.id.date_picker) as DatePicker

        val dateTime = Formatter.getDateTimeFromCode(mDayCode)
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth, null)

        alertDialog.apply {
            setView(view)
            setNegativeButton(R.string.cancel, null)
            setPositiveButton(R.string.ok) { dialog, id ->
                val month = datePicker.month + 1
                val year = datePicker.year
                val day = datePicker.dayOfMonth
                val newDateTime = dateTime.withDayOfMonth(day).withMonthOfYear(month).withYear(year)
                mListener?.goToDateTime(newDateTime)
            }

            show()
        }
    }

    private fun checkEvents() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        DBHelper.newInstance(activity.applicationContext, this).getEvents(startTS, endTS)
    }

    private fun updateEvents(events: MutableList<Event>) {
        for (e in events) {
            Log.e("DEBUG", "GOT EVENT $e")
        }
        mEvents = ArrayList(events)
        val eventsToShow = getEventsToShow(events)
        val eventsAdapter = EventsAdapter(activity.baseContext, eventsToShow)
        mHolder.day_events.apply {
            adapter = eventsAdapter
            onItemClickListener = this@DayFragment
            setMultiChoiceModeListener(this@DayFragment)
        }
    }

    private fun editEvent(event: Event) {
        val intent = Intent(activity.applicationContext, EventActivity::class.java)
        intent.putExtra(Constants.EVENT, event)
        startActivityForResult(intent, EDIT_EVENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EDIT_EVENT && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                checkEvents()
            } else {
                val deletedId = data.getIntExtra(DELETED_ID, -1)
                if (deletedId != -1) {
                    mToBeDeleted.clear()
                    mToBeDeleted.add(deletedId)
                    notifyDeletion()
                }
            }
        }
    }

    private fun getEventsToShow(events: MutableList<Event>): List<Event> {
        return events.filter { !mToBeDeleted.contains(it.id) }
    }

    private fun prepareDeleteEvents() {
        val checked = mHolder.day_events.checkedItemPositions
        for (i in mEvents!!.indices) {
            if (checked.get(i)) {
                val event = mEvents!![i]
                mToBeDeleted.add(event.id)
            }
        }

        notifyDeletion()
    }

    private fun notifyDeletion() {
        mListener?.notifyDeletion(mToBeDeleted.size)
        updateEvents(mEvents!!)
    }

    fun deleteEvents() {
        val cnt = mToBeDeleted.size
        val eventIDs = arrayOfNulls<String>(cnt)
        for (i in 0..cnt - 1) {
            eventIDs[i] = mToBeDeleted[i].toString()
        }

        DBHelper.newInstance(activity.applicationContext, this).deleteEvents(eventIDs)
    }

    fun undoDeletion() {
        mToBeDeleted.clear()
        updateEvents(mEvents!!)
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

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        //checkDeleteEvents()
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.menu_day_cab, menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mSelectedItemsCnt = 0
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

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        editEvent(getEventsToShow(mEvents!!)[position])
    }

    override fun eventInserted(event: Event?) {

    }

    override fun eventUpdated(event: Event?) {

    }

    override fun eventsDeleted(cnt: Int) {
        checkEvents()
    }

    override fun gotEvents(events: MutableList<Event>) {
        updateEvents(events)
    }

    interface DeleteListener : NavigationListener {
        fun notifyDeletion(cnt: Int)
    }
}
