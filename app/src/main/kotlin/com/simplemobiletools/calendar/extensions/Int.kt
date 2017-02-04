package com.simplemobiletools.calendar.extensions

// TODO: how to do "flags & ~flag" in kotlin?
fun Int.removeFlag(flag: Int) = this or flag - flag
