package com.simplemobiletools.calendar.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.models.DayMonthly
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.onGlobalLayout

class DayMonthlyView(context: Context, attrs: AttributeSet, defStyle: Int) : LinearLayout(context, attrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    private var textColor = context.config.textColor
    private var weakTextColor = textColor.adjustAlpha(LOW_ALPHA)

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun setDay(day: DayMonthly) {
        removeAllViews()
        (View.inflate(context, R.layout.day_monthly_item_view, null) as TextView).apply {
            setTextColor(if (day.isThisMonth) textColor else weakTextColor)
            text = day.value.toString()
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(this)

            if (day.isToday) {
                val primaryColor = context.config.primaryColor
                setTextColor(primaryColor.getContrastColor())

                onGlobalLayout {
                    val height = this@apply.height
                    if (height > 0) {
                        val baseDrawable = context.resources.getDrawable(R.drawable.monthly_today_circle)
                        val bitmap = (baseDrawable as BitmapDrawable).bitmap
                        val scaledDrawable = BitmapDrawable(resources, Bitmap.createScaledBitmap(bitmap, height, height, true))
                        scaledDrawable.setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
                        background = scaledDrawable
                    }
                }
            }
        }
    }
}
