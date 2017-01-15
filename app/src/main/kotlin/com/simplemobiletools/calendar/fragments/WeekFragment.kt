package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.models.Event
import java.util.*
import kotlin.comparisons.compareBy

class WeekFragment : Fragment(), DBHelper.GetEventsListener {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_week, container, false)

        return view
    }

    override fun gotEvents(events: MutableList<Event>) {
        val sorted = ArrayList<Event>(events.sortedWith(compareBy({ it.startTS }, { it.endTS }, { it.title }, { it.description })))
        activity?.runOnUiThread {

        }
    }
}
