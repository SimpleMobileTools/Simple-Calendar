package com.simplemobiletools.calendar.pro.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.simplemobiletools.calendar.pro.fragments.MonthDayFragment
import com.simplemobiletools.calendar.pro.helpers.DAY_CODE
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener

class MyMonthDayPagerAdapter(fragment: Fragment, private val mCodes: List<String>, private val mListener: NavigationListener) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount() = mCodes.size

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        val code = mCodes[position]
        bundle.putString(DAY_CODE, code)

        val fragment = MonthDayFragment()
        fragment.arguments = bundle
        fragment.listener = mListener
        return fragment
    }

    fun updateCalendars(pos: Int, fragment: Fragment) {
        for (i in -1..1) {
            val f = fragment.childFragmentManager.findFragmentByTag("f${pos + i}") as? MonthDayFragment
            f?.updateCalendar()
        }
    }

    fun printCurrentView(pos: Int, fragment: Fragment) {
        val f = fragment.childFragmentManager.findFragmentByTag("f$pos") as? MonthDayFragment
        f?.printCurrentView()
    }

    fun getNewEventDayCode(pos: Int, fragment: Fragment): String? {
        val f = fragment.childFragmentManager.findFragmentByTag("f$pos") as? MonthDayFragment
        return f?.getNewEventDayCode()
    }
}
