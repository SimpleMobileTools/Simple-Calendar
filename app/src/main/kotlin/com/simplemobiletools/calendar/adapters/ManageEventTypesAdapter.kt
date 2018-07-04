package com.simplemobiletools.calendar.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.helpers.DBHelper
import com.simplemobiletools.calendar.interfaces.DeleteEventTypesListener
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.item_event_type.view.*
import java.util.*

class ManageEventTypesAdapter(activity: SimpleActivity, val eventTypes: ArrayList<EventType>, val listener: DeleteEventTypesListener?, recyclerView: MyRecyclerView,
                              itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_event_type

    override fun prepareActionMode(menu: Menu) {}

    override fun prepareItemSelection(viewHolder: ViewHolder) {}

    override fun markViewHolderSelection(select: Boolean, viewHolder: ViewHolder?) {
        viewHolder?.itemView?.event_item_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = eventTypes.size

    override fun getIsItemSelectable(position: Int) = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.item_event_type, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = eventTypes[position]
        val view = holder.bindView(eventType, true, true) { itemView, layoutPosition ->
            setupView(itemView, eventType)
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = eventTypes.size

    private fun setupView(view: View, eventType: EventType) {
        view.apply {
            event_type_title.text = eventType.getDisplayTitle()
            event_type_color.setFillWithStroke(eventType.color, activity.config.backgroundColor)
            event_type_title.setTextColor(textColor)
        }
    }

    private fun askConfirmDelete() {
        val eventTypes = ArrayList<EventType>(selectedPositions.size)
        selectedPositions.forEach { eventTypes.add(this.eventTypes[it]) }

        if (activity.dbHelper.doEventTypesContainEvent(eventTypes)) {
            val MOVE_EVENTS = 0
            val DELETE_EVENTS = 1
            val res = activity.resources
            val items = ArrayList<RadioItem>().apply {
                add(RadioItem(MOVE_EVENTS, res.getString(R.string.move_events_into_default)))
                add(RadioItem(DELETE_EVENTS, res.getString(R.string.remove_affected_events)))
            }
            RadioGroupDialog(activity, items) {
                deleteEventTypes(it == DELETE_EVENTS)
            }
        } else {
            ConfirmationDialog(activity) {
                deleteEventTypes(true)
            }
        }
    }

    private fun deleteEventTypes(deleteEvents: Boolean) {
        val eventTypesToDelete = ArrayList<EventType>(selectedPositions.size)

        for (pos in selectedPositions) {
            if (eventTypes[pos].id == DBHelper.REGULAR_EVENT_TYPE_ID) {
                activity.toast(R.string.cannot_delete_default_type)
                selectedPositions.remove(pos)
                toggleItemSelection(false, pos)
                break
            }
        }

        selectedPositions.sortedDescending().forEach {
            val eventType = eventTypes[it]
            eventTypesToDelete.add(eventType)
        }

        eventTypes.removeAll(eventTypesToDelete)
        if (listener?.deleteEventTypes(eventTypesToDelete, deleteEvents) == true) {
            removeSelectedItems()
        }
    }
}
