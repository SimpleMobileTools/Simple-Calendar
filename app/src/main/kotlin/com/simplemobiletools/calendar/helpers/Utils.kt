package com.simplemobiletools.calendar.helpers

import android.graphics.Color
import com.simplemobiletools.calendar.R

object Utils {
    fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    val letterIDs: IntArray
        get() = intArrayOf(R.string.sunday_letter, R.string.monday_letter, R.string.tuesday_letter, R.string.wednesday_letter,
                R.string.thursday_letter, R.string.friday_letter, R.string.saturday_letter)
}
