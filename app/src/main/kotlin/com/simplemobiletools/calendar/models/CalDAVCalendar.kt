package com.simplemobiletools.calendar.models

data class CalDAVCalendar(val id: Long, val displayName: String, val accountName: String, val ownerName: String, val color: Int, val accessLevel: Int) {
    fun canWrite() = accessLevel >= 500
}
