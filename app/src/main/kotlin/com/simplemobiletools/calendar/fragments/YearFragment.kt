package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.NavigationListener
import com.simplemobiletools.calendar.R
import kotlinx.android.synthetic.main.year_fragment.view.*
import org.joda.time.DateTime

class YearFragment : Fragment() {
    private var mListener: NavigationListener? = null
    private var mYear = 0

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.year_fragment, container, false)

        mYear = arguments.getInt(Constants.YEAR_LABEL)

        val days = DateTime().withYear(mYear).withDayOfMonth(1).withMonthOfYear(2).dayOfMonth().maximumValue
        view.february_value.setDays(days)

        return view
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }
}
