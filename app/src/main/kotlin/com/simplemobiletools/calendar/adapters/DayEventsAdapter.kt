package com.simplemobiletools.calendar.adapters

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
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.commons.extensions.beInvisible
import com.simplemobiletools.commons.extensions.beInvisibleIf
import kotlinx.android.synthetic.main.event_item_day_view.view.*
import java.util.*

class DayEventsAdapter(val activity: SimpleActivity, val mItems: List<Event>, val listener: DeleteEventsListener?, val itemClick: (Event) -> Unit) :
        RecyclerView.Adapter<DayEventsAdapter.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()

    companion object {
        var actMode: ActionMode? = null
        val markedItems = HashSet<Int>()
        var textColor = 0
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
        textColor = activity.config.textColor
        allDayString = activity.resources.getString(R.string.all_day)
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
            activity.menuInflater.inflate(R.menu.cab_day, menu)
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
            eventIds.add(mItems[it].id)
        }
        activity.shareEvents(eventIds.distinct())
        actMode?.finish()
    }

    private fun askConfirmDelete() {
        val selections = multiSelector.selectedPositions
        val eventIds = ArrayList<Int>(selections.size)
        val timestamps = ArrayList<Int>(selections.size)
        selections.forEach {
            eventIds.add(mItems[it].id)
            timestamps.add(mItems[it].startTS)
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

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.event_item_day_view, parent, false)
        return ViewHolder(activity, view, itemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        views.add(holder.bindView(multiSelectorMode, multiSelector, mItems[position], position))
    }

    override fun getItemCount() = mItems.size

    class ViewHolder(val activity: SimpleActivity, view: View, val itemClick: (Event) -> (Unit)) : SwappingHolder(view, MultiSelector()) {
        fun bindView(multiSelectorCallback: ModalMultiSelectorCallback, multiSelector: MultiSelector, event: Event, pos: Int): View {

            itemView.apply {
                event_item_title.text = event.title
                event_item_description.text = event.description
                event_item_start.text = if (event.isAllDay) allDayString else Formatter.getTimeFromTS(context, event.startTS)
                event_item_end.beInvisibleIf(event.startTS == event.endTS)
                toggleItemSelection(this, markedItems.contains(pos), pos)

                if (event.startTS != event.endTS) {
                    val startCode = Formatter.getDayCodeFromTS(event.startTS)
                    val endCode = Formatter.getDayCodeFromTS(event.endTS)

                    event_item_end.apply {
                        text = Formatter.getTimeFromTS(context, event.endTS)
                        if (startCode != endCode) {
                            if (event.isAllDay) {
                                text = Formatter.getDateFromCode(context, endCode, true)
                            } else {
                                append(" (${Formatter.getDateFromCode(context, endCode, true)})")
                            }
                        } else if (event.isAllDay) {
                            beInvisible()
                        }
                    }
                }

                event_item_start.setTextColor(textColor)
                event_item_end.setTextColor(textColor)
                event_item_title.setTextColor(textColor)
                event_item_description.setTextColor(textColor)

                setOnClickListener { viewClicked(multiSelector, event, pos) }
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

        fun viewClicked(multiSelector: MultiSelector, event: Event, pos: Int) {
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
                itemClick(event)
            }
        }
    }
}
