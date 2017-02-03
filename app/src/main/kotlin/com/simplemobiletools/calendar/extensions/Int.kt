package com.simplemobiletools.calendar.extensions

// TODO: how to do "flags & ~flag" in kotlin?
fun Int.removeFlag(flag: Int) = this - (if (this and flag != 0) flag else 0)
