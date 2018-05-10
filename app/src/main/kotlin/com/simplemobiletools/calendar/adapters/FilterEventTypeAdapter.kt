package com.simplemobiletools.calendar.adapters

import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.setFillWithStroke
import com.simplemobiletools.commons.interfaces.MyAdapterListener
import kotlinx.android.synthetic.main.filter_event_type_view.view.*
import java.util.*

class FilterEventTypeAdapter(val activity: SimpleActivity, val eventTypes: List<EventType>, val displayEventTypes: Set<String>) :
        RecyclerView.Adapter<FilterEventTypeAdapter.ViewHolder>() {
    private val itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()

    init {
        eventTypes.forEachIndexed { index, eventType ->
            if (displayEventTypes.contains(eventType.id.toString())) {
                selectedPositions.add(index)
            }
        }
    }

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        itemViews[pos]?.filter_event_type_checkbox?.isChecked = select
    }

    private val adapterListener = object : MyAdapterListener {
        override fun toggleItemSelectionAdapter(select: Boolean, position: Int) {
            toggleItemSelection(select, position)
        }

        override fun getSelectedPositions() = selectedPositions

        override fun itemLongClicked(position: Int) {}
    }

    fun getSelectedItemsSet(): HashSet<String> {
        val selectedItemsSet = HashSet<String>(selectedPositions.size)
        selectedPositions.forEach { selectedItemsSet.add(eventTypes[it].id.toString()) }
        return selectedItemsSet
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(R.layout.filter_event_type_view, parent, false)
        return ViewHolder(view, adapterListener, activity)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = eventTypes[position]
        itemViews.put(position, holder.bindView(eventType))
        toggleItemSelection(selectedPositions.contains(position), position)
    }

    override fun getItemCount() = eventTypes.size

    class ViewHolder(view: View, val adapterListener: MyAdapterListener, val activity: SimpleActivity) : RecyclerView.ViewHolder(view) {
        fun bindView(eventType: EventType): View {
            itemView.apply {
                filter_event_type_checkbox.setColors(activity.config.textColor, activity.getAdjustedPrimaryColor(), activity.config.backgroundColor)
                filter_event_type_checkbox.text = eventType.getDisplayTitle()
                filter_event_type_color.setFillWithStroke(eventType.color, activity.config.backgroundColor)
                filter_event_type_holder.setOnClickListener { viewClicked(!filter_event_type_checkbox.isChecked) }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean) {
            adapterListener.toggleItemSelectionAdapter(select, adapterPosition)
        }
    }
}
