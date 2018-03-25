package com.simplemobiletools.calendar.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.View
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.models.DayMonthly
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.moveLastItemToFront
import org.joda.time.DateTime
import org.joda.time.Days

// used in the Monthly view fragment, 1 view per screen
class MonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private val BG_CORNER_RADIUS = 4f
    private val ROW_COUNT = 6

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
    private var smallPadding = 0
    private var availableHeightForEvents = 0
    private var maxEventsPerDay = 0
    private var bgRectF = RectF()
    private var dayLetters = ArrayList<String>()
    private var days = ArrayList<DayMonthly>()
    private var dayVerticalOffsets = SparseIntArray()

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        primaryColor = context.getAdjustedPrimaryColor()
        textColor = context.config.textColor
        weakTextColor = textColor.adjustAlpha(LOW_ALPHA)

        smallPadding = resources.displayMetrics.density.toInt()
        val normalTextSize = resources.getDimensionPixelSize(R.dimen.normal_text_size)
        weekDaysLetterHeight = normalTextSize * 2

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = normalTextSize.toFloat()
            textAlign = Paint.Align.CENTER
        }

        val smallerTextSize = resources.getDimensionPixelSize(R.dimen.smaller_text_size)
        eventTitleHeight = smallerTextSize
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
        dayVerticalOffsets.clear()
        if (dayWidth == 0f) {
            dayWidth = canvas.width / 7f
        }

        if (dayHeight == 0f) {
            dayHeight = (canvas.height - weekDaysLetterHeight) / ROW_COUNT.toFloat()
            availableHeightForEvents = dayHeight.toInt() - weekDaysLetterHeight
            maxEventsPerDay = availableHeightForEvents / eventTitleHeight
        }

        // week day letters
        for (i in 0..6) {
            val xPos = (i + 1) * dayWidth - dayWidth / 2
            var weekDayLetterPaint = paint
            if (i == currDayOfWeek) {
                weekDayLetterPaint = getColoredPaint(primaryColor)
            }
            canvas.drawText(dayLetters[i], xPos, weekDaysLetterHeight / 2f, weekDayLetterPaint)
        }

        var curId = 0
        for (y in 0 until ROW_COUNT) {
            for (x in 0..6) {
                var shownDayEvents = 0
                val day = days.getOrNull(curId)
                if (day != null) {
                    dayVerticalOffsets.put(day.indexOnMonthView, dayVerticalOffsets[day.indexOnMonthView] + weekDaysLetterHeight)
                    val xPos = x * dayWidth
                    val yPos = y * dayHeight + dayVerticalOffsets[day.indexOnMonthView]
                    val xPosCenter = xPos + dayWidth / 2
                    if (day.isToday) {
                        canvas.drawCircle(xPosCenter, yPos + paint.textSize * 0.7f, paint.textSize * 0.75f, getCirclePaint(day))
                    }
                    canvas.drawText(day.value.toString(), xPosCenter, yPos + paint.textSize, getTextPaint(day))

                    day.dayEvents.forEach {
                        if (shownDayEvents >= maxEventsPerDay) {
                            canvas.drawText("...", xPosCenter, yPos + paint.textSize * 2 + maxEventsPerDay * eventTitleHeight, getTextPaint(day))
                            return@forEach
                        }

                        val verticalOffset = dayVerticalOffsets[day.indexOnMonthView]

                        val startDateTime = Formatter.getDateTimeFromTS(it.startTS)
                        val endDateTime = Formatter.getDateTimeFromTS(it.endTS)
                        val minTS = Math.max(startDateTime.seconds(), 0)
                        val maxTS = Math.min(endDateTime.seconds(), Int.MAX_VALUE)
                        val daysCnt = Days.daysBetween(Formatter.getDateTimeFromTS(minTS).toLocalDate(), Formatter.getDateTimeFromTS(maxTS).toLocalDate()).days + 1

                        // background rectangle
                        val backgroundY = yPos + verticalOffset
                        val bgLeft = xPos + smallPadding
                        val bgTop = backgroundY + smallPadding - eventTitleHeight
                        val bgRight = xPos - smallPadding + dayWidth * daysCnt
                        val bgBottom = backgroundY + smallPadding * 2
                        bgRectF.set(bgLeft, bgTop, bgRight, bgBottom)
                        canvas.drawRoundRect(bgRectF, BG_CORNER_RADIUS, BG_CORNER_RADIUS, getColoredPaint(it.color))

                        drawEventTitle(it.title, canvas, xPos, yPos + verticalOffset, it.color, daysCnt)
                        dayVerticalOffsets.put(day.indexOnMonthView, verticalOffset + eventTitleHeight + smallPadding * 2)
                        shownDayEvents++
                    }
                }
                curId++
            }
        }
    }

    private fun drawEventTitle(title: String, canvas: Canvas, x: Float, y: Float, eventColor: Int, daysCnt: Int) {
        val ellipsized = TextUtils.ellipsize(title, eventTitlePaint, dayWidth * daysCnt - smallPadding * 4, TextUtils.TruncateAt.END)
        canvas.drawText(title, 0, ellipsized.length, x + smallPadding * 2, y, getEventTitlePaint(eventColor))
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

    private fun getEventTitlePaint(color: Int): Paint {
        val curPaint = Paint(eventTitlePaint)
        curPaint.color = color.getContrastColor()
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
