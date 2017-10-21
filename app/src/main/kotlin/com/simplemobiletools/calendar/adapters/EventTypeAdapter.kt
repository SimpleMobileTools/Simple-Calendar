package com.simplemobiletools.calendar.adapters

import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.interfaces.DeleteEventTypesListener
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.setBackgroundWithStroke
import com.simplemobiletools.commons.models.RadioItem
import kotlinx.android.synthetic.main.item_event_type.view.*
import java.util.*

class EventTypeAdapter(val activity: SimpleActivity, val mItems: List<EventType>, val listener: DeleteEventTypesListener?, val itemClick: (EventType) -> Unit) :
        RecyclerView.Adapter<EventTypeAdapter.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()

    companion object {
        var actMode: ActionMode? = null
        val markedItems = HashSet<Int>()
        var textColor = 0

        fun toggleItemSelection(itemView: View, select: Boolean, pos: Int = -1) {
            itemView.event_item_frame.isSelected = select
            if (pos == -1) {
                return
            }

            if (select) {
                markedItems.add(pos)
            } else {
                markedItems.remove(pos)
            }
        }
    }

    init {
        textColor = activity.config.textColor
    }

    private val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_delete -> askConfirmDelete()
                else -> return false
            }
            return true
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            actMode = actionMode
            activity.menuInflater.inflate(R.menu.cab_event_type, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu) = true

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { toggleItemSelection(it, false) }
            markedItems.clear()
        }
    }

    private fun askConfirmDelete() {
        val selections = multiSelector.selectedPositions
        val eventTypes = ArrayList<EventType>(selections.size)
        selections.forEach { eventTypes.add(mItems[it]) }

        if (activity.dbHelper.doEventTypesContainEvent(eventTypes)) {
            val MOVE_EVENTS = 0
            val DELETE_EVENTS = 1
            val res = activity.resources
            val items = ArrayList<RadioItem>().apply {
                add(RadioItem(MOVE_EVENTS, res.getString(R.string.move_events_into_default)))
                add(RadioItem(DELETE_EVENTS, res.getString(R.string.remove_affected_events)))
            }
            RadioGroupDialog(activity, items, -1) {
                actMode?.finish()
                deleteEventTypes(it == DELETE_EVENTS, eventTypes)
            }
        } else {
            deleteEventTypes(true, eventTypes)
        }
    }

    private fun deleteEventTypes(deleteEvents: Boolean, eventTypes: ArrayList<EventType>) {
        listener?.deleteEventTypes(eventTypes, deleteEvents)
        actMode?.finish()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.item_event_type, parent, false)
        return ViewHolder(activity, view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        views.add(holder.bindView(multiSelectorMode, multiSelector, mItems[position], position))
    }

    override fun getItemCount() = mItems.size

    class ViewHolder(val activity: SimpleActivity, view: View, val itemClick: (EventType) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, eventType: EventType, pos: Int): View {

            itemView.apply {
                event_type_title.text = eventType.getDisplayTitle()
                event_type_color.setBackgroundWithStroke(eventType.color, activity.config.backgroundColor)
                toggleItemSelection(this, markedItems.contains(pos), pos)

                event_type_title.setTextColor(textColor)

                setOnClickListener { viewClicked(multiSelector, eventType, pos) }
                setOnLongClickListener {
                    if (!multiSelector.isSelectable) {
                        activity.startSupportActionMode(multiSelectorCallback)
                        multiSelector.setSelected(this@ViewHolder, true)
                        actMode?.title = multiSelector.selectedPositions.size.toString()
                        toggleItemSelection(itemView, true, pos)
                        actMode?.invalidate()
                    }
                    true
                }
            }

            return itemView
        }

        private fun viewClicked(multiSelector: MultiSelector, eventType: EventType, pos: Int) {
            if (multiSelector.isSelectable) {
                val isSelected = multiSelector.selectedPositions.contains(layoutPosition)
                multiSelector.setSelected(this, !isSelected)
                toggleItemSelection(itemView, !isSelected, pos)

                val selectedCnt = multiSelector.selectedPositions.size
                if (selectedCnt == 0) {
                    actMode?.finish()
                } else {
                    actMode?.title = selectedCnt.toString()
                }
                actMode?.invalidate()
            } else {
                itemClick(eventType)
            }
        }
    }
}
