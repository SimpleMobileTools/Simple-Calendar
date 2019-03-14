package com.simplemobiletools.calendar.pro.adapters

import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.models.Attendee
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.normalizeString
import kotlinx.android.synthetic.main.item_autocomplete_email_name.view.*

class AutoCompleteTextViewAdapter(val activity: SimpleActivity, val contacts: ArrayList<Attendee>) : ArrayAdapter<Attendee>(activity, 0, contacts) {
    private var resultList = ArrayList<Attendee>()
    private var placeholder = activity.resources.getDrawable(R.drawable.attendee_circular_background)

    init {
        (placeholder as LayerDrawable).findDrawableByLayerId(R.id.attendee_circular_background).applyColorFilter(activity.config.primaryColor)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val contact = resultList[position]
        var listItem = convertView
        if (listItem == null || listItem.tag != contact.name.isNotEmpty()) {
            val layout = if (contact.name.isNotEmpty()) R.layout.item_autocomplete_email_name else R.layout.item_autocomplete_email
            listItem = LayoutInflater.from(activity).inflate(layout, parent, false)
        }

        listItem!!.apply {
            tag = contact.name.isNotEmpty()
            item_autocomplete_name?.text = contact.name
            item_autocomplete_email?.text = contact.email

            if (contact.photoUri.isEmpty()) {
                item_autocomplete_image.setImageDrawable(placeholder)
            } else {
                val options = RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .error(placeholder)
                        .centerCrop()

                Glide.with(activity)
                        .load(contact.photoUri)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .apply(options)
                        .apply(RequestOptions.circleCropTransform())
                        .into(item_autocomplete_image)
            }
        }

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

                resultList.sortWith(compareBy<Attendee>
                { it.name.startsWith(searchString, true) }.thenBy
                { it.email.startsWith(searchString, true) }.thenBy
                { it.name.contains(searchString, true) }.thenBy
                { it.email.contains(searchString, true) })
                resultList.reverse()

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

        override fun convertResultToString(resultValue: Any?) = (resultValue as? Attendee)?.getPublicName()
    }

    override fun getItem(index: Int) = resultList[index]

    override fun getCount() = resultList.size
}
