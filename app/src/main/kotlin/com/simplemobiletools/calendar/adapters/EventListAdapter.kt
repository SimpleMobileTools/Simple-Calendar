package com.simplemobiletools.calendar.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.dialogs.DeleteEventDialog
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.dbHelper
import com.simplemobiletools.calendar.extensions.getNowSeconds
import com.simplemobiletools.calendar.extensions.shareEvents
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_list_item.view.*
import java.util.*

class EventListAdapter(activity: SimpleActivity, val listItems: ArrayList<ListItem>, val allowLongClick: Boolean, val listener: RefreshRecyclerViewListener?,
                       recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private val ITEM_EVENT = 0
    private val ITEM_HEADER = 1

    private val topDivider = resources.getDrawable(R.drawable.divider_width)
    private val allDayString = resources.getString(R.string.all_day)
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val redTextColor = resources.getColor(R.color.red_text)
    private val now = activity.getNowSeconds()
    private val todayDate = Formatter.getDayTitle(activity, Formatter.getDayCodeFromTS(now))
    private var use24HourFormat = activity.config.use24hourFormat

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
                    if (Formatter.getDayCodeFromTS(listEvent.startTS) == Formatter.getDayCodeFromTS(now))
                        startTextColor = primaryColor
                } else {
                    startTextColor = redTextColor
                }
                endTextColor = redTextColor
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
            setTextColor(if (listSection.title == todayDate) primaryColor else textColor)
        }
    }

    private fun shareEvents() {
        val eventIds = ArrayList<Int>(selectedPositions.size)
        selectedPositions.forEach {
            eventIds.add((listItems[it] as ListEvent).id)
        }
        activity.shareEvents(eventIds.distinct())
        finishActMode()
    }

    private fun askConfirmDelete() {
        val eventIds = ArrayList<Int>(selectedPositions.size)
        val timestamps = ArrayList<Int>(selectedPositions.size)

        selectedPositions.forEach {
            eventIds.add((listItems[it] as ListEvent).id)
            timestamps.add((listItems[it] as ListEvent).startTS)
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
