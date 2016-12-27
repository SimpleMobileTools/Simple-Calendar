package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.activities.MainActivity
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.adapters.EventListAdapter
import com.simplemobiletools.calendar.extensions.beGoneIf
import com.simplemobiletools.calendar.extensions.beVisibleIf
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.interfaces.NavigationListener
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime
import java.util.*
import kotlin.comparisons.compareBy

class EventListFragment : Fragment(), DBHelper.GetEventsListener, DBHelper.EventUpdateListener, EventListAdapter.ItemOperationsListener {
    var mAllEvents: MutableList<Event>? = null
    lateinit var mToBeDeleted: MutableList<Int>
    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater!!.inflate(R.layout.fragment_event_list, container, false)
        val placeholderText = String.format(getString(R.string.two_string_placeholder), "${getString(R.string.no_upcoming_events)}\n", getString(R.string.add_some_events))
        mView.calendar_empty_list_placeholder.text = placeholderText
        mToBeDeleted = ArrayList<Int>()
        return mView
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun checkEvents() {
        val fromTS = (DateTime().millis / 1000).toInt()
        val toTS = (DateTime().plusMonths(6).millis / 1000).toInt()
        DBHelper(context).getEvents(fromTS, toTS, this)
    }

    override fun gotEvents(events: MutableList<Event>) {
        val filtered = getEventsToShow(events)
        val listItems = ArrayList<ListItem>(filtered.size)
        val sorted = filtered.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { it.description }))
        val sublist = sorted.subList(0, Math.min(sorted.size, 50))
        var prevCode = ""
        sublist.forEach {
            val code = Formatter.getDayCodeFromTS(it.startTS)
            if (code != prevCode) {
                val day = Formatter.getDayTitle(context, code)
                listItems.add(ListSection(day))
                prevCode = code
            }
            listItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description))
        }

        mAllEvents = events
        val eventsAdapter = EventListAdapter(activity as SimpleActivity, listItems, this) {
            (activity as MainActivity).checkDeleteEvents()
            editEvent(it)
        }
        activity?.runOnUiThread {
            mView.calendar_events_list.apply {
                this@apply.adapter = eventsAdapter
            }
            checkPlaceholderVisibility()
        }
    }

    private fun checkPlaceholderVisibility() {
        val events = getEventsToShow(mAllEvents)
        mView.calendar_empty_list_placeholder.beVisibleIf(events.isEmpty())
        mView.calendar_events_list.beGoneIf(events.isEmpty())
    }

    private fun getEventsToShow(events: MutableList<Event>?) = events?.filter { !mToBeDeleted.contains(it.id) } ?: ArrayList()

    override fun prepareForDeleting(ids: ArrayList<Int>) {
        mToBeDeleted = ids
        notifyDeletion()
    }

    private fun editEvent(eventId: Int) {
        Intent(activity.applicationContext, EventActivity::class.java).apply {
            putExtra(EVENT_ID, eventId)
            startActivity(this)
        }
    }

    private fun notifyDeletion() {
        (activity as MainActivity).notifyDeletion(mToBeDeleted.size)
        checkEvents()
    }

    fun deleteEvents() {
        if (activity == null)
            return

        val eventIDs = Array(mToBeDeleted.size, { i -> (mToBeDeleted[i].toString()) })
        DBHelper(activity.applicationContext, this).deleteEvents(eventIDs)
        mToBeDeleted.clear()
    }

    fun undoDeletion() {
        mToBeDeleted.clear()
        checkEvents()
    }

    override fun eventInserted(event: Event) {
        checkEvents()
    }

    override fun eventUpdated(event: Event) {
        checkEvents()
    }

    override fun eventsDeleted(cnt: Int) {
        checkPlaceholderVisibility()
    }

    interface DeleteListener : NavigationListener {
        fun notifyDeletion(cnt: Int)
    }
}
