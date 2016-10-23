package com.simplemobiletools.calendar.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import kotlinx.android.synthetic.main.event_item.view.*

class EventsListAdapter(context: Context, private val mEvents: List<ListItem>) : BaseAdapter() {
    val ITEM_EVENT = 0
    val ITEM_HEADER = 1

    private val mInflater: LayoutInflater

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder
        val type = getItemViewType(position)

        if (view == null) {
            if (type == ITEM_EVENT) {
                view = mInflater.inflate(R.layout.event_list_item, parent, false)
            } else {
                view = mInflater.inflate(R.layout.event_list_section, parent, false)
            }
            viewHolder = ViewHolder(view)
            view!!.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        if (type == ITEM_EVENT) {
            val item = mEvents[position] as ListEvent
            viewHolder.apply {
                title.text = item.title
                description?.text = item.description
                start?.text = Formatter.getTime(item.startTS)

                if (item.startTS == item.endTS) {
                    end?.visibility = View.INVISIBLE
                } else {
                    end?.text = Formatter.getTime(item.endTS)
                    end?.visibility = View.VISIBLE
                }
            }
        } else {
            val item = mEvents[position] as ListSection
            viewHolder.title.text = item.title
        }

        return view
    }

    override fun getItemViewType(position: Int): Int {
        return if (mEvents[position] is ListEvent) ITEM_EVENT else ITEM_HEADER
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getCount(): Int {
        return mEvents.size
    }

    override fun getItem(position: Int): Any {
        return mEvents[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    internal class ViewHolder(view: View) {
        val title = view.event_item_title
        val description: TextView? = view.event_item_description
        val start: TextView? = view.event_item_start
        val end: TextView? = view.event_item_end
    }
}
