package com.simplemobiletools.calendar.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.dialogs.DeleteEventDialog
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.shareEvents
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.helpers.getNowSeconds
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_list_item.view.*
import java.util.*

class EventListAdapter(activity: SimpleActivity, var listItems: ArrayList<ListItem>, val allowLongClick: Boolean, val listener: RefreshRecyclerViewListener?,
                       recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private val ITEM_EVENT = 0
    private val ITEM_HEADER = 1

    private val topDivider = resources.getDrawable(R.drawable.divider_width)
    private val allDayString = resources.getString(R.string.all_day)
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val now = getNowSeconds()
    private var use24HourFormat = activity.config.use24HourFormat
    private var currentItemsHash = listItems.hashCode()

    init {
        var firstNonPastSectionIndex = -1
        listItems.forEachIndexed { index, listItem ->
            if (firstNonPastSectionIndex == -1 && listItem is ListSection) {
                if (!listItem.isPastSection) {
                    firstNonPastSectionIndex = index
                }
            }
        }

        if (firstNonPastSectionIndex != -1) {
            activity.runOnUiThread {
                recyclerView.scrollToPosition(firstNonPastSectionIndex)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_event_list

    override fun prepareActionMode(menu: Menu) {}

    override fun prepareItemSelection(view: View) {}

    override fun markItemSelection(select: Boolean, view: View?) {
        view?.event_item_frame?.isSelected = select
    }

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = listItems.filter { it is ListEvent }.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerViewAdapter.ViewHolder {
        val layoutId = if (viewType == ITEM_EVENT) R.layout.event_list_item else R.layout.event_list_section
        return createViewHolder(layoutId, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val listItem = listItems[position]
        val view = holder.bindView(listItem, allowLongClick) { itemView, layoutPosition ->
            if (listItem is ListSection) {
                setupListSection(itemView, listItem, position)
            } else if (listItem is ListEvent) {
                setupListEvent(itemView, listItem)
            }
        }
        bindViewHolder(holder, position, view)
    }

    override fun getItemCount() = listItems.size

    override fun getItemViewType(position: Int) = if (listItems[position] is ListEvent) ITEM_EVENT else ITEM_HEADER

    fun toggle24HourFormat(use24HourFormat: Boolean) {
        this.use24HourFormat = use24HourFormat
        notifyDataSetChanged()
    }

    fun updateListItems(newListItems: ArrayList<ListItem>) {
        if (newListItems.hashCode() != currentItemsHash) {
            currentItemsHash = newListItems.hashCode()
            listItems = newListItems.clone() as ArrayList<ListItem>
            recyclerView.resetItemCount()
            notifyDataSetChanged()
            finishActMode()
        }
    }

    private fun setupListEvent(view: View, listEvent: ListEvent) {
        view.apply {
            event_section_title.text = listEvent.title
            event_item_description.text = if (replaceDescriptionWithLocation) listEvent.location else listEvent.description
            event_item_start.text = if (listEvent.isAllDay) allDayString else Formatter.getTimeFromTS(context, listEvent.startTS)
            event_item_end.beInvisibleIf(listEvent.startTS == listEvent.endTS)
            event_item_color.applyColorFilter(listEvent.color)

            if (listEvent.startTS != listEvent.endTS) {
                val startCode = Formatter.getDayCodeFromTS(listEvent.startTS)
                val endCode = Formatter.getDayCodeFromTS(listEvent.endTS)

                event_item_end.apply {
                    text = Formatter.getTimeFromTS(context, listEvent.endTS)
                    if (startCode != endCode) {
                        if (listEvent.isAllDay) {
                            text = Formatter.getDateFromCode(context, endCode, true)
                        } else {
                            append(" (${Formatter.getDateFromCode(context, endCode, true)})")
                        }
                    } else if (listEvent.isAllDay) {
                        beInvisible()
                    }
                }
            }

            var startTextColor = textColor
            var endTextColor = textColor
            if (listEvent.startTS <= now && listEvent.endTS <= now) {
                if (listEvent.isAllDay) {
                    if (Formatter.getDayCodeFromTS(listEvent.startTS) == Formatter.getDayCodeFromTS(now)) {
                        startTextColor = primaryColor
                    }
                }

                if (dimPastEvents && listEvent.isPastEvent) {
                    startTextColor = startTextColor.adjustAlpha(LOW_ALPHA)
                    endTextColor = endTextColor.adjustAlpha(LOW_ALPHA)
                }
            } else if (listEvent.startTS <= now && listEvent.endTS >= now) {
                startTextColor = primaryColor
            }

            event_item_start.setTextColor(startTextColor)
            event_item_end.setTextColor(endTextColor)
            event_section_title.setTextColor(startTextColor)
            event_item_description.setTextColor(startTextColor)
        }
    }

    private fun setupListSection(view: View, listSection: ListSection, position: Int) {
        view.event_section_title.apply {
            text = listSection.title
            setCompoundDrawablesWithIntrinsicBounds(null, if (position == 0) null else topDivider, null, null)
            var color = if (listSection.isToday) primaryColor else textColor
            if (dimPastEvents && listSection.isPastSection) {
                color = color.adjustAlpha(LOW_ALPHA)
            }
            setTextColor(color)
        }
    }

    private fun shareEvents() {
        val eventIds = ArrayList<Int>(selectedPositions.size)
        selectedPositions.forEach {
            val item = listItems[it]
            if (item is ListEvent) {
                eventIds.add(item.id)
            }
        }
        activity.shareEvents(eventIds.distinct())
    }

    private fun askConfirmDelete() {
        val eventIds = ArrayList<Int>(selectedPositions.size)
        val timestamps = ArrayList<Int>(selectedPositions.size)

        selectedPositions.forEach {
            val item = listItems[it]
            if (item is ListEvent) {
                eventIds.add(item.id)
                timestamps.add(item.startTS)
            }
        }

        DeleteEventDialog(activity, eventIds) {
            val listItemsToDelete = ArrayList<ListItem>(selectedPositions.size)
            selectedPositions.sortedDescending().forEach {
                val listItem = listItems[it]
                listItemsToDelete.add(listItem)
            }
            listItems.removeAll(listItemsToDelete)

            if (it) {
                val eventIDs = Array(eventIds.size, { i -> (eventIds[i].toString()) })
                activity.dbHelper.deleteEvents(eventIDs, true)
            } else {
                eventIds.forEachIndexed { index, value ->
                    activity.dbHelper.addEventRepeatException(value, timestamps[index], true)
                }
            }
            listener?.refreshItems()
            finishActMode()
        }
    }
}
