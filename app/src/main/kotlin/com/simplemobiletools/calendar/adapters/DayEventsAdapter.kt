package com.simplemobiletools.calendar.adapters

import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.dialogs.DeleteEventDialog
import com.simplemobiletools.calendar.extensions.shareEvents
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.interfaces.DeleteEventsListener
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_item_day_view.view.*

class DayEventsAdapter(activity: SimpleActivity, val events: List<Event>, val listener: DeleteEventsListener?, recyclerView: MyRecyclerView,
                       itemClick: (Any) -> Unit) : MyAdapter(activity, itemClick) {

    private var allDayString = resources.getString(R.string.all_day)
    private var replaceDescriptionWithLocation = config.replaceDescription

    init {
        setDragListenerRecyclerView(recyclerView)
    }

    override fun getActionMenuId() = R.menu.cab_day

    override fun getSelectableItemCount() = events.size

    override fun markItemSelection(select: Boolean, pos: Int) {
        itemViews[pos].event_item_frame.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(R.layout.event_item_day_view, parent, false)
        return createViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyAdapter.ViewHolder, position: Int) {
        val event = events[position]
        val view = holder.bindView(event) {
            setupView(it, event)
        }
        itemViews.put(position, view)
        toggleItemSelection(selectedPositions.contains(position), position)
        holder.itemView.tag = holder
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

            event_item_start.setTextColor(textColor)
            event_item_end.setTextColor(textColor)
            event_section_title.setTextColor(textColor)
            event_item_description.setTextColor(textColor)
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
        selectedPositions.forEach {
            eventIds.add(events[it].id)
            timestamps.add(events[it].startTS)
        }

        DeleteEventDialog(activity, eventIds) {
            if (it) {
                listener?.deleteItems(eventIds)
            } else {
                listener?.addEventRepeatException(eventIds, timestamps)
            }
            finishActMode()
        }
    }
}
