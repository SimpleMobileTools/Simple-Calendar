package com.simplemobiletools.calendar.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.MEDIUM_ALPHA
import com.simplemobiletools.commons.extensions.adjustAlpha
import java.util.*

class SmallMonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    var mPaint: Paint
    var mColoredPaint: Paint
    var mDayWidth = 0f
    var mTextColor = 0
    var mColoredTextColor = 0
    var mDays = 31
    var mFirstDay = 0
    var mTodaysId = 0

    var mEvents: ArrayList<Int>? = null

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    fun setDays(days: Int) {
        mDays = days
        invalidate()
    }

    fun setFirstDay(firstDay: Int) {
        mFirstDay = firstDay
    }

    fun setEvents(events: ArrayList<Int>?) {
        mEvents = events
        post { invalidate() }
    }

    fun setTodaysId(id: Int) {
        mTodaysId = id
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

        val baseColor = context.config.textColor
        mTextColor = baseColor.adjustAlpha(MEDIUM_ALPHA)
        mColoredTextColor = context.config.primaryColor.adjustAlpha(MEDIUM_ALPHA)

        mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mTextColor
            textSize = resources.getDimensionPixelSize(R.dimen.tiny_text_size).toFloat()
            textAlign = Paint.Align.RIGHT
        }

        mColoredPaint = Paint(mPaint)
        mColoredPaint.color = mColoredTextColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDayWidth == 0f) {
            mDayWidth = (canvas.width / 7).toFloat()
        }

        var curId = 1 - mFirstDay
        for (y in 1..6) {
            for (x in 1..7) {
                if (curId > 0 && curId <= mDays) {
                    canvas.drawText(curId.toString(), x * mDayWidth, y * mDayWidth, getPaint(curId))

                    if (curId == mTodaysId) {
                        canvas.drawCircle(x * mDayWidth - mDayWidth / 4, y * mDayWidth - mDayWidth / 4, mDayWidth * 0.41f, mColoredPaint)
                    }
                }
                curId++
            }
        }
    }

    private fun getPaint(curId: Int) = if (mEvents?.contains(curId) == true) mColoredPaint else mPaint
}
