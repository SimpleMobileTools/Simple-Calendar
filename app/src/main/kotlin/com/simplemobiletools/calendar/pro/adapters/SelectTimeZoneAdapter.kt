package com.simplemobiletools.calendar.pro.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.databinding.ItemSelectTimeZoneBinding
import com.simplemobiletools.calendar.pro.models.MyTimeZone
import com.simplemobiletools.commons.extensions.getProperTextColor

class SelectTimeZoneAdapter(val activity: SimpleActivity, var timeZones: ArrayList<MyTimeZone>, val itemClick: (Any) -> Unit) :
    RecyclerView.Adapter<SelectTimeZoneAdapter.TimeZoneViewHolder>() {
    val textColor = activity.getProperTextColor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeZoneViewHolder {
        return TimeZoneViewHolder(
            binding = ItemSelectTimeZoneBinding.inflate(activity.layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TimeZoneViewHolder, position: Int) {
        val timeZone = timeZones[position]
        holder.bindView(timeZone)
    }

    override fun getItemCount() = timeZones.size

    fun updateTimeZones(newTimeZones: ArrayList<MyTimeZone>) {
        timeZones = newTimeZones.clone() as ArrayList<MyTimeZone>
        notifyDataSetChanged()
    }

    inner class TimeZoneViewHolder(val binding: ItemSelectTimeZoneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindView(timeZone: MyTimeZone) {
            binding.apply {
                itemTimeZoneTitle.text = timeZone.zoneName
                itemTimeZoneShift.text = timeZone.title

                itemTimeZoneTitle.setTextColor(textColor)
                itemTimeZoneShift.setTextColor(textColor)

                itemSelectTimeZoneHolder.setOnClickListener {
                    itemClick(timeZone)
                }
            }
        }
    }
}
