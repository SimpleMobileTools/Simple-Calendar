package com.simplemobiletools.calendar.pro.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.models.DayMonthly
import com.simplemobiletools.commons.extensions.onGlobalLayout
import kotlinx.android.synthetic.main.month_view.view.*

// used in the Monthly view fragment, 1 view per screen
class MonthViewWrapper(context: Context, attrs: AttributeSet, defStyle: Int) : FrameLayout(context, attrs, defStyle) {
    private var dayWidth = 0f
    private var dayHeight = 0f
    private var weekDaysLetterHeight = 0
    private var horizontalOffset = 0
    private var wereViewsAdded = false
    private var days = ArrayList<DayMonthly>()
    private var inflater: LayoutInflater
    private var monthView: MonthView
    private var dayClickCallback: ((day: DayMonthly) -> Unit)? = null

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    init {
        val normalTextSize = resources.getDimensionPixelSize(R.dimen.normal_text_size).toFloat()
        weekDaysLetterHeight = 2 * normalTextSize.toInt()

        inflater = LayoutInflater.from(context)
        monthView = inflater.inflate(R.layout.month_view, this).month_view
        setupHorizontalOffset()

        onGlobalLayout {
            if (!wereViewsAdded && days.isNotEmpty()) {
                measureSizes()
                addViews()
                monthView.updateDays(days)
            }
        }
    }

    fun updateDays(newDays: ArrayList<DayMonthly>, callback: ((DayMonthly) -> Unit)? = null) {
        setupHorizontalOffset()
        measureSizes()
        dayClickCallback = callback
        days = newDays
        if (dayWidth != 0f && dayHeight != 0f) {
            addViews()
        }
        monthView.updateDays(days)
    }

    private fun setupHorizontalOffset() {
        horizontalOffset = if (context.config.showWeekNumbers) resources.getDimensionPixelSize(R.dimen.smaller_text_size) * 2 else 0
    }

    private fun measureSizes() {
        dayWidth = (width - horizontalOffset) / 7f

        // avoid updating the height when coming back from a new event screen, when the keyboard was visible
        val newHeight = (height - weekDaysLetterHeight) / 6f
        if (newHeight > dayHeight) {
            dayHeight = (height - weekDaysLetterHeight) / 6f
        }
    }

    private fun addViews() {
        removeAllViews()
        monthView = inflater.inflate(R.layout.month_view, this).month_view
        wereViewsAdded = true
        var curId = 0
        for (y in 0..5) {
            for (x in 0..6) {
                val day = days.getOrNull(curId)
                if (day != null) {
                    val xPos = x * dayWidth + horizontalOffset
                    val yPos = y * dayHeight + weekDaysLetterHeight
                    addViewBackground(xPos, yPos, day)
                }
                curId++
            }
        }
    }

    private fun addViewBackground(xPos: Float, yPos: Float, day: DayMonthly) {
        inflater.inflate(R.layout.month_view_background, this, false).apply {
            layoutParams.width = dayWidth.toInt()
            layoutParams.height = dayHeight.toInt()
            x = xPos
            y = yPos
            setOnClickListener {
                dayClickCallback?.invoke(day)
            }
            addView(this)
        }
    }
}
