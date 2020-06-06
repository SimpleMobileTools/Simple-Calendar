package com.simplemobiletools.calendar.pro.adapters

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.simplemobiletools.calendar.pro.fragments.DayFragment
import com.simplemobiletools.calendar.pro.helpers.DAY_CODE
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener

class MyDayPagerAdapter(fm: FragmentManager, private val mCodes: List<String>, private val mListener: NavigationListener) :
        FragmentStatePagerAdapter(fm) {
    private val mFragments = SparseArray<DayFragment>()

    override fun getCount() = mCodes.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val code = mCodes[position]
        bundle.putString(DAY_CODE, code)

        val fragment = DayFragment()
        fragment.arguments = bundle
        fragment.mListener = mListener

        mFragments.put(position, fragment)
        return fragment
    }

    fun updateCalendars(pos: Int) {
        for (i in -1..1) {
            mFragments[pos + i]?.updateCalendar()
        }
    }
}
