package com.simplemobiletools.calendar.adapters

import android.graphics.drawable.Drawable
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import kotlinx.android.synthetic.main.event_item.view.*
import java.util.*

class EventListAdapter(val activity: SimpleActivity, val mItems: List<ListItem>, val listener: EventListAdapter.ItemOperationsListener?, val itemClick: (ListItem) -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()

    val ITEM_EVENT = 0
    val ITEM_HEADER = 1

    companion object {
        var actMode: ActionMode? = null
        val markedItems = HashSet<Int>()

        var mTopDivider: Drawable? = null
        var mNow = (System.currentTimeMillis() / 1000).toInt()
        var mOrangeColor = 0
        var mGreyColor = 0
        var mTodayDate = ""

        fun toggleItemSelection(itemView: View, select: Boolean, pos: Int = -1) {
            itemView.event_item_frame.isSelected = select
            if (pos == -1)
                return

            if (select)
                markedItems.add(pos)
            else
                markedItems.remove(pos)
        }
    }

    init {
        mTopDivider = activity.resources.getDrawable(R.drawable.divider)
        mOrangeColor = activity.resources.getColor(R.color.colorPrimary)
        val mTodayCode = Formatter.getDayCodeFromTS(mNow)
        mTodayDate = Formatter.getEventDate(activity, mTodayCode)
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                else -> false
            }
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            actMode = actionMode
            activity.menuInflater.inflate(R.menu.cab_event_list, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu) = true

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            views.forEach { toggleItemSelection(it, false) }
            markedItems.clear()
        }
    }

    override fun getItemViewType(position: Int) = if (mItems[position] is ListEvent) ITEM_EVENT else ITEM_HEADER

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = if (viewType == ITEM_EVENT) R.layout.event_list_item else R.layout.event_list_section
        val view = LayoutInflater.from(parent?.context).inflate(layoutId, parent, false)
        return if (viewType == ITEM_EVENT)
            EventListAdapter.ViewHolder(activity, view, itemClick)
        else
            EventListAdapter.SectionHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == ITEM_EVENT)
            views.add((holder as ViewHolder).bindView(multiSelectorMode, multiSelector, mItems[position], position))
        else
            (holder as SectionHolder).bindView(mItems[position])
    }

    override fun getItemCount() = mItems.size

    class ViewHolder(val activity: SimpleActivity, view: View, val itemClick: (ListItem) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, listItem: ListItem, pos: Int): View {
            val item = listItem as ListEvent
            itemView.apply {
                event_item_title.text = item.title
                event_item_description.text = item.description
                event_item_start.text = Formatter.getTime(item.startTS)

                if (item.startTS == item.endTS) {
                    event_item_end.visibility = View.INVISIBLE
                } else {
                    event_item_end.text = Formatter.getTime(item.endTS)
                    event_item_end.visibility = View.VISIBLE

                    val startCode = Formatter.getDayCodeFromTS(item.startTS)
                    val endCode = Formatter.getDayCodeFromTS(item.endTS)
                    if (startCode != endCode) {
                        event_item_end.append(" (${Formatter.getEventDate(context, endCode)})")
                    }
                }

                val currTextColor = if (item.startTS <= mNow) mOrangeColor else mGreyColor
                event_item_start.setTextColor(currTextColor)
                event_item_end.setTextColor(currTextColor)
                event_item_title.setTextColor(currTextColor)
                event_item_description.setTextColor(currTextColor)
            }

            return itemView
        }
    }

    class SectionHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(listItem: ListItem): View {
            val item = listItem as ListSection
            itemView.apply {
                event_item_title.text = item.title
                event_item_title.setCompoundDrawablesWithIntrinsicBounds(null, if (position == 0) null else mTopDivider, null, null)

                if (mGreyColor == 0)
                    mGreyColor = event_item_title.currentTextColor

                event_item_title.setTextColor(if (item.title == mTodayDate) mOrangeColor else mGreyColor)
            }

            return itemView
        }
    }

    interface ItemOperationsListener {
        fun prepareForDeleting(ids: ArrayList<Int>)
    }
}
