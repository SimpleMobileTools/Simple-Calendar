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
import com.simplemobiletools.calendar.dialogs.DeleteEventDialog
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.shareEvents
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.interfaces.DeleteEventsListener
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import kotlinx.android.synthetic.main.event_list_item.view.*
import java.util.*

class EventListAdapter(val activity: SimpleActivity, val mItems: List<ListItem>, val listener: DeleteEventsListener?, val itemClick: (Int, Int) -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()

    val ITEM_EVENT = 0
    val ITEM_HEADER = 1

    companion object {
        var actMode: ActionMode? = null
        val markedItems = HashSet<Int>()

        var topDivider: Drawable? = null
        var mNow = (System.currentTimeMillis() / 1000).toInt()
        var primaryColor = 0
        var textColor = 0
        var redTextColor = 0
        var todayDate = ""
        var allDayString = ""

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
        val res = activity.resources
        allDayString = res.getString(R.string.all_day)
        topDivider = res.getDrawable(R.drawable.divider_width)
        textColor = activity.config.textColor
        redTextColor = res.getColor(R.color.red_text)
        primaryColor = activity.config.primaryColor
        val mTodayCode = Formatter.getDayCodeFromTS(mNow)
        todayDate = Formatter.getDayTitle(activity, mTodayCode)
    }

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.cab_share -> shareEvents()
                R.id.cab_delete -> askConfirmDelete()
                else -> return false
            }
            return true
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

    private fun shareEvents() {
        val selections = multiSelector.selectedPositions
        val eventIds = ArrayList<Int>(selections.size)
        selections.forEach {
            eventIds.add((mItems[it] as ListEvent).id)
        }
        activity.shareEvents(eventIds.distinct())
        actMode?.finish()
    }

    private fun askConfirmDelete() {
        val selections = multiSelector.selectedPositions
        val eventIds = ArrayList<Int>(selections.size)
        val timestamps = ArrayList<Int>(selections.size)

        selections.forEach {
            eventIds.add((mItems[it] as ListEvent).id)
            timestamps.add((mItems[it] as ListEvent).startTS)
        }

        DeleteEventDialog(activity, eventIds) {
            if (it) {
                listener?.deleteItems(eventIds)
            } else {
                listener?.addEventRepeatException(eventIds, timestamps)
            }
            actMode?.finish()
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

    class ViewHolder(val activity: SimpleActivity, view: View, val itemClick: (Int, Int) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, listItem: ListItem, pos: Int): View {
            val item = listItem as ListEvent
            itemView.apply {
                event_item_title.text = item.title
                event_item_description.text = item.description
                event_item_start.text = if (item.isAllDay) allDayString else Formatter.getTimeFromTS(context, item.startTS)
                event_item_end.beInvisibleIf(item.startTS == item.endTS)
                toggleItemSelection(this, markedItems.contains(pos), pos)

                if (item.startTS != item.endTS) {
                    val startCode = Formatter.getDayCodeFromTS(item.startTS)
                    val endCode = Formatter.getDayCodeFromTS(item.endTS)

                    event_item_end.apply {
                        text = Formatter.getTimeFromTS(context, item.endTS)
                        if (startCode != endCode) {
                            if (item.isAllDay) {
                                text = Formatter.getDateFromCode(context, endCode, true)
                            } else {
                                append(" (${Formatter.getDateFromCode(context, endCode, true)})")
                            }
                        } else if (item.isAllDay) {
                            beInvisible()
                        }
                    }
                }

                var startTextColor = textColor
                var endTextColor = textColor
                if (item.startTS <= mNow && item.endTS <= mNow) {
                    if (item.isAllDay) {
                        if (Formatter.getDayCodeFromTS(item.startTS) == Formatter.getDayCodeFromTS(mNow))
                            startTextColor = primaryColor
                    } else {
                        startTextColor = redTextColor
                    }
                    endTextColor = redTextColor
                } else if (item.startTS <= mNow && item.endTS >= mNow) {
                    startTextColor = primaryColor
                }

                event_item_start.setTextColor(startTextColor)
                event_item_end.setTextColor(endTextColor)
                event_item_title.setTextColor(startTextColor)
                event_item_description.setTextColor(startTextColor)

                setOnClickListener { viewClicked(multiSelector, listItem, pos) }
                setOnLongClickListener {
                    if (!multiSelector.isSelectable) {
                        activity.startSupportActionMode(multiSelectorCallback)
                        multiSelector.setSelected(this@ViewHolder, true)
                        actMode?.title = multiSelector.selectedPositions.size.toString()
                        toggleItemSelection(itemView, true, pos)
                        actMode?.invalidate()
                    }
                    true
                }
            }

            return itemView
        }

        fun viewClicked(multiSelector: MultiSelector, listItem: ListItem, pos: Int) {
            if (multiSelector.isSelectable) {
                val isSelected = multiSelector.selectedPositions.contains(layoutPosition)
                multiSelector.setSelected(this, !isSelected)
                toggleItemSelection(itemView, !isSelected, pos)

                val selectedCnt = multiSelector.selectedPositions.size
                if (selectedCnt == 0) {
                    actMode?.finish()
                } else {
                    actMode?.title = selectedCnt.toString()
                }
                actMode?.invalidate()
            } else {
                val listEvent = listItem as ListEvent
                itemClick(listEvent.id, listEvent.startTS)
            }
        }
    }

    class SectionHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bindView(listItem: ListItem): View {
            val item = listItem as ListSection
            itemView.event_item_title.apply {
                text = item.title
                setCompoundDrawablesWithIntrinsicBounds(null, if (position == 0) null else topDivider, null, null)
                setTextColor(if (item.title == todayDate) primaryColor else textColor)
            }

            return itemView
        }
    }
}
