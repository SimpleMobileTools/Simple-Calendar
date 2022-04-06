package com.simplemobiletools.calendar.pro.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.adapters.DayEventsAdapter
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.extensions.getViewBitmap
import com.simplemobiletools.calendar.pro.extensions.printBitmap
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.fragment_day.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import java.util.*

class DayFragment : Fragment() {
    var mListener: NavigationListener? = null
    private var mTextColor = 0
    private var mDayCode = ""
    private var lastHash = 0

    private lateinit var mHolder: RelativeLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_day, container, false)
        mHolder = view.day_holder

        mDayCode = requireArguments().getString(DAY_CODE)!!
        setupButtons()
        return view
    }

    override fun onResume() {
        super.onResume()
        updateCalendar()
    }

    private fun setupButtons() {
        mTextColor = requireContext().getProperTextColor()

        mHolder.top_left_arrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goLeft()
            }

            val pointerLeft = requireContext().getDrawable(R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
        }

        mHolder.top_right_arrow.apply {
            applyColorFilter(mTextColor)
            background = null
            setOnClickListener {
                mListener?.goRight()
            }

            val pointerRight = requireContext().getDrawable(R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
        }

        val day = Formatter.getDayTitle(requireContext(), mDayCode)
        mHolder.top_value.apply {
            text = day
            contentDescription = text
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
            setTextColor(context.getProperTextColor())
        }
    }

    fun updateCalendar() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        context?.eventsHelper?.getEvents(startTS, endTS) {
            receivedEvents(it)
        }
    }

    private fun receivedEvents(events: List<Event>) {
        val newHash = events.hashCode()
        if (newHash == lastHash || !isAdded) {
            return
        }
        lastHash = newHash

        val replaceDescription = requireContext().config.replaceDescription
        val sorted = ArrayList(events.sortedWith(compareBy({ !it.getIsAllDay() }, { it.startTS }, { it.endTS }, { it.title }, {
            if (replaceDescription) it.location else it.description
        })))

        activity?.runOnUiThread {
            updateEvents(sorted)
        }
    }

    private fun updateEvents(events: ArrayList<Event>) {
        if (activity == null)
            return

        DayEventsAdapter(activity as SimpleActivity, events, mHolder.day_events) {
            editEvent(it as Event)
        }.apply {
            mHolder.day_events.adapter = this
        }

        if (requireContext().areSystemAnimationsEnabled) {
            mHolder.day_events.scheduleLayoutAnimation()
        }
    }

    private fun editEvent(event: Event) {
        Intent(context, getActivityToOpen(event.isTask())).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            startActivity(this)
        }
    }

    fun printCurrentView() {
        mHolder.apply {
            top_left_arrow.beGone()
            top_right_arrow.beGone()
            top_value.setTextColor(resources.getColor(R.color.theme_light_text_color))
            (day_events.adapter as? DayEventsAdapter)?.togglePrintMode()

            Handler().postDelayed({
                requireContext().printBitmap(day_holder.getViewBitmap())

                Handler().postDelayed({
                    top_left_arrow.beVisible()
                    top_right_arrow.beVisible()
                    top_value.setTextColor(requireContext().getProperTextColor())
                    (day_events.adapter as? DayEventsAdapter)?.togglePrintMode()
                }, 1000)
            }, 1000)
        }
    }
}
