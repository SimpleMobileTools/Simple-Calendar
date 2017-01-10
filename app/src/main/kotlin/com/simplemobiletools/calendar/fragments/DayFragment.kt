package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.RelativeLayout
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.adapters.DayEventsAdapter
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.getAppropriateTheme
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.interfaces.NavigationListener
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.RecyclerViewDivider
import kotlinx.android.synthetic.main.fragment_day.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import org.joda.time.DateTime
import java.util.*
import kotlin.comparisons.compareBy

class DayFragment : Fragment(), DBHelper.EventUpdateListener, DBHelper.GetEventsListener, DayEventsAdapter.ItemOperationsListener {
    private var mTextColor = 0
    private var mDayCode = ""
    private var mEvents: MutableList<Event>? = null
    private var mListener: DeleteListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    lateinit var mConfig: Config
    lateinit var mToBeDeleted: MutableList<Int>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_day, container, false)
        mRes = resources
        mHolder = view.day_holder

        mConfig = context.config
        mDayCode = arguments.getString(DAY_CODE)

        val day = Formatter.getDayTitle(activity.applicationContext, mDayCode)
        mHolder.top_value.text = day
        mHolder.top_value.setOnClickListener { pickDay() }
        mHolder.top_value.setTextColor(mConfig.textColor)
        mToBeDeleted = ArrayList<Int>()

        setupButtons()
        return view
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
    }

    private fun setupButtons() {
        mTextColor = mConfig.textColor

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
        activity.setTheme(context.getAppropriateTheme())
        val view = getLayoutInflater(arguments).inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById(R.id.date_picker) as DatePicker

        val dateTime = Formatter.getDateTimeFromCode(mDayCode)
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth, null)

        AlertDialog.Builder(context)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { dialog, which -> positivePressed(dateTime, datePicker) }
                .create().apply {
            context.setupDialogStuff(view, this)
        }
    }

    private fun positivePressed(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val day = datePicker.dayOfMonth
        val newDateTime = dateTime.withDate(year, month, day)
        mListener?.goToDateTime(newDateTime)
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

        val eventsAdapter = DayEventsAdapter(activity as SimpleActivity, eventsToShow, this) {
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
            startActivity(this)
        }
    }

    private fun getEventsToShow(events: MutableList<Event>) = events.filter { !mToBeDeleted.contains(it.id) }

    override fun prepareForDeleting(ids: ArrayList<Int>) {
        mToBeDeleted = ids
        notifyDeletion()
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
