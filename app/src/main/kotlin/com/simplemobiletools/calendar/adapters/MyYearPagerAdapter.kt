package com.simplemobiletools.calendar.adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.simplemobiletools.calendar.fragments.YearFragment
import com.simplemobiletools.calendar.helpers.YEAR_LABEL

class MyYearPagerAdapter(fm: FragmentManager, val mYears: List<Int>) : FragmentStatePagerAdapter(fm) {
    override fun getCount() = mYears.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val year = mYears[position]
        bundle.putInt(YEAR_LABEL, year)

        val fragment = YearFragment()
        fragment.arguments = bundle

        return fragment
    }
}
