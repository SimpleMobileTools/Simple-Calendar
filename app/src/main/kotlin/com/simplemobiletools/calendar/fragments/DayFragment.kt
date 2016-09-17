package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.Formatter
import com.simplemobiletools.calendar.NavigationListener
import com.simplemobiletools.calendar.R
import kotlinx.android.synthetic.main.top_navigation.view.*

class DayFragment : Fragment() {
    private var mCode: String = ""

    private var mListener: NavigationListener? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.day_fragment, container, false)
        mCode = arguments.getString(Constants.DAY_CODE)

        val day = Formatter.getEventDate(activity.applicationContext, mCode)
        view.month_value.text = day

        return view
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }
}
