package com.simplemobiletools.calendar.adapters

import android.support.v7.view.ActionMode
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.simplemobiletools.calendar.activities.SimpleActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.commons.interfaces.MyAdapterListener
import java.util.*

abstract class MyAdapter(val activity: SimpleActivity, val itemClick: (Any) -> Unit) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    protected val config = activity.config
    protected val resources = activity.resources
    protected var actMode: ActionMode? = null
    protected var primaryColor = config.primaryColor
    protected var textColor = config.textColor
    protected val itemViews = SparseArray<View>()
    protected val selectedPositions = HashSet<Int>()
    protected val multiSelector = MultiSelector()

    abstract fun getActionMenuId(): Int

    abstract fun getSelectableItemCount(): Int

    abstract fun markItemSelection(select: Boolean, pos: Int)

    abstract fun actionItemPressed(id: Int)

    protected fun toggleItemSelection(select: Boolean, pos: Int) {
        if (select) {
            if (itemViews[pos] != null) {
                selectedPositions.add(pos)
            }
        } else {
            selectedPositions.remove(pos)
        }

        markItemSelection(select, pos)

        if (selectedPositions.isEmpty()) {
            finishActMode()
            return
        }

        updateTitle(selectedPositions.size)
    }

    private fun updateTitle(cnt: Int) {
        actMode?.title = "$cnt / ${getSelectableItemCount()}"
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
            actionItemPressed(item.itemId)
            return true
        }

        override fun onCreateActionMode(actionMode: ActionMode?, menu: Menu?): Boolean {
            super.onCreateActionMode(actionMode, menu)
            actMode = actionMode
            activity.menuInflater.inflate(getActionMenuId(), menu)
            return true
        }

        override fun onPrepareActionMode(actionMode: ActionMode?, menu: Menu) = true

        override fun onDestroyActionMode(actionMode: ActionMode?) {
            super.onDestroyActionMode(actionMode)
            selectedPositions.forEach {
                markItemSelection(false, it)
            }
            selectedPositions.clear()
            actMode = null
        }
    }

    fun finishActMode() {
        actMode?.finish()
    }

    protected fun createViewHolder(view: View) = ViewHolder(view, adapterListener, activity, multiSelectorMode, multiSelector, itemClick)

    class ViewHolder(view: View, val adapterListener: MyAdapterListener, val activity: SimpleActivity, val multiSelectorCallback: ModalMultiSelectorCallback,
                     val multiSelector: MultiSelector, val itemClick: (Any) -> (Unit)) : SwappingHolder(view, multiSelector) {
        fun bindView(any: Any, callback: (itemView: View) -> Unit): View {
            return itemView.apply {
                callback(this)
                itemView.setOnClickListener { viewClicked(any) }
                itemView.setOnLongClickListener { viewLongClicked(); true }
            }
        }

        private fun viewClicked(any: Any) {
            if (multiSelector.isSelectable) {
                val isSelected = adapterListener.getSelectedPositions().contains(adapterPosition)
                adapterListener.toggleItemSelectionAdapter(!isSelected, adapterPosition)
            } else {
                itemClick(any)
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
