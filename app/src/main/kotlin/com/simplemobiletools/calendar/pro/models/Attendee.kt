package com.simplemobiletools.calendar.pro.models

data class Attendee(val name: String, val email: String, val status: Int) {
    fun getPublicName() = if (name.isNotEmpty()) name else email
}
