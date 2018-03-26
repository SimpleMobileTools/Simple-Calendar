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
import com.simplemobiletools.calendar.models.Event
import com.simplemobiletools.calendar.models.MonthViewEvent
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
    private var allEvents = ArrayList<MonthViewEvent>()
    private var bgRectF = RectF()
    private var dayLetters = ArrayList<String>()
    private var days = ArrayList<DayMonthly>()
    private var dayVerticalOffsets = SparseIntArray()
    private var dayEventsCount = SparseIntArray()

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
        days.forEach {
            val day = it
            day.dayEvents.forEach {
                val event = it
                if (allEvents.firstOrNull { it.id == event.id } == null) {
                    val monthViewEvent = MonthViewEvent(event.id, event.title, event.startTS, event.color, day.indexOnMonthView, getEventLastingDaysCount(event), day.indexOnMonthView)
                    allEvents.add(monthViewEvent)
                }
            }
        }

        allEvents = allEvents.sortedWith(compareBy({ -it.daysCnt }, { it.startTS }, { it.startDayIndex }, { it.title })).toMutableList() as ArrayList<MonthViewEvent>
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dayVerticalOffsets.clear()
        if (dayWidth == 0f || dayHeight == 0f) {
            measureDaySize(canvas)
        }

        addWeekDayLetters(canvas)

        var curId = 0
        for (y in 0 until ROW_COUNT) {
            for (x in 0..6) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    dayVerticalOffsets.put(day.indexOnMonthView, dayVerticalOffsets[day.indexOnMonthView] + weekDaysLetterHeight)
                    val verticalOffset = dayVerticalOffsets[day.indexOnMonthView]
                    val xPos = x * dayWidth
                    val yPos = y * dayHeight + verticalOffset
                    val xPosCenter = xPos + dayWidth / 2
                    if (day.isToday) {
                        canvas.drawCircle(xPosCenter, yPos + paint.textSize * 0.7f, paint.textSize * 0.75f, getCirclePaint(day))
                    }
                    canvas.drawText(day.value.toString(), xPosCenter, yPos + paint.textSize, getTextPaint(day))
                    dayVerticalOffsets.put(day.indexOnMonthView, (verticalOffset + paint.textSize * 2).toInt())
                }
                curId++
            }
        }

        drawEvents(canvas)
    }

    private fun addWeekDayLetters(canvas: Canvas) {
        for (i in 0..6) {
            val xPos = (i + 1) * dayWidth - dayWidth / 2
            var weekDayLetterPaint = paint
            if (i == currDayOfWeek) {
                weekDayLetterPaint = getColoredPaint(primaryColor)
            }
            canvas.drawText(dayLetters[i], xPos, weekDaysLetterHeight / 2f, weekDayLetterPaint)
        }
    }

    private fun measureDaySize(canvas: Canvas) {
        dayWidth = canvas.width / 7f
        dayHeight = (canvas.height - weekDaysLetterHeight) / ROW_COUNT.toFloat()
        availableHeightForEvents = dayHeight.toInt() - weekDaysLetterHeight
        maxEventsPerDay = availableHeightForEvents / eventTitleHeight
    }

    private fun drawEvents(canvas: Canvas) {
        for (event in allEvents) {
            drawEvent(event, canvas)
        }
    }

    private fun drawEvent(event: MonthViewEvent, canvas: Canvas) {
        val verticalOffset = dayVerticalOffsets[event.startDayIndex]
        val xPos = event.startDayIndex % 7 * dayWidth
        val yPos = (event.startDayIndex / 7) * dayHeight
        val xPosCenter = xPos + dayWidth / 2

        if (dayEventsCount[event.startDayIndex] >= maxEventsPerDay) {
            canvas.drawText("...", xPosCenter, yPos + verticalOffset - eventTitleHeight / 2, getTextPaint(days[event.startDayIndex]))
            return
        }

        // event background rectangle
        val backgroundY = yPos + verticalOffset
        val bgLeft = xPos + smallPadding
        val bgTop = backgroundY + smallPadding - eventTitleHeight
        var bgRight = xPos - smallPadding + dayWidth * event.daysCnt
        val bgBottom = backgroundY + smallPadding * 2
        if (bgRight > canvas.width.toFloat()) {
            bgRight = canvas.width.toFloat() - smallPadding
            val newStartDayIndex = (event.startDayIndex / 7 + 1) * 7
            val newEvent = event.copy(startDayIndex = newStartDayIndex, daysCnt = event.daysCnt - (newStartDayIndex - event.startDayIndex))
            drawEvent(newEvent, canvas)
        }

        val startDayIndex = days[event.originalStartDayIndex]
        val endDayIndex = days[event.startDayIndex + event.daysCnt - 1]
        bgRectF.set(bgLeft, bgTop, bgRight, bgBottom)
        canvas.drawRoundRect(bgRectF, BG_CORNER_RADIUS, BG_CORNER_RADIUS, getEventBackgroundColor(event, startDayIndex, endDayIndex))

        drawEventTitle(event.title, canvas, xPos, yPos + verticalOffset, bgRight - bgLeft, event.color, startDayIndex, endDayIndex)
        dayVerticalOffsets.put(event.startDayIndex, verticalOffset + eventTitleHeight + smallPadding * 2)

        for (i in 0 until event.daysCnt) {
            dayEventsCount.put(event.startDayIndex + i, dayEventsCount[event.startDayIndex + i] + 1)
        }
    }

    private fun drawEventTitle(title: String, canvas: Canvas, x: Float, y: Float, availableWidth: Float, eventColor: Int, startDay: DayMonthly, endDay: DayMonthly) {
        val ellipsized = TextUtils.ellipsize(title, eventTitlePaint, availableWidth - smallPadding, TextUtils.TruncateAt.END)
        canvas.drawText(title, 0, ellipsized.length, x + smallPadding * 2, y, getEventTitlePaint(eventColor, startDay, endDay))
    }

    private fun getTextPaint(startDay: DayMonthly): Paint {
        var paintColor = textColor
        if (startDay.isToday) {
            paintColor = primaryColor.getContrastColor()
        }

        if (!startDay.isThisMonth) {
            paintColor = paintColor.adjustAlpha(LOW_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getColoredPaint(color: Int): Paint {
        val curPaint = Paint(paint)
        curPaint.color = color
        return curPaint
    }

    private fun getEventBackgroundColor(event: MonthViewEvent, startDay: DayMonthly, endDay: DayMonthly): Paint {
        var paintColor = event.color
        if (!startDay.isThisMonth && !endDay.isThisMonth) {
            paintColor = paintColor.adjustAlpha(LOW_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getEventTitlePaint(color: Int, startDay: DayMonthly, endDay: DayMonthly): Paint {
        var paintColor = color.getContrastColor()
        if (!startDay.isThisMonth && !endDay.isThisMonth) {
            paintColor = paintColor.adjustAlpha(LOW_ALPHA)
        }

        val curPaint = Paint(eventTitlePaint)
        curPaint.color = paintColor
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

    private fun getEventLastingDaysCount(event: Event): Int {
        val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
        val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
        return Days.daysBetween(Formatter.getDateTimeFromTS(startDateTime.seconds()).toLocalDate(), Formatter.getDateTimeFromTS(endDateTime.seconds()).toLocalDate()).days + 1
    }
}
