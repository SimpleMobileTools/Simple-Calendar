package com.simplemobiletools.calendar.pro.extensions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.TextView
import com.simplemobiletools.commons.extensions.applyColorFilter

fun TextView.addResizedBackgroundDrawable(res: Resources, drawableHeight: Int, primaryColor: Int, drawableId: Int) {
    val baseDrawable = res.getDrawable(drawableId)
    val bitmap = (baseDrawable as BitmapDrawable).bitmap
    val scaledDrawable = BitmapDrawable(res, Bitmap.createScaledBitmap(bitmap, drawableHeight, drawableHeight, true))
    scaledDrawable.applyColorFilter(primaryColor)
    background = scaledDrawable
}
