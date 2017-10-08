package com.simplemobiletools.calendar.extensions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.widget.TextView
import com.simplemobiletools.calendar.R

fun TextView.addResizedBackgroundDrawable(res: Resources, drawableHeight: Int, primaryColor: Int) {
    val baseDrawable = res.getDrawable(R.drawable.monthly_today_circle)
    val bitmap = (baseDrawable as BitmapDrawable).bitmap
    val scaledDrawable = BitmapDrawable(res, Bitmap.createScaledBitmap(bitmap, drawableHeight, drawableHeight, true))
    scaledDrawable.mutate().setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
    background = scaledDrawable
}
