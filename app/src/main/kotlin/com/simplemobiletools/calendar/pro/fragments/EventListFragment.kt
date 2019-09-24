package com.simplemobiletools.calendar.pro.fragments

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.EventActivity
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.adapters.EventListAdapter
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.EVENT_ID
import com.simplemobiletools.calendar.pro.helpers.EVENT_OCCURRENCE_TS
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.ListEvent
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.MONTH_SECONDS
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime
import java.util.*

class EventListFragment : MyFragmentHolder(), RefreshRecyclerViewListener {
    private var FETCH_INTERVAL = 3 * MONTH_SECONDS
    private var MIN_EVENTS_TRESHOLD = 30

    private var mEvents = ArrayList<Event>()
    private var minFetchedTS = 0L
    private var maxFetchedTS = 0L
    private var wereInitialEventsAdded = false

    private var use24HourFormat = false

    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_event_list, container, false)
        mView.background = ColorDrawable(context!!.config.backgroundColor)
        mView.calendar_events_list_holder?.id = (System.currentTimeMillis() % 100000).toInt()
        mView.calendar_empty_list_placeholder_2.apply {
            setTextColor(context.getAdjustedPrimaryColor())
            underlineText()
            setOnClickListener {
                context.launchNewEventIntent(getNewEventDayCode())
            }
        }

        use24HourFormat = context!!.config.use24HourFormat
        updateActionBarTitle()
        return mView
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
        val use24Hour = context!!.config.use24HourFormat
        if (use24Hour != use24HourFormat) {
            use24HourFormat = use24Hour
            (mView.calendar_events_list.adapter as? EventListAdapter)?.toggle24HourFormat(use24HourFormat)
        }
    }

    override fun onPause() {
        super.onPause()
        use24HourFormat = context!!.config.use24HourFormat
    }

    private fun checkEvents() {
        if (!wereInitialEventsAdded) {
            minFetchedTS = DateTime().minusMinutes(context!!.config.displayPastEvents).seconds()
            maxFetchedTS = DateTime().plusMonths(6).seconds()
        }

        context!!.eventsHelper.getEvents(minFetchedTS, maxFetchedTS) {
            if (it.size >= MIN_EVENTS_TRESHOLD) {
                receivedEvents(it, false)
            } else {
                if (!wereInitialEventsAdded) {
                    maxFetchedTS += FETCH_INTERVAL
                }
                context!!.eventsHelper.getEvents(minFetchedTS, maxFetchedTS) {
                    mEvents = it
                    receivedEvents(mEvents, false, !wereInitialEventsAdded)
                }
            }
            wereInitialEventsAdded = true
        }
    }

    private fun receivedEvents(events: ArrayList<Event>, scrollAfterUpdating: Boolean, forceRecreation: Boolean = false) {
        if (context == null || activity == null) {
            return
        }

        mEvents = events
        val listItems = context!!.getEventListItems(mEvents)

        activity?.runOnUiThread {
            if (activity == null) {
                return@runOnUiThread
            }

            val currAdapter = mView.calendar_events_list.adapter
            if (currAdapter == null || forceRecreation) {
                EventListAdapter(activity as SimpleActivity, listItems, true, this, mView.calendar_events_list) {
                    if (it is ListEvent) {
                        editEvent(it)
                    }
                }.apply {
                    mView.calendar_events_list.adapter = this
                }

                mView.calendar_events_list.endlessScrollListener = object : MyRecyclerView.EndlessScrollListener {
                    override fun updateTop() {
                        fetchPreviousPeriod()
                    }

                    override fun updateBottom() {
                        fetchNextPeriod(true)
                    }
                }
            } else {
                (currAdapter as EventListAdapter).updateListItems(listItems)
                if (scrollAfterUpdating) {
                    mView.calendar_events_list.smoothScrollBy(0, context!!.resources.getDimension(R.dimen.endless_scroll_move_height).toInt())
                }
            }
            checkPlaceholderVisibility()
        }
    }

    private fun checkPlaceholderVisibility() {
        mView.calendar_empty_list_placeholder.beVisibleIf(mEvents.isEmpty())
        mView.calendar_empty_list_placeholder_2.beVisibleIf(mEvents.isEmpty())
        mView.calendar_events_list.beGoneIf(mEvents.isEmpty())
        if (activity != null)
            mView.calendar_empty_list_placeholder.setTextColor(activity!!.config.textColor)
    }

    private fun editEvent(event: ListEvent) {
        Intent(context, EventActivity::class.java).apply {
            putExtra(EVENT_ID, event.id)
            putExtra(EVENT_OCCURRENCE_TS, event.startTS)
            startActivity(this)
        }
    }

    private fun fetchPreviousPeriod() {
        val oldMinFetchedTS = minFetchedTS - 1
        minFetchedTS -= FETCH_INTERVAL
        context!!.eventsHelper.getEvents(minFetchedTS, oldMinFetchedTS) {
            mEvents.addAll(0, it)
            receivedEvents(mEvents, false)
        }
    }

    private fun fetchNextPeriod(scrollAfterUpdating: Boolean) {
        val oldMaxFetchedTS = maxFetchedTS + 1
        maxFetchedTS += FETCH_INTERVAL
        context!!.eventsHelper.getEvents(oldMaxFetchedTS, maxFetchedTS) {
            mEvents.addAll(it)
            receivedEvents(mEvents, scrollAfterUpdating)
        }
    }

    override fun refreshItems() {
        checkEvents()
    }

    override fun goToToday() {}

    override fun showGoToDateDialog() {}

    override fun refreshEvents() {
        checkEvents()
    }

    override fun shouldGoToTodayBeVisible() = false

    override fun updateActionBarTitle() {
        (activity as? MainActivity)?.updateActionBarTitle(getString(R.string.app_launcher_name))
    }

    override fun getNewEventDayCode() = Formatter.getTodayCode()
}
