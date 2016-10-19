package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.Config
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.NavigationListener
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.views.SmallMonthView
import kotlinx.android.synthetic.main.year_fragment.view.*
import org.joda.time.DateTime

class YearFragment : Fragment() {
    private var mListener: NavigationListener? = null
    private var mYear = 0
    private var mSundayFirst = false
    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater!!.inflate(R.layout.year_fragment, container, false)
        mYear = arguments.getInt(Constants.YEAR_LABEL)
        setupMonths()
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
        val dateTime = DateTime().withYear(mYear).withDayOfMonth(1).withMonthOfYear(2).withHourOfDay(12)
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
                mListener?.goToDateTime(DateTime().withDayOfMonth(1).withMonthOfYear(i).withYear(mYear), true)
            }
        }
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }
}
