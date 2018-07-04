package com.simplemobiletools.calendar.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.dialogs.DeleteEventDialog
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.handleEventDeleting
import com.simplemobiletools.calendar.extensions.shareEvents
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_item_day_view.view.*

class DayEventsAdapter(activity: SimpleActivity, val events: ArrayList<Event>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit)
    : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private val allDayString = resources.getString(R.string.all_day)
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_day

    override fun prepareActionMode(menu: Menu) {}

    override fun prepareItemSelection(viewHolder: ViewHolder) {}

    override fun markViewHolderSelection(select: Boolean, viewHolder: ViewHolder?) {
        viewHolder?.itemView?.event_item_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = events.size

    override fun getIsItemSelectable(position: Int) = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.event_item_day_view, parent)

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val event = events[position]
        val view = holder.bindView(event, true, true) { itemView, layoutPosition ->
            setupView(itemView, event)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = events.size

    private fun setupView(view: View, event: Event) {
        view.apply {
            event_section_title.text = event.title
            event_item_description.text = if (replaceDescriptionWithLocation) event.location else event.description
            event_item_start.text = if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(context, event.startTS)
            event_item_end.beInvisibleIf(event.startTS == event.endTS)
            event_item_color.applyColorFilter(event.color)

            if (event.startTS != event.endTS) {
                val startCode = Formatter.getDayCodeFromTS(event.startTS)
                val endCode = Formatter.getDayCodeFromTS(event.endTS)

                event_item_end.apply {
                    text = Formatter.getTimeFromTS(context, event.endTS)
                    if (startCode != endCode) {
                        if (event.getIsAllDay()) {
                            text = Formatter.getDateFromCode(context, endCode, true)
                        } else {
                            append(" (${Formatter.getDateFromCode(context, endCode, true)})")
                        }
                    } else if (event.getIsAllDay()) {
                        beInvisible()
                    }
                }
            }

            var newTextColor = textColor
            if (dimPastEvents && event.isPastEvent) {
                newTextColor = newTextColor.adjustAlpha(LOW_ALPHA)
            }

            event_item_start.setTextColor(newTextColor)
            event_item_end.setTextColor(newTextColor)
            event_section_title.setTextColor(newTextColor)
            event_item_description.setTextColor(newTextColor)
        }
    }

    private fun shareEvents() {
        val eventIds = ArrayList<Int>(selectedPositions.size)
        selectedPositions.forEach {
            eventIds.add(events[it].id)
        }
        activity.shareEvents(eventIds.distinct())
    }

    private fun askConfirmDelete() {
        val eventIds = ArrayList<Int>(selectedPositions.size)
        val timestamps = ArrayList<Int>(selectedPositions.size)
        val eventsToDelete = ArrayList<Event>(selectedPositions.size)
        selectedPositions.forEach {
            val event = events[it]
            eventsToDelete.add(event)
            eventIds.add(event.id)
            timestamps.add(event.startTS)
        }

        val hasRepeatableEvent = eventsToDelete.any { it.repeatInterval > 0 }
        DeleteEventDialog(activity, eventIds, hasRepeatableEvent) {
            events.removeAll(eventsToDelete)

            val nonRepeatingEventIDs = eventsToDelete.filter { it.repeatInterval == 0 }.map { it.id.toString() }.toTypedArray()
            activity.dbHelper.deleteEvents(nonRepeatingEventIDs, true)

            val repeatingEventIDs = eventsToDelete.filter { it.repeatInterval != 0 }.map { it.id }
            activity.handleEventDeleting(repeatingEventIDs, timestamps, it)
            removeSelectedItems()
        }
    }
}
