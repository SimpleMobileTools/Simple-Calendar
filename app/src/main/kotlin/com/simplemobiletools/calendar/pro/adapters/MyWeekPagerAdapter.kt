package com.simplemobiletools.calendar.pro.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.simplemobiletools.calendar.pro.fragments.WeekFragment
import com.simplemobiletools.calendar.pro.helpers.WEEK_START_TIMESTAMP
import com.simplemobiletools.calendar.pro.interfaces.WeekFragmentListener

class MyWeekPagerAdapter(fragment: Fragment, private val mWeekTimestamps: List<Long>, private val mListener: WeekFragmentListener) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount() = mWeekTimestamps.size

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        val weekTimestamp = mWeekTimestamps[position]
        bundle.putLong(WEEK_START_TIMESTAMP, weekTimestamp)

        val fragment = WeekFragment()
        fragment.arguments = bundle
        fragment.listener = mListener
        return fragment
    }

    fun updateScrollY(pos: Int, y: Int, fragment: Fragment) {
        for (i in -1..1) {
            if (i == 0) continue
            val f = fragment.childFragmentManager.findFragmentByTag("f${pos + i}") as? WeekFragment
            f?.updateScrollY(y)
        }
    }

    fun updateCalendars(pos: Int, fragment: Fragment) {
        for (i in -1..1) {
            val f = fragment.childFragmentManager.findFragmentByTag("f${pos + i}") as? WeekFragment
            f?.updateCalendar()
        }
    }

    fun updateNotVisibleScaleLevel(pos: Int, fragment: Fragment) {
        for (i in -1..1) {
            if (i == 0) continue
            val f = fragment.childFragmentManager.findFragmentByTag("f${pos + i}") as? WeekFragment
            f?.updateNotVisibleViewScaleLevel()
        }
    }

    fun togglePrintMode(pos: Int, fragment: Fragment) {
        val f = fragment.childFragmentManager.findFragmentByTag("f$pos") as? WeekFragment
        f?.togglePrintMode()
    }
}
