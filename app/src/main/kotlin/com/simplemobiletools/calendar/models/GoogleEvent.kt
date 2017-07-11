package com.simplemobiletools.calendar.models

import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class GoogleEvent(val summary: String, val description: String, val status: String, val start: GoogleEventDateTime, val end: GoogleEventDateTime,
                       val reminders: JsonObject, val recurrence: JsonArray)
