package com.simplemobiletools.calendar.pro.adapters

import android.os.Build
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi

import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.dialogs.DeleteEventDialog
import com.simplemobiletools.calendar.pro.extensions.*

import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.ITEM_EVENT
import com.simplemobiletools.calendar.pro.helpers.ITEM_EVENT_SIMPLE
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import com.simplemobiletools.commons.helpers.LOWER_ALPHA
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.views.MyRecyclerView
import kotlinx.android.synthetic.main.event_item_day_view.view.*
import kotlinx.android.synthetic.main.event_item_day_view.view.event_item_color_bar
import kotlinx.android.synthetic.main.event_item_day_view.view.event_item_frame
import kotlinx.android.synthetic.main.event_item_day_view.view.event_item_start
import kotlinx.android.synthetic.main.event_item_day_view.view.event_item_title
import kotlinx.android.synthetic.main.event_item_day_view_simple.view.*
import java.time.LocalDate
import java.time.LocalDateTime

class DayEventsAdapter(activity: SimpleActivity, val events: ArrayList<Event>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit)
    : MyRecyclerViewAdapter(activity, recyclerView, null, itemClick) {

    private val allDayString = resources.getString(R.string.all_day)
    private val replaceDescriptionWithLocation = activity.config.replaceDescription
    private val dimPastEvents = activity.config.dimPastEvents
    private var isPrintVersion = false
    var ageCounter = HashMap<Long?,Long>()
    var anniversariesCounter = HashMap<Long?,Long>()

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (viewType) {
            ITEM_EVENT -> R.layout.event_item_day_view
            else -> R.layout.event_item_day_view_simple
        }
        return createViewHolder(layoutId, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.bindView(event, true, true) { itemView, layoutPosition ->
            setupView(itemView, event)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = events.size

    override fun getItemViewType(position: Int): Int {
        val event = events[position]
        val detailField = if (replaceDescriptionWithLocation) event.location else event.description
        return if (detailField.isNotEmpty()) {
            ITEM_EVENT
        } else if (event.startTS == event.endTS) {
            ITEM_EVENT_SIMPLE
        } else if (event.getIsAllDay()) {
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
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.theme_light_text_color)
        } else {
            baseConfig.textColor
        }
        notifyDataSetChanged()
    }

    fun printAge(event: Event):String{
        val anniversariesDate = (anniversariesCounter[event.id]?.let { Formatter.getDateTimeFromTS(it).year.toString()})
        val ageDate = (ageCounter[event.id]?.let { Formatter.getDateTimeFromTS(it).year.toString()})
        if (anniversariesDate != null) {
            return "("+ (Formatter.getDateTimeFromTS(event.startTS).year - anniversariesDate.toInt()).toString()+")"
        }else if(ageDate != null) {
            return "("+ (Formatter.getDateTimeFromTS(event.startTS).year - ageDate.toInt()).toString()+")"
        }
        return ""
    }

    private fun setupView(view: View, event: Event) {
        view.apply {
            event_item_frame.isSelected = selectedKeys.contains(event.id?.toInt())
            event_item_title.text = event.title
            event_item_description?.text = if (replaceDescriptionWithLocation) event.location else event.description
            event_item_date?.text = printAge(event)
            event_item_start.text = if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(context, event.startTS)
            event_item_end?.beInvisibleIf(event.startTS == event.endTS)
            event_item_color_bar.background.applyColorFilter(event.color)

            if (event.startTS != event.endTS) {
                val startCode = Formatter.getDayCodeFromTS(event.startTS)
                val endCode = Formatter.getDayCodeFromTS(event.endTS)

                event_item_end?.apply {
                    text = Formatter.getTimeFromTS(context, event.endTS)
                    if (startCode != endCode) {
                        if (event.getIsAllDay()) {
                            text = Formatter.getDateFromCode(context, endCode, true)
                        } else {
                            append(" (${Formatter.getDateFromCode(context, endCode, true)})")
                        }
                    } else if (event.getIsAllDay()) {
                        beInvisible()
                    }
                }
            }

            var newTextColor = textColor
            if (dimPastEvents && event.isPastEvent && !isPrintVersion) {
                newTextColor = newTextColor.adjustAlpha(LOWER_ALPHA)
            }

            event_item_start.setTextColor(newTextColor)
            event_item_end?.setTextColor(newTextColor)
            event_item_title.setTextColor(newTextColor)
            event_item_date?.setTextColor(newTextColor)
            event_item_description?.setTextColor(newTextColor)
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
