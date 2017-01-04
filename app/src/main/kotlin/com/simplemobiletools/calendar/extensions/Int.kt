package com.simplemobiletools.calendar.extensions

import android.graphics.Paint

// TODO: how to do "flags & ~Paint.UNDERLINE_TEXT_FLAG" in kotlin?
fun Int.removeUnderlineFlag(): Int {
    return this - (if (this and Paint.UNDERLINE_TEXT_FLAG == Paint.UNDERLINE_TEXT_FLAG) Paint.UNDERLINE_TEXT_FLAG else 0)
}
