package com.simplemobiletools.calendar.pro.interfaces

import com.simplemobiletools.calendar.pro.models.EventType
import java.util.*

interface DeleteEventTypesListener {
    fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean): Boolean
}
