package com.simplemobiletools.calendar.adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.simplemobiletools.calendar.fragments.YearFragment
import com.simplemobiletools.calendar.helpers.YEAR_LABEL
import com.simplemobiletools.calendar.interfaces.NavigationListener

class MyYearPagerAdapter(fm: FragmentManager, private val mYears: List<Int>, private val mListener: NavigationListener) : FragmentStatePagerAdapter(fm) {

    override fun getCount() = mYears.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val year = mYears[position]
        bundle.putInt(YEAR_LABEL, year)

        val fragment = YearFragment()
        fragment.arguments = bundle
        fragment.setListener(mListener)
        return fragment
    }
}
