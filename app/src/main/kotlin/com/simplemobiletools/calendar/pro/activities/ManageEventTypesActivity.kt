package com.simplemobiletools.calendar.pro.activities

import android.os.Bundle
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.ManageEventTypesAdapter
import com.simplemobiletools.calendar.pro.databinding.ActivityManageEventTypesBinding
import com.simplemobiletools.calendar.pro.dialogs.EditEventTypeDialog
import com.simplemobiletools.calendar.pro.extensions.eventsHelper
import com.simplemobiletools.calendar.pro.interfaces.DeleteEventTypesListener
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.updateTextColors
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.helpers.NavigationIcon
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class ManageEventTypesActivity : SimpleActivity(), DeleteEventTypesListener {

    private val binding by viewBinding(ActivityManageEventTypesBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()

        updateMaterialActivityViews(
            binding.manageEventTypesCoordinator,
            binding.manageEventTypesList,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
        setupMaterialScrollListener(binding.manageEventTypesList, binding.manageEventTypesToolbar)

        getEventTypes()
        updateTextColors(binding.manageEventTypesList)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.manageEventTypesToolbar, NavigationIcon.Arrow)
    }

    private fun showEventTypeDialog(eventType: EventType? = null) {
        EditEventTypeDialog(this, eventType?.copy()) {
            getEventTypes()
        }
    }

    private fun getEventTypes() {
        eventsHelper.getEventTypes(this, false) {
            val adapter = ManageEventTypesAdapter(this, it, this, binding.manageEventTypesList) {
                showEventTypeDialog(it as EventType)
            }
            binding.manageEventTypesList.adapter = adapter
        }
    }

    private fun setupOptionsMenu() {
        binding.manageEventTypesToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_event_type -> showEventTypeDialog()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean): Boolean {
        if (eventTypes.any { it.caldavCalendarId != 0 }) {
            toast(R.string.unsync_caldav_calendar)
            if (eventTypes.size == 1) {
                return false
            }
        }

        ensureBackgroundThread {
            eventsHelper.deleteEventTypes(eventTypes, deleteEvents)
        }

        return true
    }
}
