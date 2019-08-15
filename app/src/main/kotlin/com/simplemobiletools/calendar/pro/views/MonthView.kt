package com.simplemobiletools.calendar.pro.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.View
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.pro.helpers.MEDIUM_ALPHA
import com.simplemobiletools.calendar.pro.models.DayMonthly
import com.simplemobiletools.calendar.pro.models.Event
import com.simplemobiletools.calendar.pro.models.MonthViewEvent
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.moveLastItemToFront
import org.joda.time.DateTime
import org.joda.time.Days

// used in the Monthly view fragment, 1 view per screen
class MonthView(context: Context, attrs: AttributeSet, defStyle: Int) : View(context, attrs, defStyle) {
    private val BG_CORNER_RADIUS = 8f
    private val ROW_COUNT = 6

    private var paint: Paint
    private var eventTitlePaint: TextPaint
    private var gridPaint: Paint
    private var config = context.config
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var primaryColor = 0
    private var textColor = 0
    private var weekDaysLetterHeight = 0
    private var eventTitleHeight = 0
    private var currDayOfWeek = 0
    private var smallPadding = 0
    private var maxEventsPerDay = 0
    private var horizontalOffset = 0
    private var showWeekNumbers = false
    private var dimPastEvents = true
    private var allEvents = ArrayList<MonthViewEvent>()
    private var bgRectF = RectF()
    private var dayLetters = ArrayList<String>()
    private var days = ArrayList<DayMonthly>()
    private var dayVerticalOffsets = SparseIntArray()

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        primaryColor = context.getAdjustedPrimaryColor()
        textColor = config.textColor
        showWeekNumbers = config.showWeekNumbers
        dimPastEvents = config.dimPastEvents

