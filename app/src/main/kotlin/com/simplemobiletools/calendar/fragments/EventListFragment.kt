package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.*
import android.widget.AbsListView
import android.widget.AdapterView
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.DBHelper
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.adapters.EventsListAdapter
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import kotlinx.android.synthetic.main.fragment_event_list.view.*
import org.joda.time.DateTime
import java.util.*
import kotlin.comparisons.compareBy

class EventListFragment : Fragment(), DBHelper.GetEventsListener, AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener {
    private val EDIT_EVENT = 1

    var mSelectedItemsCnt = 0
    var mListItems: ArrayList<ListItem> = ArrayList()
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
        mListItems = ArrayList<ListItem>(events.size)
        val sorted = events.sortedWith(compareBy({ it.startTS }, { it.endTS }))
        var prevCode = ""
        sorted.forEach {
            val code = Formatter.getDayCodeFromTS(it.startTS)
            if (code != prevCode) {
                val day = Formatter.getEventDate(context, code)
                mListItems.add(ListSection(day, false))
                prevCode = code
            }
            mListItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description))
        }

        val eventsAdapter = EventsListAdapter(context, mListItems)
        activity?.runOnUiThread {
            mView.calendar_events_list.apply {
                adapter = eventsAdapter
                onItemClickListener = this@EventListFragment
                setMultiChoiceModeListener(this@EventListFragment)
            }
        }
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        editEvent((mListItems[position] as ListEvent).id)
    }

    private fun editEvent(eventId: Int) {
        val intent = Intent(activity.applicationContext, EventActivity::class.java)
        intent.putExtra(Constants.EVENT_ID, eventId)
        startActivityForResult(intent, EDIT_EVENT)
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                mode.finish()
                true
            }
            else -> false
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.menu_day_cab, menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        mSelectedItemsCnt = 0
    }

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
        if (checked) {
            mSelectedItemsCnt++
        } else {
            mSelectedItemsCnt--
        }

        mode.title = mSelectedItemsCnt.toString()
        mode.invalidate()
    }
}
