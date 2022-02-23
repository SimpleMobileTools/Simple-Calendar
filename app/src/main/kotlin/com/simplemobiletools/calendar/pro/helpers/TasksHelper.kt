package com.simplemobiletools.calendar.pro.helpers

import android.app.Activity
import android.content.Context
import com.simplemobiletools.calendar.pro.extensions.eventTypesDB
import com.simplemobiletools.calendar.pro.extensions.tasksDB
import com.simplemobiletools.calendar.pro.extensions.updateWidgets
import com.simplemobiletools.calendar.pro.models.EventType
import com.simplemobiletools.calendar.pro.models.Task
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class TasksHelper(val context: Context) {
    private val tasksDB = context.tasksDB
    private val eventTypesDB = context.eventTypesDB

    fun getEventTypes(activity: Activity, callback: (notes: ArrayList<EventType>) -> Unit) {
        ensureBackgroundThread {
            var eventTypes = ArrayList<EventType>()
            try {
                eventTypes = eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            } catch (ignored: Exception) {
            }

            activity.runOnUiThread {
                callback(eventTypes)
            }
        }
    }

    fun insertTask(task: Task, callback: () -> Unit) {
        tasksDB.insertOrUpdate(task)
        context.updateWidgets()
        callback()
    }
}
