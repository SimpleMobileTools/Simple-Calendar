package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.RelativeLayout
import com.simplemobiletools.calendar.*
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.adapters.EventsAdapter
import com.simplemobiletools.calendar.models.Event
import kotlinx.android.synthetic.main.day_fragment.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import java.util.*

class DayFragment : Fragment(), DBHelper.DBOperationsListener, AdapterView.OnItemClickListener,
        AbsListView.MultiChoiceModeListener {
    private var mDayCode: String = ""
    private var mEvents: MutableList<Event>? = null
    private var mListener: NavigationListener? = null

    lateinit var mHolder: RelativeLayout

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.day_fragment, container, false)
        mHolder = view.day_holder
        mDayCode = arguments.getString(Constants.DAY_CODE)

        val day = Formatter.getEventDate(activity.applicationContext, mDayCode)
        view.month_value.text = day

        checkEvents()

        return view
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }

    private fun checkEvents() {
        val startTS = Formatter.getDayStartTS(mDayCode)
        val endTS = Formatter.getDayEndTS(mDayCode)
        DBHelper.newInstance(activity.applicationContext, this).getEvents(startTS, endTS)
    }

    private fun updateEvents(events: MutableList<Event>) {
        mEvents = ArrayList(events)
        val eventsToShow = getEventsToShow(events)
        val eventsAdapter = EventsAdapter(activity.applicationContext, eventsToShow)
        mHolder.day_events.apply {
            adapter = eventsAdapter
            onItemClickListener = this@DayFragment
            setMultiChoiceModeListener(this@DayFragment)
        }
    }

    private fun getEventsToShow(events: MutableList<Event>): List<Event> {
        /*val cnt = events.size
        for (i in cnt - 1 downTo 0) {
            if (mToBeDeleted!!.contains(events[i].id)) {
                events.removeAt(i)
            }
        }*/
        return events
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                //prepareDeleteEvents()
                mode.finish()
                return true
            }
            else -> return false
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        //checkDeleteEvents()
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.menu_day_cab, menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        //mSelectedItemsCnt = 0
    }

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
        /*if (checked) {
            mSelectedItemsCnt++
        } else {
            mSelectedItemsCnt--
        }

        mode.title = mSelectedItemsCnt.toString()
        mode.invalidate()*/
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        //editEvent(getEventsToShow(mEvents)[position])
    }

    override fun eventInserted(event: Event?) {

    }

    override fun eventUpdated(event: Event?) {

    }

    override fun eventsDeleted(cnt: Int) {
        checkEvents()
    }

    override fun gotEvents(events: MutableList<Event>) {
        updateEvents(events)
    }
}
