package com.simplemobiletools.calendar.extensions

fun String.substringTo(cnt: Int): String {
    return if (isEmpty()) {
        ""
    } else
        substring(0, Math.min(length, cnt))
}

fun String.getMonthCode() = if (length == 8) substring(0, 6) else ""
