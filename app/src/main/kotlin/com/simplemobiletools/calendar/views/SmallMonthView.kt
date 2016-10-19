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
    var mDays = 31

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
    }

    fun setDays(days: Int) {
        mDays = days
        invalidate()
    }

    init {
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.SmallMonthView,
                0, 0)

        try {
            mDays = a.getInt(R.styleable.SmallMonthView_days, 31)
        } finally {
            a.recycle()
        }

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
                if (curId <= mDays) {
                    canvas.drawText(curId.toString(), x * mDayWidth, y * mDayWidth, mPaint)
                    curId++
                }
            }
        }
    }
}
