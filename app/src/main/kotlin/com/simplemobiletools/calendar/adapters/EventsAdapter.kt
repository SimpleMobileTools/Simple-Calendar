package com.simplemobiletools.calendar.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.models.Event
import kotlinx.android.synthetic.main.event_item.view.*

class EventsAdapter(context: Context, private val mEvents: List<Event>) : BaseAdapter() {
    private val mInflater: LayoutInflater

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val viewHolder: ViewHolder
        if (view == null) {
            view = mInflater.inflate(R.layout.event_item, parent, false)
            viewHolder = ViewHolder(view)
            view!!.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        val event = mEvents[position]
        viewHolder.apply {
            title.text = event.title
            description.text = event.description
            start.text = Formatter.getTime(event.startTS)
        }

        if (event.startTS == event.endTS) {
            viewHolder.end.visibility = View.INVISIBLE
        } else {
            viewHolder.end.text = Formatter.getTime(event.endTS)
        }

        return view
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

    class ViewHolder(view: View) {
        val title = view.event_item_title
        val description = view.event_item_description
        val start = view.event_item_start
        val end = view.event_item_end
    }
}
