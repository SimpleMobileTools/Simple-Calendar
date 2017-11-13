package com.simplemobiletools.calendar.adapters

import android.graphics.PorterDuff
import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
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
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import com.simplemobiletools.commons.interfaces.MyAdapterListener
import kotlinx.android.synthetic.main.event_item_day_view.view.*
import java.util.*

class DayEventsAdapter(val activity: SimpleActivity, val events: List<Event>, val listener: DeleteEventsListener?, val itemClick: (Event) -> Unit) :
        RecyclerView.Adapter<DayEventsAdapter.ViewHolder>() {
    private val config = activity.config
    private var actMode: ActionMode? = null
    private var textColor = config.textColor
    private val multiSelector = MultiSelector()
    private var itemViews = SparseArray<View>()
    private val selectedPositions = HashSet<Int>()
    private var allDayString = activity.resources.getString(R.string.all_day)
    private var replaceDescriptionWithLocation = config.replaceDescription

    fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        itemViews[pos].event_item_frame.isSelected = select

        if (selectedPositions.isEmpty()) {
            finishActMode()
            return
        }

        updateTitle(selectedPositions.size)
    }

    private fun updateTitle(cnt: Int) {
        actMode?.title = "$cnt / ${events.size}"
        actMode?.invalidate()
    }

    private val adapterListener = object : MyAdapterListener {
        override fun toggleItemSelectionAdapter(select: Boolean, position: Int) {
            toggleItemSelection(select, position)
        }

        override fun getSelectedPositions() = selectedPositions
    }

    private val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
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
            activity.menuInflater.inflate(R.menu.cab_day, menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu) = true

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            selectedPositions.forEach {
                itemViews[it].event_item_frame.isSelected = false
            }
            selectedPositions.clear()
            actMode = null
        }
    }

    fun finishActMode() {
        actMode?.finish()
    }

    private fun shareEvents() {
        val selections = multiSelector.selectedPositions
        val eventIds = ArrayList<Int>(selections.size)
        selections.forEach {
            eventIds.add(events[it].id)
        }
        activity.shareEvents(eventIds.distinct())
        finishActMode()
    }

    private fun askConfirmDelete() {
        val selections = multiSelector.selectedPositions
        val eventIds = ArrayList<Int>(selections.size)
        val timestamps = ArrayList<Int>(selections.size)
        selections.forEach {
            eventIds.add(events[it].id)
            timestamps.add(events[it].startTS)
        }

        DeleteEventDialog(activity, eventIds) {
            if (it) {
                listener?.deleteItems(eventIds)
            } else {
                listener?.addEventRepeatException(eventIds, timestamps)
            }
            finishActMode()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.event_item_day_view, parent, false)
        return ViewHolder(view, adapterListener, activity, multiSelectorMode, multiSelector, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        itemViews.put(position, holder.bindView(event, textColor, replaceDescriptionWithLocation, allDayString))
        toggleItemSelection(selectedPositions.contains(position), position)
    }

    override fun getItemCount() = events.size

    class ViewHolder(view: View, val adapterListener: MyAdapterListener, val activity: SimpleActivity, val multiSelectorCallback: ModalMultiSelectorCallback,
                     val multiSelector: MultiSelector, val itemClick: (Event) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(event: Event, textColor: Int, replaceDescriptionWithLocation: Boolean, allDayString: String): View {
            itemView.apply {
                event_item_title.text = event.title
                event_item_description.text = if (replaceDescriptionWithLocation) event.location else event.description
                event_item_start.text = if (event.getIsAllDay()) allDayString else Formatter.getTimeFromTS(context, event.startTS)
                event_item_end.beInvisibleIf(event.startTS == event.endTS)
                event_item_color.setColorFilter(event.color, PorterDuff.Mode.SRC_IN)

                if (event.startTS != event.endTS) {
                    val startCode = Formatter.getDayCodeFromTS(event.startTS)
                    val endCode = Formatter.getDayCodeFromTS(event.endTS)

                    event_item_end.apply {
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

                event_item_start.setTextColor(textColor)
                event_item_end.setTextColor(textColor)
                event_item_title.setTextColor(textColor)
                event_item_description.setTextColor(textColor)

                setOnClickListener { viewClicked(event) }
                setOnLongClickListener { viewLongClicked(); true }
            }

            return itemView
        }

        private fun viewClicked(event: Event) {
            if (multiSelector.isSelectable) {
                val isSelected = adapterListener.getSelectedPositions().contains(adapterPosition)
                adapterListener.toggleItemSelectionAdapter(!isSelected, adapterPosition)
            } else {
                itemClick(event)
            }
        }

        private fun viewLongClicked() {
            if (!multiSelector.isSelectable) {
                activity.startSupportActionMode(multiSelectorCallback)
                adapterListener.toggleItemSelectionAdapter(true, adapterPosition)
            }
        }
    }
}
