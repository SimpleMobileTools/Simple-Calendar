package com.simplemobiletools.calendar.pro.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.setFillWithStroke
import kotlinx.android.synthetic.main.filter_event_type_view.view.*
import java.util.*

class FilterEventTypeAdapter(val activity: SimpleActivity, val eventTypes: List<EventType>, val displayEventTypes: Set<String>) :
        RecyclerView.Adapter<FilterEventTypeAdapter.ViewHolder>() {
    private val selectedKeys = HashSet<Long>()

    init {
        eventTypes.forEachIndexed { index, eventType ->
            if (displayEventTypes.contains(eventType.id.toString())) {
                selectedKeys.add(eventType.id!!)
            }
        }
    }

    private fun toggleItemSelection(select: Boolean, eventType: EventType, pos: Int) {
        if (select) {
            selectedKeys.add(eventType.id!!)
        } else {
            selectedKeys.remove(eventType.id)
        }

        notifyItemChanged(pos)
    }

    fun getSelectedItemsList() = selectedKeys.asSequence().map { it }.toMutableList() as ArrayList<Long>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = activity.layoutInflater.inflate(R.layout.filter_event_type_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = eventTypes[position]
        holder.bindView(eventType)
    }

    override fun getItemCount() = eventTypes.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(eventType: EventType): View {
            val isSelected = selectedKeys.contains(eventType.id)
            itemView.apply {
                filter_event_type_checkbox.isChecked = isSelected
                filter_event_type_checkbox.setColors(activity.config.textColor, activity.getAdjustedPrimaryColor(), activity.config.backgroundColor)
                filter_event_type_checkbox.text = eventType.getDisplayTitle()
                filter_event_type_color.setFillWithStroke(eventType.color, activity.config.backgroundColor)
                filter_event_type_holder.setOnClickListener { viewClicked(!isSelected, eventType) }
            }

            return itemView
        }

        private fun viewClicked(select: Boolean, eventType: EventType) {
            toggleItemSelection(select, eventType, adapterPosition)
        }
    }
}
