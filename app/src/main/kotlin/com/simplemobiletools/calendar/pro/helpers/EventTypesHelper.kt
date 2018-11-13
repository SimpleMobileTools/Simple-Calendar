package com.simplemobiletools.calendar.pro.helpers

import android.app.Activity
import android.content.Context
import com.simplemobiletools.calendar.pro.extensions.eventTypesDB
import com.simplemobiletools.calendar.pro.models.EventType

class EventTypesHelper {
    fun getEventTypes(activity: Activity, callback: (notes: ArrayList<EventType>) -> Unit) {
        Thread {
            val eventTypes = activity.eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
            activity.runOnUiThread {
                callback(eventTypes)
            }
        }.start()
    }

    fun getEventTypesSync(context: Context) = context.eventTypesDB.getEventTypes().toMutableList() as ArrayList<EventType>
}
