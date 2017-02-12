package com.simplemobiletools.calendar.adapters

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.models.EventType
import com.simplemobiletools.commons.extensions.setBackgroundWithStroke
import kotlinx.android.synthetic.main.filter_event_type_view.view.*
import java.util.*

class FilterEventTypeAdapter(val activity: SimpleActivity, val mItems: List<EventType>, mDisplayEventTypes: Set<String>) :
        RecyclerView.Adapter<FilterEventTypeAdapter.ViewHolder>() {
    val views = ArrayList<View>()

    companion object {
        var textColor = 0
        lateinit var displayEventTypes: Set<String>
        lateinit var selectedItems: ArrayList<String>
    }

    init {
        textColor = activity.config.textColor
        displayEventTypes = mDisplayEventTypes
        selectedItems = ArrayList()
        displayEventTypes.mapTo(selectedItems, { it })
    }

    fun getSelectedItemsSet(): HashSet<String> {
        val selectedItemsSet = HashSet<String>(selectedItems.size)
        selectedItems.mapTo(selectedItemsSet, { it })
        return selectedItemsSet
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.filter_event_type_view, parent, false)
        return ViewHolder(activity, view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        views.add(holder.bindView(mItems[position]))
    }

    override fun getItemCount() = mItems.size

    class ViewHolder(val activity: SimpleActivity, view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(eventType: EventType): View {
            val id = eventType.id.toString()
            itemView.apply {
                filter_event_type_checkbox.setColors(activity.config.textColor, activity.config.primaryColor, activity.config.backgroundColor)
                filter_event_type_checkbox.text = eventType.title
                filter_event_type_color.setBackgroundWithStroke(eventType.color, activity.config.backgroundColor)

                filter_event_type_holder.setOnClickListener {
                    filter_event_type_checkbox.toggle()
                    if (filter_event_type_checkbox.isChecked) {
                        selectedItems.add(id)
                    } else {
                        selectedItems.remove(id)
                    }
                }

                filter_event_type_checkbox.isChecked = displayEventTypes.contains(id)
            }

            return itemView
        }
    }
}
