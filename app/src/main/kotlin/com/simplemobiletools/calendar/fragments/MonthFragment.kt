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
import com.simplemobiletools.calendar.*
import com.simplemobiletools.calendar.activities.DayActivity
import com.simplemobiletools.calendar.models.Day
import kotlinx.android.synthetic.main.calendar_layout.view.*
import kotlinx.android.synthetic.main.top_navigation.view.*
import org.joda.time.DateTime

class MonthFragment : Fragment(), Calendar {
    private var mDayTextSize: Float = 0f
    private var mTodayTextSize: Float = 0f
    private var mPackageName: String = ""
    private var mTextColor: Int = 0
    private var mWeakTextColor: Int = 0
    private var mTextColorWithEvent: Int = 0
    private var mWeakTextColorWithEvent: Int = 0
    private var mSundayFirst: Boolean = false
    private var mCode: String = ""

    private var mCalendar: CalendarImpl? = null
    private var mListener: NavigationListener? = null

    lateinit var mRes: Resources
    lateinit var mHolder: RelativeLayout
    lateinit var mConfig: Config

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.calendar_layout, container, false)
        mRes = resources

        mHolder = view.calendar_holder
        mCode = arguments.getString(Constants.DAY_CODE)
        mConfig = Config.newInstance(context)
        mSundayFirst = mConfig.isSundayFirst

        setupColors()

        mPackageName = activity.packageName
        mDayTextSize = mRes.getDimension(R.dimen.day_text_size) / mRes.displayMetrics.density
        mTodayTextSize = mRes.getDimension(R.dimen.today_text_size) / mRes.displayMetrics.density
        setupLabels()

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mCalendar = CalendarImpl(this, context)
        mCalendar!!.updateCalendar(Formatter.getDateTimeFromCode(mCode))
    }

    override fun onResume() {
        super.onResume()
        if (mConfig.isSundayFirst != mSundayFirst) {
            mSundayFirst = mConfig.isSundayFirst
            setupLabels()
        }
    }

    override fun updateCalendar(month: String, days: List<Day>) {
        mHolder.month_value.text = month
        updateDays(days)
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }

    private fun setupColors() {
        val baseColor = if (mConfig.isDarkTheme) Color.WHITE else Color.BLACK
        mTextColor = Utils.adjustAlpha(baseColor, Constants.HIGH_ALPHA)
        mTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.HIGH_ALPHA)
        mWeakTextColor = Utils.adjustAlpha(baseColor, Constants.LOW_ALPHA)
        mWeakTextColorWithEvent = Utils.adjustAlpha(mRes.getColor(R.color.colorPrimary), Constants.LOW_ALPHA)
        mHolder.top_left_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
        mHolder.top_right_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
        mHolder.top_left_arrow.background = null
        mHolder.top_right_arrow.background = null

        mHolder.top_left_arrow.setOnClickListener {
            if (mListener != null)
                mListener!!.goLeft()
        }

        mHolder.top_right_arrow.setOnClickListener {
            if (mListener != null)
                mListener!!.goRight()
        }

        mHolder.month_value.setOnClickListener { showMonthDialog() }
    }

    fun showMonthDialog() {
        val theme = if (mConfig.isDarkTheme) R.style.MyAlertDialog_Dark else R.style.MyAlertDialog
        val alertDialog = AlertDialog.Builder(context, theme)
        val view = getLayoutInflater(arguments).inflate(R.layout.date_picker, null)
        val datePicker = view.findViewById(R.id.date_picker) as DatePicker
        hideDayPicker(datePicker)

        val dateTime = DateTime(mCalendar!!.targetDate.toString())
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        alertDialog.setView(view)
        alertDialog.setNegativeButton(R.string.cancel, null)
        alertDialog.setPositiveButton(R.string.ok) { dialog, id ->
            val month = datePicker.month + 1
            val year = datePicker.year
            if (mListener != null)
                mListener!!.goToDateTime(DateTime().withMonthOfYear(month).withYear(year))
        }

        alertDialog.show()
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
        val letters = Utils.getLetterIDs()

        for (i in 0..6) {
            val dayTV = mHolder.findViewById(mRes.getIdentifier("label_" + i, "id", mPackageName)) as TextView
            dayTV.textSize = mDayTextSize
            dayTV.setTextColor(mWeakTextColor)

            var index = i
            if (!mSundayFirst)
                index = (index + 1) % letters.size

            dayTV.text = getString(letters[index])
        }
    }

    private fun updateDays(days: List<Day>) {
        val len = days.size

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

        val intent = Intent(context, DayActivity::class.java)
        intent.putExtra(Constants.DAY_CODE, code)
        startActivity(intent)
    }

    interface NavigationListener {
        fun goLeft()

        fun goRight()

        fun goToDateTime(dateTime: DateTime)
    }
}
