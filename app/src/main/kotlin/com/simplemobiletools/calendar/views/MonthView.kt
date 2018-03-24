package com.simplemobiletools.calendar.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config

// used in the Monthly view fragment, 1 view per screen
class MonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private var paint: Paint
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var textColor = 0
    private var days = 42

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        textColor = context.config.textColor

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = resources.getDimensionPixelSize(R.dimen.normal_text_size).toFloat()
            textAlign = Paint.Align.CENTER
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dayWidth == 0f) {
            dayWidth = (canvas.width / 7).toFloat()
        }

        if (dayHeight == 0f) {
            dayHeight = (canvas.height / 6).toFloat()
        }

        var curId = 1
        for (y in 0..5) {
            for (x in 1..7) {
                if (curId in 1..days) {
                    canvas.drawText(curId.toString(), x * dayWidth - dayWidth / 2, y * dayHeight + paint.textSize, paint)
                }
                curId++
            }
        }
    }
}
