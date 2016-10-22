package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.DBHelper
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.models.Event

class EventListFragment : Fragment(), DBHelper.GetEventsListener {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_event_list, container, false)
        return view
    }

    override fun gotEvents(events: MutableList<Event>) {

    }
}
