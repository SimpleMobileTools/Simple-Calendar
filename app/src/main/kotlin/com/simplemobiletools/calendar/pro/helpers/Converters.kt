package com.simplemobiletools.calendar.pro.helpers

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplemobiletools.calendar.pro.models.Attendee

class Converters {
    private val gson = Gson()
    private val stringType = object : TypeToken<List<String>>() {}.type
    private val attendeeType = object : TypeToken<List<Attendee>>() {}.type

    @TypeConverter
    fun jsonToStringList(value: String): List<String> {
        val newValue = if (value.isNotEmpty() && !value.startsWith("[")) {
            "[$value]"
        } else {
            value
        }

        return try {
            gson.fromJson(newValue, stringType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun stringListToJson(list: List<String>) = gson.toJson(list)

    @TypeConverter
    fun attendeeListToJson(list: List<Attendee>): String = gson.toJson(list)

    @TypeConverter
    fun jsonToAttendeeList(value: String): List<Attendee> {
        if (value.isEmpty()) {
            return emptyList()
        }

        return try {
            gson.fromJson<ArrayList<Attendee>>(value, attendeeType) ?: ArrayList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
