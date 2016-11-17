package com.simplemobiletools.calendar.fragments

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.simplemobiletools.calendar.MonthlyCalendarImpl
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.DayActivity
import com.simplemobiletools.calendar.extensions.adjustAlpha
import com.simplemobiletools.calendar.extensions.beVisibleIf
import com.simplemobiletools.calendar.helpers.*
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.interfaces.NavigationListener
import com.simplemobiletools.calendar.models.Day
import kotlinx.android.synthetic.main.first_row.*
import kotlinx.android.synthetic.main.month_fragment.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import org.joda.time.DateTime

class MonthFragment : Fragment(), MonthlyCalendar {
    private var mDayTextSize = 0f
    private var mTodayTextSize = 0f
    private var mPackageName = ""
    private var mTextColor = 0
    private var mWeakTextColor = 0
    private var mTextColorWithEvent = 0
    private var mWeakTextColorWithEvent = 0
    private var mSundayFirst = false
    private var mDayCode = ""

    private var mListener: NavigationListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    lateinit var mConfig: Config
    lateinit var mCalendar: MonthlyCalendarImpl

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.month_fragment, container, false)
        mRes = resources

        mHolder = view.calendar_holder
        mDayCode = arguments.getString(DAY_CODE)
        mConfig = Config.newInstance(context)
        mSundayFirst = mConfig.isSundayFirst

        setupButtons()

        mPackageName = activity.packageName
        mDayTextSize = mRes.getDimension(R.dimen.day_text_size) / mRes.displayMetrics.density
        mTodayTextSize = mRes.getDimension(R.dimen.today_text_size) / mRes.displayMetrics.density
        setupLabels()
        mCalendar = MonthlyCalendarImpl(this, context)

        val padding = resources.getDimension(R.dimen.activity_margin).toInt()
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
            updateMonthlyCalendar(Formatter.getDateTimeFromCode(mDayCode))
        }
    }

    override fun updateMonthlyCalendar(month: String, days: List<Day>) {
        activity?.runOnUiThread {
            mHolder.top_value.text = month
            updateDays(days)
        }
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }

    private fun setupButtons() {
        val baseColor = if (mConfig.isDarkTheme) Color.WHITE else Color.BLACK
        mTextColor = baseColor.adjustAlpha(HIGH_ALPHA)
        mTextColorWithEvent = mRes.getColor(R.color.colorPrimary).adjustAlpha(HIGH_ALPHA)
        mWeakTextColor = baseColor.adjustAlpha(LOW_ALPHA)
        mWeakTextColorWithEvent = mRes.getColor(R.color.colorPrimary).adjustAlpha(LOW_ALPHA)

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
        val theme = if (mConfig.isDarkTheme) R.style.MyAlertDialog_Dark else R.style.MyAlertDialog
        val alertDialog = AlertDialog.Builder(context, theme)
        val view = getLayoutInflater(arguments).inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById(R.id.date_picker) as DatePicker
        hideDayPicker(datePicker)

        val dateTime = DateTime(mCalendar.mTargetDate.toString())
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        alertDialog.apply {
            setView(view)
            setNegativeButton(R.string.cancel, null)
            setPositiveButton(R.string.ok) { dialog, id ->
                val month = datePicker.month + 1
                val year = datePicker.year
                val newDateTime = dateTime.withDate(year, month, 1)
                mListener?.goToDateTime(newDateTime)
            }

            show()
        }
    }

    private fun hideDayPicker(datePicker: DatePicker) {
        val ll = datePicker.getChildAt(0) as LinearLayout
        val ll2 = ll.getChildAt(0) as LinearLayout
        val picker1 = ll2.getChildAt(0) as NumberPicker
        val picker2 = ll2.getChildAt(1) as NumberPicker
        val dayPicker = if (picker1.maxValue > picker2.maxValue) picker1 else picker2
        dayPicker.visibility = View.GONE
    }

    private fun setupLabels() {
        val letters = letterIDs

        for (i in 0..6) {
            val dayTV = mHolder.findViewById(mRes.getIdentifier("label_" + i, "id", mPackageName)) as TextView
            dayTV.textSize = mDayTextSize
            dayTV.setTextColor(mTextColor)

            var index = i
            if (!mSundayFirst)
                index = (index + 1) % letters.size

            dayTV.text = getString(letters[index])
        }
    }

    private fun updateDays(days: List<Day>) {
        val displayWeekNumbers = mConfig.displayWeekNumbers
        val len = days.size

        if (week_num == null)
            return

        week_num.setTextColor(mWeakTextColor)
        week_num.beVisibleIf(displayWeekNumbers)

        for (i in 0..5) {
            val weekIdTV = mHolder.findViewById(mRes.getIdentifier("week_num_" + i, "id", mPackageName)) as TextView
            weekIdTV.text = "${days[i * 7].weekOfYear}:"
            weekIdTV.setTextColor(mWeakTextColor)
            weekIdTV.beVisibleIf(displayWeekNumbers)
        }

        for (i in 0..len - 1) {
            val day = days[i]
            val dayTV = mHolder.findViewById(mRes.getIdentifier("day_" + i, "id", mPackageName)) as TextView

            var curTextColor = if (day.hasEvent) mWeakTextColorWithEvent else mWeakTextColor
            var curTextSize = mDayTextSize

            if (day.isThisMonth) {
                curTextColor = if (day.hasEvent) mTextColorWithEvent else mTextColor
            }

            if (day.isToday) {
                curTextSize = mTodayTextSize
            }

            dayTV.text = day.value.toString()
            dayTV.setTextColor(curTextColor)
            dayTV.textSize = curTextSize

            dayTV.setOnClickListener { openDay(day.code) }
        }
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
