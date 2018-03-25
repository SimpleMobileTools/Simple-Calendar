package com.simplemobiletools.calendar.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.View
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.models.DayMonthly
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.moveLastItemToFront
import org.joda.time.DateTime

// used in the Monthly view fragment, 1 view per screen
class MonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private var paint: Paint
    private var eventTitlePaint: TextPaint
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var primaryColor = 0
    private var textColor = 0
    private var weakTextColor = 0
    private var weekDaysLetterHeight = 0
    private var eventTitleHeight = 0
    private var currDayOfWeek = 0
    private var eventTitlePadding = 0
    private var dayLetters = ArrayList<String>()
    private var days = ArrayList<DayMonthly>()
    private var dayVerticalOffsets = SparseIntArray()

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        primaryColor = context.getAdjustedPrimaryColor()
        textColor = context.config.textColor
        weakTextColor = textColor.adjustAlpha(LOW_ALPHA)

        eventTitlePadding = resources.getDimensionPixelSize(R.dimen.tiny_margin)
        val normalTextSize = resources.getDimensionPixelSize(R.dimen.normal_text_size)
        weekDaysLetterHeight = 2 * normalTextSize

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = normalTextSize.toFloat()
            textAlign = Paint.Align.CENTER
        }

        val smallerTextSize = resources.getDimensionPixelSize(R.dimen.smaller_text_size)
        eventTitleHeight = smallerTextSize + 2 * eventTitlePadding
        eventTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = smallerTextSize.toFloat()
            textAlign = Paint.Align.LEFT
        }

        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
    }

    fun updateDays(newDays: ArrayList<DayMonthly>) {
        days = newDays
        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dayWidth == 0f) {
            dayWidth = canvas.width / 7f
        }

        if (dayHeight == 0f) {
            dayHeight = (canvas.height - weekDaysLetterHeight) / 6f
        }

        for (i in 0..6) {
            val xPos = (i + 1) * dayWidth - dayWidth / 2
            var weekDayLetterPaint = paint
            if (i == currDayOfWeek) {
                weekDayLetterPaint = getColoredPaint(primaryColor)
            }
            canvas.drawText(dayLetters[i], xPos, weekDaysLetterHeight / 2f, weekDayLetterPaint)
        }

        var curId = 0
        for (y in 0..5) {
            for (x in 0..6) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    val xPos = x * dayWidth
                    val yPos = y * dayHeight + weekDaysLetterHeight
                    val xPosCenter = xPos + dayWidth / 2
                    if (day.isToday) {
                        canvas.drawCircle(xPosCenter, yPos + paint.textSize * 0.7f, paint.textSize * 0.75f, getCirclePaint(day))
                    }
                    canvas.drawText(day.value.toString(), xPosCenter, yPos + paint.textSize, getTextPaint(day))
                    dayVerticalOffsets.put(day.indexOnMonthView, weekDaysLetterHeight)

                    day.dayEvents.forEach {
                        drawEventTitle(it.title, canvas, xPos, yPos + dayVerticalOffsets[day.indexOnMonthView])
                        dayVerticalOffsets.put(day.indexOnMonthView, dayVerticalOffsets[day.indexOnMonthView] + eventTitleHeight)
                    }
                }
                curId++
            }
        }
    }

    private fun drawEventTitle(title: String, canvas: Canvas, x: Float, y: Float) {
        val ellipsized = TextUtils.ellipsize(title, eventTitlePaint, dayWidth - 2 * eventTitlePadding, TextUtils.TruncateAt.END)
        canvas.drawText(title, 0, ellipsized.length, x + eventTitlePadding, y, eventTitlePaint)
    }

    private fun getTextPaint(day: DayMonthly): Paint {
        var paintColor = textColor
        if (day.isToday) {
            paintColor = primaryColor.getContrastColor()
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

    private fun getCirclePaint(day: DayMonthly): Paint {
        val curPaint = Paint(paint)
        var paintColor = primaryColor
        if (!day.isThisMonth) {
            paintColor = paintColor.adjustAlpha(LOW_ALPHA)
        }
        curPaint.color = paintColor
        return curPaint
    }

    private fun initWeekDayLetters() {
        dayLetters = context.resources.getStringArray(R.array.week_day_letters).toList() as ArrayList<String>
        if (context.config.isSundayFirst) {
            dayLetters.moveLastItemToFront()
        }
    }

    private fun setupCurrentDayOfWeekIndex() {
        if (days.firstOrNull { it.isToday && it.isThisMonth } == null) {
            currDayOfWeek = -1
            return
        }

        currDayOfWeek = DateTime().dayOfWeek
        if (context.config.isSundayFirst) {
            currDayOfWeek %= 7
        } else {
            currDayOfWeek--
        }
    }
}
