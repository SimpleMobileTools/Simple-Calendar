package com.simplemobiletools.calendar.pro.models

data class ListEvent(var id: Int, var startTS: Int, var endTS: Int, var title: String, var description: String, var isAllDay: Boolean, var color: Int,
                     var location: String, var isPastEvent: Boolean, var isRepeatable: Boolean) : ListItem()
