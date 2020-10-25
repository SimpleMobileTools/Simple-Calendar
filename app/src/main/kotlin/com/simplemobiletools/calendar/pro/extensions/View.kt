package com.simplemobiletools.calendar.pro.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

fun View.getViewBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    layout(left, top, right, bottom)
    draw(canvas)
    return bitmap
}
