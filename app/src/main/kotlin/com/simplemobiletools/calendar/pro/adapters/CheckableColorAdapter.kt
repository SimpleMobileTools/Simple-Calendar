package com.simplemobiletools.calendar.pro.adapters

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.commons.extensions.applyColorFilter
import kotlinx.android.synthetic.main.checkable_color_button.view.*

class CheckableColorAdapter(private val activity: Activity, private val colors: IntArray, var currentColor: Int, val callback: (color: Int) -> Unit) :
    RecyclerView.Adapter<CheckableColorAdapter.CheckableColorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckableColorViewHolder {
        val itemView = LayoutInflater.from(activity)
            .inflate(R.layout.checkable_color_button, parent, false)
        return CheckableColorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CheckableColorViewHolder, position: Int) {
        val color = colors[position]
        holder.bindView(color = color, checked = color == currentColor)
    }

    override fun getItemCount() = colors.size

    private fun updateSelection(color: Int) {
        currentColor = color
        callback(color)
    }

    inner class CheckableColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(color: Int, checked: Boolean) {
            itemView.checkable_color_button.apply {
                backgroundTintList = ColorStateList.valueOf(color)
                setOnClickListener {
                    updateSelection(color)
                }

                if (checked) {
                    setImageResource(R.drawable.ic_check_vector)
                    applyColorFilter(Color.WHITE)
                } else {
                    setImageDrawable(null)
                }
            }
        }
    }
}
