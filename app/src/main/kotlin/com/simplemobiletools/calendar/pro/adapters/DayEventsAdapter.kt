package com.simplemobiletools.calendar.pro.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.dialogs.DeleteEventDialog
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_list_item.view.*

class DayEventsAdapter(activity: SimpleActivity, val events: ArrayList<Event>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    private val allDayString = resources.getString(R.string.all_day)
    private val displayDescription = activity.config.displayDescription
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private var isPrintVersion = false
    private val mediumMargin = activity.resources.getDimension(R.dimen.medium_margin).toInt()

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_day

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        when (id) {
            R.id.cab_share -> shareEvents()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = events.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = events.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = events.indexOfFirst { it.id?.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.event_list_item, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.bindView(event, true, true) { itemView, layoutPosition ->
            setupView(itemView, event)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = events.size

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.theme_light_text_color)
        } else {
            activity.getProperTextColor()
        }
        notifyDataSetChanged()
    }

    private fun setupView(view: View, event: Event) {
        view.apply {
            event_item_holder.isSelected = selectedKeys.contains(event.id?.toInt())
            event_item_holder.background.applyColorFilter(textColor)
            event_item_title.text = event.title
            event_item_title.checkViewStrikeThrough(event.isTaskCompleted())
            event_item_time.text = if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(context, event.startTS)
            if (event.startTS != event.endTS && !event.getIsAllDay()) {
                event_item_time.text = "${event_item_time.text} - ${Formatter.getTimeFromTS(context, event.endTS)}"
            }

            event_item_description.text = if (replaceDescriptionWithLocation) event.location else event.description.replace("\n", " ")
            event_item_description.beVisibleIf(displayDescription && event_item_description.text.isNotEmpty())
            event_item_color_bar.background.applyColorFilter(event.color)

            var newTextColor = textColor
            if (dimPastEvents && event.isPastEvent && !isPrintVersion) {
                newTextColor = newTextColor.adjustAlpha(MEDIUM_ALPHA)
            }

            event_item_time.setTextColor(newTextColor)
            event_item_title.setTextColor(newTextColor)
            event_item_description?.setTextColor(newTextColor)
            event_item_task_image.applyColorFilter(newTextColor)
            event_item_task_image.beVisibleIf(event.isTask())

            val startMargin = if (event.isTask()) {
                0
            } else {
                mediumMargin
            }
            (event_item_title.layoutParams as ConstraintLayout.LayoutParams).marginStart = startMargin
        }
    }

    private fun shareEvents() = activity.shareEvents(selectedKeys.distinct().map { it.toLong() })

    private fun askConfirmDelete() {
        val eventIds = selectedKeys.map { it.toLong() }.toMutableList()
        val eventsToDelete = events.filter { selectedKeys.contains(it.id?.toInt()) }
        val timestamps = eventsToDelete.map { it.startTS }
        val positions = getSelectedItemPositions()

        val hasRepeatableEvent = eventsToDelete.any { it.repeatInterval > 0 }
        DeleteEventDialog(activity, eventIds, hasRepeatableEvent) { it ->
            events.removeAll(eventsToDelete)

            ensureBackgroundThread {
                val nonRepeatingEventIDs = eventsToDelete.asSequence().filter { it.repeatInterval == 0 }.mapNotNull { it.id }.toMutableList()
                activity.eventsHelper.deleteEvents(nonRepeatingEventIDs, true)

                val repeatingEventIDs = eventsToDelete.asSequence().filter { it.repeatInterval != 0 }.mapNotNull { it.id }.toList()
                activity.handleEventDeleting(repeatingEventIDs, timestamps, it)
                activity.runOnUiThread {
                    removeSelectedItems(positions)
                }
            }
        }
    }
}
