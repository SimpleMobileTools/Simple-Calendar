package com.simplemobiletools.calendar.models

data class ListEvent(var id: Int = 0, var startTS: Int = 0, var endTS: Int = 0, var title: String = "", var description: String = "",
                var isAllDay: Boolean, var color: Int, var location: String = "", var isPastEvent: Boolean = false, var isRepeatable: Boolean = false) : ListItem()
