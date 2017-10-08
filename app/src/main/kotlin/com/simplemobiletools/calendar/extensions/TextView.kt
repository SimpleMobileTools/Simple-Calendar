package com.simplemobiletools.calendar.extensions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.widget.TextView

fun TextView.addResizedBackgroundDrawable(res: Resources, drawableHeight: Int, primaryColor: Int, drawableId: Int) {
    val baseDrawable = res.getDrawable(drawableId)
    val bitmap = (baseDrawable as BitmapDrawable).bitmap
    val scaledDrawable = BitmapDrawable(res, Bitmap.createScaledBitmap(bitmap, drawableHeight, drawableHeight, true))
    scaledDrawable.mutate().setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
    background = scaledDrawable
}
