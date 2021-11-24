package com.simplemobiletools.calendar.pro.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.isSaturday
import com.simplemobiletools.calendar.pro.helpers.isWeekend
import com.simplemobiletools.calendar.pro.models.DayYearly
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA
import java.util.*

// used for displaying months at Yearly view
class SmallMonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private var paint: Paint
    private var todayCirclePaint: Paint
    private var dayWidth = 0f
    private var textColor = 0
    private var weekendsTextColor = 0
    private var saturdayTextColor = 0
    private var days = 31
    private var isLandscape = false
    private var highlightWeekends = false
    private var highlightSaturday = false
    private var isSundayFirst = false
    private var isPrintVersion = false
    private var mEvents: ArrayList<DayYearly>? = null

    var firstDay = 0
    var todaysId = 0

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    fun setDays(days: Int) {
        this.days = days
        invalidate()
    }

    fun setEvents(events: ArrayList<DayYearly>?) {
        mEvents = events
        post { invalidate() }
    }

    init {
        val attributes = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SmallMonthView,
            0, 0)

        try {
            days = attributes.getInt(R.styleable.SmallMonthView_days, 31)
        } finally {
            attributes.recycle()
        }

        val baseColor = context.config.textColor
        textColor = baseColor.adjustAlpha(MEDIUM_ALPHA)
        weekendsTextColor = context.config.highlightWeekendsColor.adjustAlpha(MEDIUM_ALPHA)
        highlightWeekends = context.config.highlightWeekends
        saturdayTextColor = context.config.highlightSaturdayColor.adjustAlpha(MEDIUM_ALPHA)
        highlightSaturday = context.config.highlightWeekends && context.config.highlightSaturday
        isSundayFirst = context.config.isSundayFirst

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = resources.getDimensionPixelSize(R.dimen.year_view_day_text_size).toFloat()
            textAlign = Paint.Align.RIGHT
        }

        todayCirclePaint = Paint(paint)
        todayCirclePaint.color = context.getAdjustedPrimaryColor().adjustAlpha(MEDIUM_ALPHA)
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dayWidth == 0f) {
            dayWidth = if (isLandscape) {
                width / 9f
            } else {
                width / 7f
            }
        }

        var curId = 1 - firstDay
        for (y in 1..6) {
            for (x in 1..7) {
                if (curId in 1..days) {
                    val paint = getPaint(curId, x, highlightWeekends, highlightSaturday)
                    canvas.drawText(curId.toString(), x * dayWidth - (dayWidth / 4), y * dayWidth, paint)

                    if (curId == todaysId && !isPrintVersion) {
                        val dividerConstant = if (isLandscape) 6 else 4
                        canvas.drawCircle(x * dayWidth - dayWidth / 2, y * dayWidth - dayWidth / dividerConstant, dayWidth * 0.41f, todayCirclePaint)
                    }
                }
                curId++
            }
        }
    }

    private fun getPaint(curId: Int, weekDay: Int, highlightWeekends: Boolean, highlightSaturday: Boolean): Paint {
        val colors = mEvents?.get(curId)?.eventColors ?: HashSet()
        if (colors.isNotEmpty()) {
            val curPaint = Paint(paint)
            curPaint.color = colors.first()
            return curPaint
        } else if (highlightSaturday && isSaturday(weekDay - 1, isSundayFirst)) {
            val curPaint = Paint(paint)
            curPaint.color = saturdayTextColor
            return curPaint
        } else if (highlightWeekends && isWeekend(weekDay - 1, isSundayFirst)) {
            val curPaint = Paint(paint)
            curPaint.color = weekendsTextColor
            return curPaint
        }

        return paint
    }

    fun togglePrintMode() {
        isPrintVersion = !isPrintVersion
        textColor = if (isPrintVersion) {
            resources.getColor(R.color.theme_light_text_color)
        } else {
            context.config.textColor.adjustAlpha(MEDIUM_ALPHA)
        }

        paint.color = textColor
        invalidate()
    }
}
