package com.simplemobiletools.calendar.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.adapters.FilterEventTypeAdapter
import com.simplemobiletools.calendar.pro.databinding.DialogFilterEventTypesBinding
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.viewBinding

class SelectEventTypesDialog(val activity: SimpleActivity, selectedEventTypes: Set<String>, val callback: (HashSet<String>) -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding by activity.viewBinding(DialogFilterEventTypesBinding::inflate)

    init {
        activity.eventsHelper.getEventTypes(activity, false) {
            binding.filterEventTypesList.adapter = FilterEventTypeAdapter(activity, it, selectedEventTypes)

            activity.getAlertDialogBuilder()
                .setPositiveButton(R.string.ok) { _, _ -> confirmEventTypes() }
                .setNegativeButton(R.string.cancel, null)
                .apply {
                    activity.setupDialogStuff(binding.root, this) { alertDialog ->
                        dialog = alertDialog
                    }
                }
        }
    }

    private fun confirmEventTypes() {
        val adapter = binding.filterEventTypesList.adapter as FilterEventTypeAdapter
        val selectedItems = adapter.getSelectedItemsList()
            .map { it.toString() }
            .toHashSet()
        callback(selectedItems)
        dialog?.dismiss()
    }
}
