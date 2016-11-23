package com.simplemobiletools.calendar.adapters

import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.view.*
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.filepicker.dialogs.ConfirmationDialog
import kotlinx.android.synthetic.main.event_item.view.*
import java.util.*

class EventsAdapter(val activity: SimpleActivity, val mItems: List<Event>, val listener: ItemOperationsListener?, val itemClick: (Event) -> Unit) :
        RecyclerView.Adapter<EventsAdapter.ViewHolder>() {
    val multiSelector = MultiSelector()
    val views = ArrayList<View>()

    companion object {
        var actMode: ActionMode? = null
        val markedItems = HashSet<Int>()

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

    val multiSelectorMode = object : ModalMultiSelectorCallback(multiSelector) {
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.cab_delete -> {
                    askConfirmDelete()
                    true
                }
                else -> false
            }
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

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            actMode?.finish()
            prepareForDeleting()
        }
    }

    private fun prepareForDeleting() {
        val selections = multiSelector.selectedPositions
        val ids = ArrayList<Int>(selections.size)
        selections.forEach { ids.add(mItems[it].id) }
        listener?.prepareForDeleting(ids)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.event_item, parent, false)
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
                event_item_start.text = Formatter.getTime(event.startTS)

                if (event.startTS == event.endTS) {
                    event_item_end.visibility = View.INVISIBLE
                } else {
                    event_item_end.text = Formatter.getTime(event.endTS)
                    event_item_end.visibility = View.VISIBLE
                }

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

    interface ItemOperationsListener {
        fun prepareForDeleting(ids: ArrayList<Int>)
    }
}
