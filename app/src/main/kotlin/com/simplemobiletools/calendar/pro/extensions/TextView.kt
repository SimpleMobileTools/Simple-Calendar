package com.simplemobiletools.calendar.pro.extensions

import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import com.simplemobiletools.commons.extensions.applyColorFilter

fun TextView.addResizedBackgroundDrawable(res: Resources, drawableHeight: Int, primaryColor: Int, drawableId: Int) {
    val baseDrawable = res.getDrawable(drawableId).toBitmap(drawableHeight, drawableHeight)
    val scaledDrawable = BitmapDrawable(res, baseDrawable)
    scaledDrawable.applyColorFilter(primaryColor)
    background = scaledDrawable
}
