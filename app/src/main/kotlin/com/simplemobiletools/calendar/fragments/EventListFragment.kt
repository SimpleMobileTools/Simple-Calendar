package com.simplemobiletools.calendar.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.view.*
import android.widget.AbsListView
import android.widget.AdapterView
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.activities.MainActivity
import com.simplemobiletools.calendar.adapters.EventsListAdapter
import com.simplemobiletools.calendar.extensions.beGoneIf
import com.simplemobiletools.calendar.extensions.beVisibleIf
import com.simplemobiletools.calendar.extensions.updateWidget
import com.simplemobiletools.calendar.helpers.DBHelper
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

class EventListFragment : Fragment(), DBHelper.GetEventsListener, AdapterView.OnItemClickListener, AbsListView.MultiChoiceModeListener, DBHelper.EventsListener {
    private val EDIT_EVENT = 1

    var mSelectedItemsCnt = 0
    var mListItems: ArrayList<ListItem> = ArrayList()
    var mAllEvents: MutableList<Event>? = null
    var mState: Parcelable? = null
    lateinit var mToBeDeleted: MutableList<Int>
    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater!!.inflate(R.layout.fragment_event_list, container, false)
        mToBeDeleted = ArrayList<Int>()
        return mView
    }

    override fun onResume() {
        super.onResume()
        checkEvents()
    }

    override fun onPause() {
        super.onPause()
        mState = mView.calendar_events_list.onSaveInstanceState()
    }

    private fun checkEvents() {
        val fromTS = (DateTime().millis / 1000).toInt()
        val toTS = (DateTime().plusYears(1).millis / 1000).toInt()
        DBHelper(context).getEvents(fromTS, toTS, this)
    }

    override fun gotEvents(events: MutableList<Event>) {
        val filtered = getEventsToShow(events)
        mListItems = ArrayList<ListItem>(filtered.size)
        val sorted = filtered.sortedWith(compareBy({ it.startTS }, { it.endTS }))
        var prevCode = ""
        sorted.forEach {
            val code = Formatter.getDayCodeFromTS(it.startTS)
            if (code != prevCode) {
                val day = Formatter.getDayTitle(context, code)
                mListItems.add(ListSection(day, false))
                prevCode = code
            }
            mListItems.add(ListEvent(it.id, it.startTS, it.endTS, it.title, it.description))
        }

        mAllEvents = events
        val eventsAdapter = EventsListAdapter(context, mListItems)
        activity?.runOnUiThread {
            mView.calendar_events_list.apply {
                adapter = eventsAdapter
                onItemClickListener = this@EventListFragment
                setMultiChoiceModeListener(this@EventListFragment)

                if (mState != null)
                    onRestoreInstanceState(mState)
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

    private fun prepareDeleteEvents() {
        val checked = mView.calendar_events_list.checkedItemPositions
        mListItems.indices.filter { checked.get(it) }
                .map { mListItems[it] }
                .forEach { mToBeDeleted.add((it as ListEvent).id) }

        notifyDeletion()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        (activity as MainActivity).checkDeleteEvents()
        editEvent((mListItems[position] as ListEvent).id)
    }

    private fun editEvent(eventId: Int) {
        val intent = Intent(activity.applicationContext, EventActivity::class.java)
        intent.putExtra(Constants.EVENT_ID, eventId)
        startActivityForResult(intent, EDIT_EVENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EDIT_EVENT && resultCode == Activity.RESULT_OK && data != null) {
            val deletedId = data.getIntExtra(DayFragment.DELETED_ID, -1)
            if (deletedId != -1) {
                mToBeDeleted.clear()
                mToBeDeleted.add(deletedId)
                notifyDeletion()
            }
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

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = true

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete -> {
                prepareDeleteEvents()
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

    override fun eventInserted(event: Event) {
        checkEvents()
        context.updateWidget()
    }

    override fun eventUpdated(event: Event) {
        checkEvents()
        context.updateWidget()
    }

    override fun eventsDeleted(cnt: Int) {
        checkPlaceholderVisibility()
        context.updateWidget()
    }

    interface DeleteListener : NavigationListener {
        fun notifyDeletion(cnt: Int)
    }
}
