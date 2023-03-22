package com.simplemobiletools.calendar.pro.dialogs

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SimpleActivity
import com.simplemobiletools.calendar.pro.adapters.FilterEventTypeAdapter
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_filter_event_types.view.*

class SelectEventTypesDialog(val activity: SimpleActivity, selectedEventTypes: Set<String>, val callback: (HashSet<String>) -> Unit) {
    private var dialog: AlertDialog? = null
    private val view = activity.layoutInflater.inflate(R.layout.dialog_filter_event_types, null)

    init {
        activity.eventsHelper.getEventTypes(activity, false) {
            view.filter_event_types_list.adapter = FilterEventTypeAdapter(activity, it, selectedEventTypes)

            activity.getAlertDialogBuilder()
                .setPositiveButton(R.string.ok) { _, _ -> confirmEventTypes() }
                .setNegativeButton(R.string.cancel, null)
                .apply {
                    activity.setupDialogStuff(view, this) { alertDialog ->
                        dialog = alertDialog
                    }
                }
        }
    }

    private fun confirmEventTypes() {
        val adapter = view.filter_event_types_list.adapter as FilterEventTypeAdapter
        val selectedItems = adapter.getSelectedItemsList()
            .map { it.toString() }
            .toHashSet()
        callback(selectedItems)
        dialog?.dismiss()
    }
}
