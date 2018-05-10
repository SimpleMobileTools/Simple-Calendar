package com.simplemobiletools.calendar.models

data class EventType(var id: Int = 0, var title: String, var color: Int, var caldavCalendarId: Int = 0, var caldavDisplayName: String = "", var caldavEmail: String = "") {
    fun getDisplayTitle() = if (caldavCalendarId == 0) title else "$caldavDisplayName ($caldavEmail)"

    fun isSyncedEventType() = caldavCalendarId != 0
}
