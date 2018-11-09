package com.simplemobiletools.calendar.pro.models

data class CalDAVCalendar(val id: Int, val displayName: String, val accountName: String, val accountType: String, val ownerName: String,
                          var color: Int, val accessLevel: Int) {
    fun canWrite() = accessLevel >= 500

    fun getFullTitle() = "$displayName ($accountName)"
}
