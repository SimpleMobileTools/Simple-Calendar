package com.simplemobiletools.calendar.models

class ListEvent(var id: Int = 0, var startTS: Int = 0, var endTS: Int = 0, var title: String = "", var description: String = "",
                var isAllDay: Boolean = false) : ListItem() {
    override fun toString() = "Event {id=$id, startTS=$startTS, endTS=$endTS, title=$title, description=$description, isAllDay=$isAllDay}"
}
