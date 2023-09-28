package com.simplemobiletools.calendar.pro.models

data class ListEvent(
    var id: Long,
    var startTS: Long,
    var endTS: Long,
    var title: String,
    var description: String,
    var isAllDay: Boolean,
    var color: Int,
    var location: String,
    var isPastEvent: Boolean,
    var isRepeatable: Boolean,
    var isTask: Boolean,
    var isTaskCompleted: Boolean,
    var isAttendeeInviteDeclined: Boolean
) : ListItem() {

    companion object {
        val empty = ListEvent(
            id = 0,
            startTS = 0,
            endTS = 0,
            title = "",
            description = "",
            isAllDay = false,
            color = 0,
            location = "",
            isPastEvent = false,
            isRepeatable = false,
            isTask = false,
            isTaskCompleted = false,
            isAttendeeInviteDeclined = false
        )
    }
}
