package com.simplemobiletools.calendar.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import com.simplemobiletools.commons.extensions.beInvisibleIf
import kotlinx.android.synthetic.main.event_list_item_widget.view.*

class EventListWidgetAdapterOld(val context: Context, val mEvents: List<ListItem>) : BaseAdapter() {
    val ITEM_EVENT = 0
    val ITEM_HEADER = 1

    private val mInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var mTopDivider: Drawable? = null
    private var mTextColor = 0

    init {
        mTopDivider = context.resources.getDrawable(R.drawable.divider_width)
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
                view.setOnClickListener(null)
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
                start?.text = Formatter.getTimeFromTS(context, item.startTS)
                end?.beInvisibleIf(item.startTS == item.endTS)

                if (item.startTS != item.endTS) {
                    end?.text = Formatter.getTimeFromTS(context, item.endTS)

                    val startCode = Formatter.getDayCodeFromTS(item.startTS)
                    val endCode = Formatter.getDayCodeFromTS(item.endTS)
                    if (startCode != endCode) {
                        end?.append(" (${Formatter.getDateFromCode(context, endCode)})")
                    }
                }

                start?.setTextColor(mTextColor)
                end?.setTextColor(mTextColor)
                title.setTextColor(mTextColor)
                description?.setTextColor(mTextColor)
            }
        } else {
            val item = mEvents[position] as ListSection
            viewHolder.title.apply {
                text = item.title
                setCompoundDrawablesWithIntrinsicBounds(null, if (position == 0) null else mTopDivider, null, null)
                setTextColor(mTextColor)
            }
        }

        return view
    }

    fun setTextColor(color: Int) {
        mTextColor = color
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = if (mEvents[position] is ListEvent) ITEM_EVENT else ITEM_HEADER

    override fun getViewTypeCount() = 2

    override fun getCount() = mEvents.size

    override fun getItem(position: Int) = mEvents[position]

    override fun getItemId(position: Int) = 0L

    internal class ViewHolder(view: View) {
        val title = view.event_item_title
        val description: TextView? = view.event_item_description
        val start: TextView? = view.event_item_start
        val end: TextView? = view.event_item_end
    }
}
