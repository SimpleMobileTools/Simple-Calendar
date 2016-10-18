package com.simplemobiletools.calendar.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.simplemobiletools.calendar.Config
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.Utils

class SmallMonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    var mPaint: Paint
    var mDayWidth = 0f
    var mTextColor = 0

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    init {
        val baseColor = if (Config.newInstance(context).isDarkTheme) Color.WHITE else Color.BLACK
        mTextColor = Utils.adjustAlpha(baseColor, Constants.MEDIUM_ALPHA)

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint.color = mTextColor
        mPaint.textSize = resources.getDimensionPixelSize(R.dimen.tiny_text_size).toFloat()
        mPaint.textAlign = Paint.Align.RIGHT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDayWidth == 0f) {
            mDayWidth = (canvas.width / 7).toFloat()
        }

        var curId = 1
        for (y in 1..6) {
            for (x in 1..7) {
                canvas.drawText(curId.toString(), x * mDayWidth, y * mDayWidth, mPaint)
                curId++
            }
        }
    }
}
