package com.simplemobiletools.calendar.extensions

import com.google.gson.Gson
import com.simplemobiletools.calendar.models.GoogleError

fun Exception.getGoogleMessageError(): String {
    val json = message!!.substring(message!!.indexOf('{'))
    val error = Gson().fromJson<GoogleError>(json, GoogleError::class.java)
    return error.message
}
