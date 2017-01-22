package com.simplemobiletools.calendar.adapters

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config

class WeekEventsAdapter(val context: Context, val startTS: Int, val callback: (timestamp: Int) -> Unit) : BaseAdapter() {
    private val mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val defaultBackground = context.config.backgroundColor
    private val coloredBackground = context.config.primaryColor
    private var selectedGrid: ImageView? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView

        if (view == null) {
            view = mInflater.inflate(R.layout.week_grid_item, parent, false) as ImageView
            view.background = ColorDrawable(defaultBackground)
        }

        view.setOnClickListener {
            selectedGrid?.background = ColorDrawable(defaultBackground)
            if (selectedGrid == view) {
                selectedGrid = null
                callback.invoke(startTS + (position / 7 * 60 * 60) + (position % 7 * 24 * 60 * 60))
            } else {
                view!!.background = ColorDrawable(coloredBackground)
                (view as ImageView).setImageResource(R.drawable.ic_plus)
                selectedGrid = view as ImageView
            }
        }

        return view
    }

    override fun getItem(position: Int) = null

    override fun getItemId(position: Int) = 0L

    override fun getCount() = 24 * 7
}
