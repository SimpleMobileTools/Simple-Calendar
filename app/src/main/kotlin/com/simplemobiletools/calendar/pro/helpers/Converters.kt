package com.simplemobiletools.calendar.pro.helpers

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    private val stringType = object : TypeToken<List<String>>() {}.type

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
}
