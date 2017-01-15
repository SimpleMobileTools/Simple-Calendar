package com.simplemobiletools.calendar.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.models.Event

class WeekEventsAdapter(val context: Context, val mEvents: List<Event>) : BaseAdapter() {
    private val mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val viewHolder: ViewHolder

        if (view == null) {
            view = mInflater.inflate(R.layout.week_grid_item, parent, false)
            viewHolder = ViewHolder(view)
            view!!.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        return view
    }

    override fun getItem(position: Int) = mEvents[position]

    override fun getItemId(position: Int) = 0L

    override fun getCount() = 24 * 7

    internal class ViewHolder(view: View)
}
