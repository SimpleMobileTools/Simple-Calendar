package com.simplemobiletools.calendar.pro.adapters

import android.os.Bundle
import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.simplemobiletools.calendar.pro.fragments.DayFragment
import com.simplemobiletools.calendar.pro.helpers.ANNIVERSARIES_COUNTER
import com.simplemobiletools.calendar.pro.helpers.BIRTHDAY_COUNTER
import com.simplemobiletools.calendar.pro.helpers.DAY_CODE
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener

class MyDayPagerAdapter(fm: FragmentManager, private val mCodes: List<String>, private val mListener: NavigationListener) :
        FragmentStatePagerAdapter(fm) {
    private val mFragments = SparseArray<DayFragment>()
    var ageCounter  = HashMap<Long?,Long>()
    var anniversariesCounter = HashMap<Long?,Long>()

    override fun getCount() = mCodes.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val code = mCodes[position]
        bundle.putString(DAY_CODE, code)
        bundle.putSerializable(BIRTHDAY_COUNTER,ageCounter)
        bundle.putSerializable(ANNIVERSARIES_COUNTER,anniversariesCounter)

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

    fun printCurrentView(pos: Int) {
        mFragments[pos].printCurrentView()
    }
}
