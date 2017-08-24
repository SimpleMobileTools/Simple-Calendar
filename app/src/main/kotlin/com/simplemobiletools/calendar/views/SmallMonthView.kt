package com.simplemobiletools.calendar.views

import android.content.Context
import android.content.res.Configuration
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
    var mIsLandscape = false

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
            textSize = resources.getDimensionPixelSize(R.dimen.year_view_day_text_size).toFloat()
            textAlign = Paint.Align.RIGHT
        }

        mColoredPaint = Paint(mPaint)
        mColoredPaint.color = mColoredTextColor
        mIsLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDayWidth == 0f) {
            mDayWidth = if (mIsLandscape) {
                (canvas.width / 9).toFloat()
            } else {
                (canvas.width / 7).toFloat()
            }
        }

        var curId = 1 - mFirstDay
        for (y in 1..6) {
            for (x in 1..7) {
                if (curId in 1..mDays) {
                    canvas.drawText(curId.toString(), x * mDayWidth, y * mDayWidth, getPaint(curId))

                    if (curId == mTodaysId) {
                        val dividerConstant = if (mIsLandscape) 6 else 4
                        canvas.drawCircle(x * mDayWidth - mDayWidth / dividerConstant, y * mDayWidth - mDayWidth / dividerConstant, mDayWidth * 0.41f, mColoredPaint)
                    }
                }
                curId++
            }
        }
    }

    private fun getPaint(curId: Int) = if (mEvents?.contains(curId) == true) mColoredPaint else mPaint
}
