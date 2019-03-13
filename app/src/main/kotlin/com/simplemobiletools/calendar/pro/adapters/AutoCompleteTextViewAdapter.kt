package com.simplemobiletools.calendar.pro.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.models.Attendee
import com.simplemobiletools.commons.extensions.normalizeString
import kotlinx.android.synthetic.main.item_autocomplete.view.*

class AutoCompleteTextViewAdapter(val activity: SimpleActivity, val contacts: ArrayList<Attendee>) : ArrayAdapter<Attendee>(activity, 0, contacts) {
    private var resultList = ArrayList<Attendee>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItem = convertView
        if (listItem == null) {
            listItem = LayoutInflater.from(activity).inflate(R.layout.item_autocomplete, parent, false)
        }

        val contact = resultList[position]
        listItem!!.item_autocomplete.text = contact.email

        return listItem
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = Filter.FilterResults()
            if (constraint != null) {
                resultList.clear()
                val searchString = constraint.toString().normalizeString()
                contacts.forEach {
                    if (it.email.contains(searchString, true) || it.name.contains(searchString, true)) {
                        resultList.add(it)
                    }
                }
                filterResults.values = resultList
                filterResults.count = resultList.size
            }
            return filterResults
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            if (results?.count ?: -1 > 0) {
                notifyDataSetChanged()
            } else {
                notifyDataSetInvalidated()
            }
        }

        override fun convertResultToString(resultValue: Any?) = (resultValue as? Attendee)?.email
    }

    override fun getItem(index: Int) = resultList[index]

    override fun getCount() = resultList.size
}