        smallPadding = resources.displayMetrics.density.toInt()
        val normalTextSize = resources.getDimensionPixelSize(R.dimen.normal_text_size)
        weekDaysLetterHeight = normalTextSize * 2

        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = normalTextSize.toFloat()
            textAlign = Paint.Align.CENTER
        }

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.adjustAlpha(LOW_ALPHA)
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
        showWeekNumbers = config.showWeekNumbers
        horizontalOffset = if (showWeekNumbers) eventTitleHeight * 2 else 0
        initWeekDayLetters()
        setupCurrentDayOfWeekIndex()
        groupAllEvents()
        invalidate()
    }

    private fun groupAllEvents() {
        days.forEach {
            val day = it
            day.dayEvents.forEach {
                val event = it

                // make sure we properly handle events lasting multiple days and repeating ones
                val lastEvent = allEvents.lastOrNull { it.id == event.id }
                val daysCnt = getEventLastingDaysCount(event)
                val validDayEvent = isDayValid(event, day.code)
                if ((lastEvent == null || lastEvent.startDayIndex + daysCnt <= day.indexOnMonthView) && !validDayEvent) {
                    val monthViewEvent = MonthViewEvent(event.id!!, event.title, event.startTS, event.color, day.indexOnMonthView,
                            daysCnt, day.indexOnMonthView, event.getIsAllDay(), event.isPastEvent)
                    allEvents.add(monthViewEvent)
                }
            }
        }

        allEvents = allEvents.asSequence().sortedWith(compareBy({ -it.daysCnt }, { !it.isAllDay }, { it.startTS }, { it.startDayIndex }, { it.title }))
                .toMutableList() as ArrayList<MonthViewEvent>
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        dayVerticalOffsets.clear()
        measureDaySize(canvas)

        if (config.showGrid) {
            drawGrid(canvas)
        }

        addWeekDayLetters(canvas)
        if (showWeekNumbers && days.isNotEmpty()) {
            addWeekNumbers(canvas)
        }

        var curId = 0
        for (y in 0 until ROW_COUNT) {
            for (x in 0..6) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    dayVerticalOffsets.put(day.indexOnMonthView, dayVerticalOffsets[day.indexOnMonthView] + weekDaysLetterHeight)
                    val verticalOffset = dayVerticalOffsets[day.indexOnMonthView]
                    val xPos = x * dayWidth + horizontalOffset
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

        for (event in allEvents) {
            drawEvent(event, canvas)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        // vertical lines
        for (i in 0..6) {
            var lineX = i * dayWidth
            if (showWeekNumbers) {
                lineX += horizontalOffset
            }
            canvas.drawLine(lineX, 0f, lineX, canvas.height.toFloat(), gridPaint)
        }

        // horizontal lines
        canvas.drawLine(0f, 0f, canvas.width.toFloat(), 0f, gridPaint)
        for (i in 0..5) {
            canvas.drawLine(0f, i * dayHeight + weekDaysLetterHeight, canvas.width.toFloat(), i * dayHeight + weekDaysLetterHeight, gridPaint)
        }
    }

    private fun addWeekDayLetters(canvas: Canvas) {
        for (i in 0..6) {
            val xPos = horizontalOffset + (i + 1) * dayWidth - dayWidth / 2
            var weekDayLetterPaint = paint
            if (i == currDayOfWeek) {
                weekDayLetterPaint = getColoredPaint(primaryColor)
            }
            canvas.drawText(dayLetters[i], xPos, weekDaysLetterHeight * 0.7f, weekDayLetterPaint)
        }
    }

    private fun addWeekNumbers(canvas: Canvas) {
        val weekNumberPaint = Paint(paint)
        weekNumberPaint.textAlign = Paint.Align.RIGHT

        for (i in 0 until ROW_COUNT) {
            val weekDays = days.subList(i * 7, i * 7 + 7)
            weekNumberPaint.color = if (weekDays.any { it.isToday }) primaryColor else textColor

            // fourth day of the week determines the week of the year number
            val weekOfYear = days.getOrNull(i * 7 + 3)?.weekOfYear ?: 1
            val id = "$weekOfYear:"
            val yPos = i * dayHeight + weekDaysLetterHeight
            canvas.drawText(id, horizontalOffset.toFloat() * 0.9f, yPos + paint.textSize, weekNumberPaint)
        }
    }

    private fun measureDaySize(canvas: Canvas) {
        dayWidth = (canvas.width - horizontalOffset) / 7f
        dayHeight = (canvas.height - weekDaysLetterHeight) / ROW_COUNT.toFloat()
        val availableHeightForEvents = dayHeight.toInt() - weekDaysLetterHeight
        maxEventsPerDay = availableHeightForEvents / eventTitleHeight
    }

    private fun drawEvent(event: MonthViewEvent, canvas: Canvas) {
        var verticalOffset = 0
        for (i in 0 until Math.min(event.daysCnt, 7 - event.startDayIndex % 7)) {
            verticalOffset = Math.max(verticalOffset, dayVerticalOffsets[event.startDayIndex + i])
        }
        val xPos = event.startDayIndex % 7 * dayWidth + horizontalOffset
        val yPos = (event.startDayIndex / 7) * dayHeight
        val xPosCenter = xPos + dayWidth / 2

        if (verticalOffset - eventTitleHeight * 2 > dayHeight) {
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
            if (newStartDayIndex < 42) {
                val newEvent = event.copy(startDayIndex = newStartDayIndex, daysCnt = event.daysCnt - (newStartDayIndex - event.startDayIndex))
                drawEvent(newEvent, canvas)
            }
        }

        val startDayIndex = days[event.originalStartDayIndex]
        val endDayIndex = days[Math.min(event.startDayIndex + event.daysCnt - 1, 41)]
        bgRectF.set(bgLeft, bgTop, bgRight, bgBottom)
        canvas.drawRoundRect(bgRectF, BG_CORNER_RADIUS, BG_CORNER_RADIUS, getEventBackgroundColor(event, startDayIndex, endDayIndex))

        drawEventTitle(event, canvas, xPos, yPos + verticalOffset, bgRight - bgLeft - smallPadding, startDayIndex, endDayIndex)

        for (i in 0 until Math.min(event.daysCnt, 7 - event.startDayIndex % 7)) {
            dayVerticalOffsets.put(event.startDayIndex + i, verticalOffset + eventTitleHeight + smallPadding * 2)
        }
    }

    private fun drawEventTitle(event: MonthViewEvent, canvas: Canvas, x: Float, y: Float, availableWidth: Float, startDay: DayMonthly, endDay: DayMonthly) {
        val ellipsized = TextUtils.ellipsize(event.title, eventTitlePaint, availableWidth - smallPadding, TextUtils.TruncateAt.END)
        canvas.drawText(event.title, 0, ellipsized.length, x + smallPadding * 2, y, getEventTitlePaint(event, startDay, endDay))
    }

    private fun getTextPaint(startDay: DayMonthly): Paint {
        var paintColor = textColor
        if (startDay.isToday) {
            paintColor = primaryColor.getContrastColor()
        }

        if (!startDay.isThisMonth) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
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
        if ((!startDay.isThisMonth && !endDay.isThisMonth) || (dimPastEvents && event.isPastEvent)) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }

        return getColoredPaint(paintColor)
    }

    private fun getEventTitlePaint(event: MonthViewEvent, startDay: DayMonthly, endDay: DayMonthly): Paint {
        var paintColor = event.color.getContrastColor()
        if ((!startDay.isThisMonth && !endDay.isThisMonth) || (dimPastEvents && event.isPastEvent)) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }

        val curPaint = Paint(eventTitlePaint)
        curPaint.color = paintColor
        return curPaint
    }

    private fun getCirclePaint(day: DayMonthly): Paint {
        val curPaint = Paint(paint)
        var paintColor = primaryColor
        if (!day.isThisMonth) {
            paintColor = paintColor.adjustAlpha(MEDIUM_ALPHA)
        }
        curPaint.color = paintColor
        return curPaint
    }

    private fun initWeekDayLetters() {
        dayLetters = context.resources.getStringArray(R.array.week_day_letters).toMutableList() as ArrayList<String>
        if (config.isSundayFirst) {
            dayLetters.moveLastItemToFront()
        }
    }

    private fun setupCurrentDayOfWeekIndex() {
        if (days.firstOrNull { it.isToday && it.isThisMonth } == null) {
            currDayOfWeek = -1
            return
        }

        currDayOfWeek = DateTime().dayOfWeek
        if (config.isSundayFirst) {
            currDayOfWeek %= 7
        } else {
            currDayOfWeek--
        }
    }

    // take into account cases when an event starts on the previous screen, subtract those days
    private fun getEventLastingDaysCount(event: Event): Int {
        val startDateTime = Formatter.getDateTimeFromTS(event.startTS)
        val endDateTime = Formatter.getDateTimeFromTS(event.endTS)
        val code = days.first().code
        val screenStartDateTime = Formatter.getDateTimeFromCode(code).toLocalDate()
        var eventStartDateTime = Formatter.getDateTimeFromTS(startDateTime.seconds()).toLocalDate()
        val eventEndDateTime = Formatter.getDateTimeFromTS(endDateTime.seconds()).toLocalDate()
        val diff = Days.daysBetween(screenStartDateTime, eventStartDateTime).days
        if (diff < 0) {
            eventStartDateTime = screenStartDateTime
        }

        val isMidnight = Formatter.getDateTimeFromTS(endDateTime.seconds()) == Formatter.getDateTimeFromTS(endDateTime.seconds()).withTimeAtStartOfDay()
        val numDays = Days.daysBetween(eventStartDateTime, eventEndDateTime).days
        val daysCnt = if (numDays == 1 && isMidnight) 0 else numDays
        return daysCnt + 1
    }

    private fun isDayValid(event: Event, code: String): Boolean {
        val date = Formatter.getDateTimeFromCode(code)
        return event.startTS != event.endTS && Formatter.getDateTimeFromTS(event.endTS) == Formatter.getDateTimeFromTS(date.seconds()).withTimeAtStartOfDay()
    }
}
