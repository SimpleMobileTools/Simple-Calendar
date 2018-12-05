package com.simplemobiletools.calendar.pro.helpers

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    private val stringType = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun jsonToStringList(value: String) = gson.fromJson<ArrayList<String>>(value, stringType)

    @TypeConverter
    fun stringListToJson(list: ArrayList<String>) = gson.toJson(list)
}
