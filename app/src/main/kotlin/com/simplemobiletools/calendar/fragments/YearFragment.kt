package com.simplemobiletools.calendar.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.calendar.Constants
import com.simplemobiletools.calendar.NavigationListener
import com.simplemobiletools.calendar.R

class YearFragment : Fragment() {
    private var mListener: NavigationListener? = null
    private var mYear = 0

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.year_fragment, container, false)

        mYear = arguments.getInt(Constants.YEAR_LABEL)

        return view
    }

    fun setListener(listener: NavigationListener) {
        mListener = listener
    }
}
