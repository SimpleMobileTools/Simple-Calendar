package com.simplemobiletools.calendar.pro.models

data class Attendee(val contactId: Int, var name: String, val email: String, val status: Int, var photoUri: String) {
    fun getPublicName() = if (name.isNotEmpty()) name else email
}
