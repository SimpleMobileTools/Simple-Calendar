package com.simplemobiletools.calendar.extensions

fun String.substringTo(cnt: Int): String {
    return if (isEmpty()) {
        ""
    } else
        substring(0, Math.min(length, cnt))
}
