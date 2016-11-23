package com.simplemobiletools.calendar.fragments

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.*
import android.widget.AbsListView
import android.widget.DatePicker
import android.widget.RelativeLayout
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.adapters.EventsAdapter
import com.simplemobiletools.calendar.extensions.adjustAlpha
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.interfaces.NavigationListener
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.views.RecyclerViewDivider
import kotlinx.android.synthetic.main.day_fragment.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import java.util.*
import kotlin.comparisons.compareBy

class DayFragment : Fragment(), DBHelper.EventsListener, AbsListView.MultiChoiceModeListener, DBHelper.GetEventsListener {

    private val EDIT_EVENT = 1

    private var mTextColor = 0
    private var mSelectedItemsCnt = 0
    private var mDayCode = ""
    private var mEvents: MutableList<Event>? = null
    private var mListener: DeleteListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    lateinit var mConfig: Config
    lateinit var mToBeDeleted: MutableList<Int>

    companion object {
        val DELETED_ID = "deleted_id"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.day_fragment, container, false)
        mRes = resources
        mHolder = view.day_holder

        mConfig = Config.newInstance(context)
        mDayCode = arguments.getString(DAY_CODE)

        val day = Formatter.getDayTitle(activity.applicationContext, mDayCode)
        mHolder.top_value.text = day
        mHolder.top_value.setOnClickListener { pickDay() }
        mToBeDeleted = ArrayList<Int>()

        setupButtons()
        return view
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
    }

    private fun setupButtons() {
        val baseColor = if (mConfig.isDarkTheme) Color.WHITE else Color.BLACK
        mTextColor = baseColor.adjustAlpha(HIGH_ALPHA)

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
                val newDateTime = dateTime.withDate(year, month, day)
                mListener?.goToDateTime(newDateTime)
            }

            show()
        }
    }

    private fun checkEvents() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        DBHelper(context, this).getEvents(startTS, endTS, this)
    }

    private fun updateEvents(events: MutableList<Event>) {
        mEvents = ArrayList(events)
        val eventsToShow = getEventsToShow(events)
        if (activity == null)
            return

        val eventsAdapter = EventsAdapter(activity as SimpleActivity, eventsToShow) {
            editEvent(it.id)
        }
        mHolder.day_events.apply {
            this@apply.adapter = eventsAdapter
            addItemDecoration(RecyclerViewDivider(context))
        }
    }

    private fun editEvent(eventId: Int) {
        Intent(activity.applicationContext, EventActivity::class.java).apply {
            putExtra(EVENT_ID, eventId)
            startActivityForResult(this, EDIT_EVENT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EDIT_EVENT && resultCode == Activity.RESULT_OK && data != null) {
            val deletedId = data.getIntExtra(DELETED_ID, -1)
            if (deletedId != -1) {
                mToBeDeleted.clear()
                mToBeDeleted.add(deletedId)
                notifyDeletion()
            }
        }
    }

    private fun getEventsToShow(events: MutableList<Event>) = events.filter { !mToBeDeleted.contains(it.id) }

    private fun prepareDeleteEvents() {
        /*val checked = mHolder.day_events.checkedItemPositions
        mEvents!!.indices
                .filter { checked.get(it) }
                .map { mEvents!![it] }
                .forEach { mToBeDeleted.add(it.id) }

        notifyDeletion()*/
    }

    private fun notifyDeletion() {
        mListener?.notifyDeletion(mToBeDeleted.size)
        updateEvents(mEvents!!)
    }

    fun deleteEvents() {
        val eventIDs = Array(mToBeDeleted.size, { i -> (mToBeDeleted[i].toString()) })
        DBHelper(activity.applicationContext, this).deleteEvents(eventIDs)
    }

    fun undoDeletion() {
        mToBeDeleted.clear()
        updateEvents(mEvents!!)
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = true

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                prepareDeleteEvents()
                mode.finish()
                true
            }
            else -> false
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.cab_day, menu)
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

    override fun eventInserted(event: Event) {
    }

    override fun eventUpdated(event: Event) {
    }

    override fun eventsDeleted(cnt: Int) {
        checkEvents()
    }

    override fun gotEvents(events: MutableList<Event>) {
        val sorted = ArrayList<Event>(events.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { it.description })))
        activity?.runOnUiThread {
            updateEvents(sorted)
        }
    }

    interface DeleteListener : NavigationListener {
        fun notifyDeletion(cnt: Int)
    }
}
