package com.simplemobiletools.calendar.pro.extensions

import android.content.res.Resources
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import com.simplemobiletools.commons.extensions.addBit
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.removeBit

fun TextView.addResizedBackgroundDrawable(res: Resources, drawableHeight: Int, primaryColor: Int, drawableId: Int) {
    val baseDrawable = res.getDrawable(drawableId).toBitmap(drawableHeight, drawableHeight)
    val scaledDrawable = BitmapDrawable(res, baseDrawable)
    scaledDrawable.applyColorFilter(primaryColor)
    background = scaledDrawable
}

fun TextView.checkViewStrikeThrough(addFlag: Boolean) {
    paintFlags = if (addFlag) {
        paintFlags.addBit(Paint.STRIKE_THRU_TEXT_FLAG)
    } else {
        paintFlags.removeBit(Paint.STRIKE_THRU_TEXT_FLAG)
    }
}
