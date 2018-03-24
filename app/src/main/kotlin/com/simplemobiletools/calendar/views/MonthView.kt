package com.simplemobiletools.calendar.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.models.DayMonthly
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.getContrastColor

// used in the Monthly view fragment, 1 view per screen
class MonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private var paint: Paint
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var textColor = 0
    private var weakTextColor = 0
    private var todayTextColor = 0
    private var days = ArrayList<DayMonthly>()

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        textColor = context.config.textColor
        weakTextColor = textColor.adjustAlpha(LOW_ALPHA)
        todayTextColor = context.config.primaryColor.getContrastColor()

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = resources.getDimensionPixelSize(R.dimen.normal_text_size).toFloat()
            textAlign = Paint.Align.CENTER
        }
    }

    fun updateDays(newDays: ArrayList<DayMonthly>) {
        days = newDays
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dayWidth == 0f) {
            dayWidth = (canvas.width / 7).toFloat()
        }

        if (dayHeight == 0f) {
            dayHeight = (canvas.height / 6).toFloat()
        }

        var curId = 0
        for (y in 0..5) {
            for (x in 1..7) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    canvas.drawText(day.value.toString(), x * dayWidth - dayWidth / 2, y * dayHeight + paint.textSize, getPaint(day))
                }
                curId++
            }
        }
    }

    private fun getPaint(day: DayMonthly): Paint {
        var paintColor = textColor
        if (day.isToday) {
            paintColor = paintColor.getContrastColor()
        }

        if (!day.isThisMonth) {
            paintColor = paintColor.adjustAlpha(LOW_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getColoredPaint(color: Int): Paint {
        val curPaint = Paint(paint)
        curPaint.color = color
        return curPaint
    }
}
