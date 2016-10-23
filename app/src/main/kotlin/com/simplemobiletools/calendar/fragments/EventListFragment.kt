package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.DBHelper
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.EventsListAdapter
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime
import java.util.*
import kotlin.comparisons.compareBy

class EventListFragment : Fragment(), DBHelper.GetEventsListener {
    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater!!.inflate(R.layout.fragment_event_list, container, false)
        return mView
    }

    override fun onResume() {
        super.onResume()
        val fromTS = (DateTime().millis / 1000).toInt()
        val toTS = (DateTime().plusYears(1).millis / 1000).toInt()
        DBHelper(context).getEvents(fromTS, toTS, this)
    }

    override fun gotEvents(events: MutableList<Event>) {
        val listItems = ArrayList<ListItem>(events.size)
        val sorted = events.sortedWith(compareBy({ it.startTS }, { it.endTS }))
        sorted.forEach { listItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description)) }

        val eventsAdapter = EventsListAdapter(context, listItems)
        activity?.runOnUiThread {
            mView.calendar_events_list.adapter = eventsAdapter
        }
    }
}
