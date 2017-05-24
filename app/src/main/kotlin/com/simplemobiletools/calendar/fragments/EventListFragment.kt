package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.adapters.EventListAdapter
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.getFilteredEvents
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.helpers.EVENT_ID
import com.simplemobiletools.calendar.helpers.EVENT_OCCURRENCE_TS
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.interfaces.DeleteEventsListener
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime
import java.util.*

class EventListFragment : Fragment(), DBHelper.EventUpdateListener, DeleteEventsListener {
    private var mEvents: List<Event> = ArrayList()
    private var prevEventsHash = 0
    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater!!.inflate(R.layout.fragment_event_list, container, false)
        val placeholderText = String.format(getString(R.string.two_string_placeholder), "${getString(R.string.no_upcoming_events)}\n", getString(R.string.add_some_events))
        mView.calendar_empty_list_placeholder.text = placeholderText
        return mView
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
    }

    private fun checkEvents() {
        val fromTS = DateTime().seconds() - context.config.displayPastEvents * 60
        val toTS = DateTime().plusYears(1).seconds()
        context.dbHelper.getEvents(fromTS, toTS) {
            receivedEvents(it)
        }
    }

    override fun gotEvents(events: MutableList<Event>) {
        receivedEvents(events)
    }

    private fun receivedEvents(events: MutableList<Event>) {
        if (context == null || activity == null)
            return

        val filtered = context.getFilteredEvents(events)
        val hash = filtered.hashCode()
        if (prevEventsHash == hash)
            return

        prevEventsHash = hash
        mEvents = filtered
        val listItems = ArrayList<ListItem>(mEvents.size)
        val sorted = mEvents.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { it.description }))
        val sublist = sorted.subList(0, Math.min(sorted.size, 100))
        var prevCode = ""
        sublist.forEach {
            val code = Formatter.getDayCodeFromTS(it.startTS)
            if (code != prevCode) {
                val day = Formatter.getDayTitle(context, code)
                listItems.add(ListSection(day))
                prevCode = code
            }
            listItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description, it.isAllDay))
        }

        val eventsAdapter = EventListAdapter(activity as SimpleActivity, listItems, this) { eventId, eventTS ->
            editEvent(eventId, eventTS)
        }
        activity?.runOnUiThread {
            mView.calendar_events_list.apply {
                this@apply.adapter = eventsAdapter
            }
            checkPlaceholderVisibility()
        }
    }

    private fun checkPlaceholderVisibility() {
        mView.calendar_empty_list_placeholder.beVisibleIf(mEvents.isEmpty())
        mView.calendar_events_list.beGoneIf(mEvents.isEmpty())
        if (activity != null)
            mView.calendar_empty_list_placeholder.setTextColor(activity.config.textColor)
    }

    private fun editEvent(eventId: Int, eventTS: Int) {
        Intent(activity.applicationContext, EventActivity::class.java).apply {
            putExtra(EVENT_OCCURRENCE_TS, eventTS)
            putExtra(EVENT_ID, eventId)
            startActivity(this)
        }
    }

    override fun deleteItems(ids: ArrayList<Int>) {
        val eventIDs = Array(ids.size, { i -> (ids[i].toString()) })
        DBHelper.newInstance(activity.applicationContext, this).deleteEvents(eventIDs)
    }

    override fun addEventRepeatException(parentIds: ArrayList<Int>, timestamps: ArrayList<Int>) {
        parentIds.forEachIndexed { index, value ->
            context.dbHelper.addEventRepeatException(value, timestamps[index])
        }
        checkEvents()
    }

    override fun eventInserted(event: Event) {
        checkEvents()
    }

    override fun eventUpdated(event: Event) {
        checkEvents()
    }

    override fun eventsDeleted(cnt: Int) {
        checkEvents()
        checkPlaceholderVisibility()
    }
}
