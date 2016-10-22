package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.*
import com.simplemobiletools.calendar.views.SmallMonthView
import kotlinx.android.synthetic.main.year_fragment.view.*
import org.joda.time.DateTime

class YearFragment : Fragment(), YearlyCalendar {
    private var mListener: NavigationListener? = null
    private var mYear = 0
    private var mSundayFirst = false

    lateinit var mView: View
    lateinit var mCalendar: YearlyCalendarImpl

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater!!.inflate(R.layout.year_fragment, container, false)
        mYear = arguments.getInt(Constants.YEAR_LABEL)
        setupMonths()

        mCalendar = YearlyCalendarImpl(this, context)
        mCalendar.getEvents(mYear)

        return mView
    }

    override fun onResume() {
        super.onResume()
        val sundayFirst = Config.newInstance(context).isSundayFirst
        if (sundayFirst != mSundayFirst) {
            mSundayFirst = sundayFirst
            setupMonths()
        }
    }

    fun setupMonths() {
        val dateTime = DateTime().withDate(mYear, 2, 1).withHourOfDay(12)
        val days = dateTime.dayOfMonth().maximumValue
        mView.month_2.setDays(days)

        val res = resources
        for (i in 1..12) {
            val monthView = mView.findViewById(res.getIdentifier("month_" + i, "id", activity.packageName)) as SmallMonthView
            var dayOfWeek = dateTime.withMonthOfYear(i).dayOfWeek().get()
            if (!mSundayFirst)
                dayOfWeek--

            monthView.setFirstDay(dayOfWeek)
            monthView.setOnClickListener {
                mListener?.goToDateTime(DateTime().withDate(mYear, i, 1)
            }
        }
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }

    override fun updateYearlyCalendar(events: MutableList<String>) {

    }
}
