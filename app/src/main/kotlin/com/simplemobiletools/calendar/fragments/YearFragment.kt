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
import java.util.*

class YearFragment : Fragment() {
    private var mListener: NavigationListener? = null
    private var mYear = 0

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.year_fragment, container, false)

        mYear = arguments.getInt(Constants.YEAR_LABEL)

        val calendar = GregorianCalendar(mYear, Calendar.FEBRUARY, 1)
        view.february_value.setDays(calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

        return view
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }
}
