package com.simplemobiletools.calendar.models

import java.io.Serializable

class Event(var id: Int = 0, var startTS: Int = 0, var endTS: Int = 0, var title: String = "", var description: String = "",
            var reminderMinutes: Int = 0, var repeatInterval: Int = 0) : Serializable {
    override fun toString(): String {
        return "Event {id=$id, startTS=$startTS, endTS=$endTS, title=$title, description=$description, " +
                "reminderMinutes=$reminderMinutes, repeatInterval=$repeatInterval}"
    }

    companion object {
        private val serialVersionUID = -32456795132344616L
    }
}
