package com.simplemobiletools.calendar.interfaces

import com.simplemobiletools.calendar.models.EventType
import java.util.*

interface DeleteEventTypesListener {
    fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean): Boolean
}
