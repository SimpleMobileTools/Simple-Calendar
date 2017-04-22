package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.RelativeLayout
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.DayActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.getAppropriateTheme
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.interfaces.NavigationListener
import com.simplemobiletools.calendar.models.Day
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.beGone
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.first_row.*
import kotlinx.android.synthetic.main.fragment_month.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import org.joda.time.DateTime

class MonthFragment : Fragment(), MonthlyCalendar {
    private var mPackageName = ""
    private var mTextColor = 0
    private var mWeakTextColor = 0
    private var mSundayFirst = false
    private var mDayCode = ""

    private var mListener: NavigationListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    lateinit var mConfig: Config
    lateinit var mCalendar: MonthlyCalendarImpl

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_month, container, false)
        mRes = resources

        mHolder = view.calendar_holder
        mDayCode = arguments.getString(DAY_CODE)
        mConfig = context.config
        mSundayFirst = mConfig.isSundayFirst

        setupButtons()

        mPackageName = activity.packageName
        setupLabels()
        mCalendar = MonthlyCalendarImpl(this, context)

        val padding = mRes.getDimension(R.dimen.activity_margin).toInt()
        view.calendar_holder.setPadding(padding, padding, padding, padding)

        return view
    }

    override fun onResume() {
        super.onResume()
        if (mConfig.isSundayFirst != mSundayFirst) {
            mSundayFirst = mConfig.isSundayFirst
            setupLabels()
        }

        mCalendar.apply {
            mTargetDate = Formatter.getDateTimeFromCode(mDayCode)
            getDays()    // prefill the screen asap, even if without events
        }
        updateCalendar()
    }

    fun updateCalendar() {
        mCalendar.updateMonthlyCalendar(Formatter.getDateTimeFromCode(mDayCode))
    }

    override fun updateMonthlyCalendar(month: String, days: List<Day>) {
        activity?.runOnUiThread {
            mHolder.top_value.text = month
            mHolder.top_value.setTextColor(mConfig.textColor)
            updateDays(days)
        }
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }

    private fun setupButtons() {
        val baseColor = mConfig.textColor
        mTextColor = baseColor
        mWeakTextColor = baseColor.adjustAlpha(LOW_ALPHA)

        mHolder.apply {
            top_left_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
            top_right_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
            top_left_arrow.background = null
            top_right_arrow.background = null

            top_left_arrow.setOnClickListener {
                mListener?.goLeft()
            }

            top_right_arrow.setOnClickListener {
                mListener?.goRight()
            }

            top_value.setOnClickListener { showMonthDialog() }
        }
    }

    fun showMonthDialog() {
        activity.setTheme(context.getAppropriateTheme())
        val view = getLayoutInflater(arguments).inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById(R.id.date_picker) as DatePicker
        datePicker.findViewById(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()

        val dateTime = DateTime(mCalendar.mTargetDate.toString())
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        AlertDialog.Builder(context)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { dialog, which -> positivePressed(dateTime, datePicker) }
                .create().apply {
            context.setupDialogStuff(view, this)
        }
    }

    private fun positivePressed(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val newDateTime = dateTime.withDate(year, month, 1)
        mListener?.goToDateTime(newDateTime)
    }

    private fun setupLabels() {
        val letters = letterIDs

        for (i in 0..6) {
            var index = i
            if (!mSundayFirst)
                index = (index + 1) % letters.size

            (mHolder.findViewById(mRes.getIdentifier("label_$i", "id", mPackageName)) as TextView).apply {
                setTextColor(mTextColor)
                text = getString(letters[index])
            }
        }
    }

    private fun updateDays(days: List<Day>) {
        val displayWeekNumbers = mConfig.displayWeekNumbers
        val len = days.size

        if (week_num == null)
            return

        week_num.setTextColor(mTextColor)
        week_num.beVisibleIf(displayWeekNumbers)

        for (i in 0..5) {
            (mHolder.findViewById(mRes.getIdentifier("week_num_$i", "id", mPackageName)) as TextView).apply {
                text = "${days[i * 7 + 3].weekOfYear}:"
                setTextColor(mTextColor)
                beVisibleIf(displayWeekNumbers)
            }
        }

        val weakerText = mTextColor.adjustAlpha(MEDIUM_ALPHA)

        for (i in 0..len - 1) {
            val day = days[i]
            var curTextColor = mWeakTextColor

            if (day.isThisMonth) {
                curTextColor = mTextColor
            }

            (mHolder.findViewById(mRes.getIdentifier("day_$i", "id", mPackageName)) as TextView).apply {
                text = day.value.toString()
                setTextColor(curTextColor)
                setOnClickListener { openDay(day.code) }

                background = if (!day.isThisMonth) {
                    null
                } else if (day.isToday && day.hasEvent) {
                    val drawable = mRes.getDrawable(R.drawable.monthly_day_with_event_today).mutate()
                    drawable.setColorFilter(getDayDotColor(day, weakerText), PorterDuff.Mode.SRC_IN)
                    drawable
                } else if (day.isToday) {
                    val todayCircle = mRes.getDrawable(R.drawable.circle_empty)
                    todayCircle.setColorFilter(weakerText, PorterDuff.Mode.SRC_IN)
                    todayCircle
                } else if (day.hasEvent) {
                    val drawable = mRes.getDrawable(R.drawable.monthly_day_dot).mutate()
                    drawable.setColorFilter(getDayDotColor(day, weakerText), PorterDuff.Mode.SRC_IN)
                    drawable
                } else {
                    null
                }
            }
        }
    }

    private fun getDayDotColor(day: Day, defaultColor: Int): Int {
        val colors = day.eventColors.distinct()
        return if (colors.size == 1)
            colors[0]
        else
            defaultColor
    }

    private fun openDay(code: String) {
        if (code.isEmpty())
            return

        Intent(context, DayActivity::class.java).apply {
            putExtra(DAY_CODE, code)
            startActivity(this)
        }
    }
}
