package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.DBHelper
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.EventsAdapter
import com.simplemobiletools.calendar.models.Event
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime

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
        val eventsAdapter = EventsAdapter(context, events)
        activity?.runOnUiThread {
            mView.calendar_events_list.adapter = eventsAdapter
        }
    }
}
