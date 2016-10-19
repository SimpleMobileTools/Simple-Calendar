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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.year_fragment, container, false)

        mYear = arguments.getInt(Constants.YEAR_LABEL)

        val dateTime = DateTime().withYear(mYear).withDayOfMonth(1).withMonthOfYear(2).withHourOfDay(12)
        val days = dateTime.dayOfMonth().maximumValue
        view.month_2.setDays(days)

        val res = resources
        val sundayFirst = Config.newInstance(context).isSundayFirst
        for (i in 1..12) {
            val monthView = view.findViewById(res.getIdentifier("month_" + i, "id", activity.packageName)) as SmallMonthView
            var dayOfWeek = dateTime.withMonthOfYear(i).dayOfWeek().get()
            if (!sundayFirst)
                dayOfWeek--

            monthView.setFirstDay(dayOfWeek)
        }

        return view
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }
}
