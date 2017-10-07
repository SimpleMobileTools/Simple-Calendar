package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.DayActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.getAppropriateTheme
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.interfaces.NavigationListener
import com.simplemobiletools.calendar.models.DayMonthly
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.first_row.*
import kotlinx.android.synthetic.main.fragment_month.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import org.joda.time.DateTime

class MonthFragment : Fragment(), MonthlyCalendar {
    private var mTextColor = 0
    private var mWeakTextColor = 0
    private var mSundayFirst = false
    private var mDayCode = ""
    private var dividerMargin = 0

    var listener: NavigationListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    lateinit var mConfig: Config
    lateinit var mCalendar: MonthlyCalendarImpl

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_month, container, false)
        mRes = resources
        dividerMargin = mRes.displayMetrics.density.toInt()

        mHolder = view.calendar_holder
        mDayCode = arguments.getString(DAY_CODE)
        mConfig = context.config
        mSundayFirst = mConfig.isSundayFirst

        setupButtons()

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

    override fun updateMonthlyCalendar(month: String, days: List<DayMonthly>) {
        activity?.runOnUiThread {
            mHolder.top_value.text = month
            mHolder.top_value.setTextColor(mConfig.textColor)
            updateDays(days)
        }
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
                listener?.goLeft()
            }

            top_right_arrow.setOnClickListener {
                listener?.goRight()
            }

            top_value.setOnClickListener { showMonthDialog() }
        }
    }

    private fun showMonthDialog() {
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
        listener?.goToDateTime(newDateTime)
    }

    private fun setupLabels() {
        val letters = letterIDs

        for (i in 0..6) {
            var index = i
            if (!mSundayFirst)
                index = (index + 1) % letters.size

            (mHolder.findViewById(mRes.getIdentifier("label_$i", "id", activity.packageName)) as TextView).apply {
                setTextColor(mTextColor)
                text = getString(letters[index])
            }
        }
    }

    private fun updateDays(days: List<DayMonthly>) {
        val packageName = activity.packageName
        val displayWeekNumbers = mConfig.displayWeekNumbers
        val len = days.size

        if (week_num == null)
            return

        week_num.setTextColor(mTextColor)
        week_num.beVisibleIf(displayWeekNumbers)

        for (i in 0..5) {
            (mHolder.findViewById(mRes.getIdentifier("week_num_$i", "id", packageName)) as TextView).apply {
                text = "${days[i * 7 + 3].weekOfYear}:"
                setTextColor(mTextColor)
                beVisibleIf(displayWeekNumbers)
            }
        }

        for (i in 0 until len) {
            (mHolder.findViewById(mRes.getIdentifier("day_$i", "id", packageName)) as LinearLayout).apply {
                val day = days[i]
                setOnClickListener { openDay(day.code) }

                removeAllViews()
                addDayNumber(day, this)
                addDayEvents(day, this)
            }
        }
    }

    private fun openDay(code: String) {
        if (code.isNotEmpty()) {
            Intent(context, DayActivity::class.java).apply {
                putExtra(DAY_CODE, code)
                startActivity(this)
            }
        }
    }

    private fun addDayNumber(day: DayMonthly, linearLayout: LinearLayout) {
        (View.inflate(context, R.layout.day_monthly_item_view, null) as TextView).apply {
            setTextColor(if (day.isThisMonth) mTextColor else mWeakTextColor)
            text = day.value.toString()
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            linearLayout.addView(this)

            if (day.isToday) {
                val primaryColor = context.config.primaryColor
                setTextColor(primaryColor.getContrastColor().adjustAlpha(MEDIUM_ALPHA))

                onGlobalLayout {
                    val height = this@apply.height
                    if (height > 0) {
                        val baseDrawable = mRes.getDrawable(R.drawable.monthly_today_circle)
                        val bitmap = (baseDrawable as BitmapDrawable).bitmap
                        val scaledDrawable = BitmapDrawable(mRes, Bitmap.createScaledBitmap(bitmap, height, height, true))
                        scaledDrawable.mutate().setColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
                        background = scaledDrawable
                    }
                }
            }
        }
    }

    private fun addDayEvents(day: DayMonthly, linearLayout: LinearLayout) {
        day.dayEvents.forEach {
            val backgroundDrawable = mRes.getDrawable(R.drawable.day_monthly_event_background)
            backgroundDrawable.mutate().setColorFilter(it.color, PorterDuff.Mode.SRC_IN)

            val eventLayoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            eventLayoutParams.setMargins(dividerMargin, dividerMargin, dividerMargin, dividerMargin)

            var textColor = it.color.getContrastColor().adjustAlpha(MEDIUM_ALPHA)
            if (!day.isThisMonth) {
                backgroundDrawable.alpha = 64
                textColor = textColor.adjustAlpha(0.25f)
            }

            (View.inflate(context, R.layout.day_monthly_item_view, null) as TextView).apply {
                setTextColor(textColor)
                text = it.title
                gravity = Gravity.START
                background = backgroundDrawable
                layoutParams = eventLayoutParams
                linearLayout.addView(this)
            }
        }
    }
}
