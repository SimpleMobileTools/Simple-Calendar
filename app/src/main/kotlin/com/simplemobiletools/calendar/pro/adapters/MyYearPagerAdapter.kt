package com.simplemobiletools.calendar.pro.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.simplemobiletools.calendar.pro.fragments.YearFragment
import com.simplemobiletools.calendar.pro.helpers.YEAR_LABEL
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener

class MyYearPagerAdapter(fragment: Fragment, val mYears: List<Int>, private val mListener: NavigationListener) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount() = mYears.size

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        val year = mYears[position]
        bundle.putInt(YEAR_LABEL, year)

        val fragment = YearFragment()
        fragment.arguments = bundle
        fragment.listener = mListener
        return fragment
    }

    fun updateCalendars(pos: Int, fragment: Fragment) {
        for (i in -1..1) {
            val f = fragment.childFragmentManager.findFragmentByTag("f${pos + i}") as? YearFragment
            f?.updateCalendar()
        }
    }

    fun printCurrentView(pos: Int, fragment: Fragment) {
        val f = fragment.childFragmentManager.findFragmentByTag("f$pos") as? YearFragment
        f?.printCurrentView()
    }
}
