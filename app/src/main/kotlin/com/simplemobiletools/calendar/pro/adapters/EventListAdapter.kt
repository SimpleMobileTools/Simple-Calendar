package com.simplemobiletools.calendar.pro.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.dialogs.DeleteEventDialog
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.extensions.handleEventDeleting
import com.simplemobiletools.calendar.pro.extensions.shareEvents
import com.simplemobiletools.calendar.pro.helpers.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.models.ListEvent
import com.simplemobiletools.calendar.pro.models.ListItem
import com.simplemobiletools.calendar.pro.models.ListSection
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_list_item.view.*
import kotlinx.android.synthetic.main.event_list_section.view.*
import java.util.*

class EventListAdapter(activity: SimpleActivity, var listItems: ArrayList<ListItem>, val allowLongClick: Boolean, val listener: RefreshRecyclerViewListener?,
                       recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private val topDivider = resources.getDrawable(R.drawable.divider_width)
    private val allDayString = resources.getString(R.string.all_day)
    private val replaceDescription = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private val now = getNowSeconds()
    private var use24HourFormat = activity.config.use24HourFormat
    private var currentItemsHash = listItems.hashCode()

    init {
        setupDragListener(true)
        val firstNonPastSectionIndex = listItems.indexOfFirst { it is ListSection && !it.isPastSection }
        if (firstNonPastSectionIndex != -1) {
            activity.runOnUiThread {
                recyclerView.scrollToPosition(firstNonPastSectionIndex)
            }
        }
    }

    override fun getActionMenuId() = R.menu.cab_event_list

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = listItems.filter { it is ListEvent }.size

    override fun getIsItemSelectable(position: Int) = listItems[position] is ListEvent

    override fun getItemSelectionKey(position: Int) = (listItems.getOrNull(position) as? ListEvent)?.hashCode()

    override fun getItemKeyPosition(key: Int) = listItems.indexOfFirst { (it as? ListEvent)?.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerViewAdapter.ViewHolder {
        val layoutId = when (viewType) {
            ITEM_EVENT -> R.layout.event_list_item
            ITEM_EVENT_SIMPLE -> R.layout.event_list_item_simple
            else -> R.layout.event_list_section
        }
        return createViewHolder(layoutId, parent)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val listItem = listItems[position]
        holder.bindView(listItem, true, allowLongClick && listItem is ListEvent) { itemView, layoutPosition ->
            if (listItem is ListSection) {
                setupListSection(itemView, listItem, position)
            } else if (listItem is ListEvent) {
                setupListEvent(itemView, listItem)
            }
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = listItems.size

    override fun getItemViewType(position: Int) = if (listItems[position] is ListEvent) {
        val event = listItems[position] as ListEvent
        val detailField = if (replaceDescription) event.location else event.description
        if (detailField.isNotEmpty()) {
            ITEM_EVENT
        } else if (event.startTS == event.endTS) {
            ITEM_EVENT_SIMPLE
        } else if (event.isAllDay) {
            val startCode = Formatter.getDayCodeFromTS(event.startTS)
            val endCode = Formatter.getDayCodeFromTS(event.endTS)
            if (startCode == endCode) {
                ITEM_EVENT_SIMPLE
            } else {
                ITEM_EVENT
            }
        } else {
            ITEM_EVENT
        }
    } else {
        ITEM_HEADER
    }

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
            event_item_frame.isSelected = selectedKeys.contains(listEvent.hashCode())
            event_item_title.text = listEvent.title
            event_item_description?.text = if (replaceDescription) listEvent.location else listEvent.description
            event_item_start.text = if (listEvent.isAllDay) allDayString else Formatter.getTimeFromTS(context, listEvent.startTS)
            event_item_end?.beInvisibleIf(listEvent.startTS == listEvent.endTS)
            event_item_color_bar.background.applyColorFilter(listEvent.color)

            if (listEvent.startTS != listEvent.endTS) {
                event_item_end?.apply {
                    val startCode = Formatter.getDayCodeFromTS(listEvent.startTS)
                    val endCode = Formatter.getDayCodeFromTS(listEvent.endTS)

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
            if (listEvent.isAllDay || listEvent.startTS <= now && listEvent.endTS <= now) {
                if (listEvent.isAllDay && Formatter.getDayCodeFromTS(listEvent.startTS) == Formatter.getDayCodeFromTS(now)) {
                    startTextColor = primaryColor
                }

                if (dimPastEvents && listEvent.isPastEvent) {
                    startTextColor = startTextColor.adjustAlpha(LOW_ALPHA)
                    endTextColor = endTextColor.adjustAlpha(LOW_ALPHA)
                }
            } else if (listEvent.startTS <= now && listEvent.endTS >= now) {
                startTextColor = primaryColor
            }

            event_item_start.setTextColor(startTextColor)
            event_item_end?.setTextColor(endTextColor)
            event_item_title.setTextColor(startTextColor)
            event_item_description?.setTextColor(startTextColor)
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

    private fun shareEvents() = activity.shareEvents(getSelectedEventIds())

    private fun getSelectedEventIds() = listItems.filter { it is ListEvent && selectedKeys.contains(it.hashCode()) }.map { (it as ListEvent).id }.toMutableList() as ArrayList<Long>

    private fun askConfirmDelete() {
        val eventIds = getSelectedEventIds()
        val eventsToDelete = listItems.filter { selectedKeys.contains((it as? ListEvent)?.hashCode()) } as List<ListEvent>
        val timestamps = eventsToDelete.mapNotNull { (it as? ListEvent)?.startTS }

        val hasRepeatableEvent = eventsToDelete.any { it.isRepeatable }
        DeleteEventDialog(activity, eventIds, hasRepeatableEvent) {
            listItems.removeAll(eventsToDelete)

            ensureBackgroundThread {
                val nonRepeatingEventIDs = eventsToDelete.filter { !it.isRepeatable }.mapNotNull { it.id }.toMutableList()
                activity.eventsHelper.deleteEvents(nonRepeatingEventIDs, true)

                val repeatingEventIDs = eventsToDelete.filter { it.isRepeatable }.map { it.id }
                activity.handleEventDeleting(repeatingEventIDs, timestamps, it)
                activity.runOnUiThread {
                    listener?.refreshItems()
                    finishActMode()
                }
            }
        }
    }
}
