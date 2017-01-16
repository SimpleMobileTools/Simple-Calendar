package com.simplemobiletools.calendar.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.simplemobiletools.calendar.R

class WeekEventsAdapter(val context: Context) : BaseAdapter() {
    private val mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView

        if (view == null) {
            view = mInflater.inflate(R.layout.week_grid_item, parent, false)
        }

        return view!!
    }

    override fun getItem(position: Int) = null

    override fun getItemId(position: Int) = 0L

    override fun getCount() = 24 * 7
}
