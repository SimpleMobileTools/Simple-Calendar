package com.simplemobiletools.calendar.adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.util.SparseArray
import com.simplemobiletools.calendar.fragments.WeekFragment

class MyWeekPagerAdapter(fm: FragmentManager, private val mListener: WeekFragment.WeekScrollListener) : FragmentStatePagerAdapter(fm) {
    private val mFragments = SparseArray<WeekFragment>()

    override fun getCount() = 1

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val fragment = WeekFragment()
        fragment.arguments = bundle
        fragment.setListener(mListener)

        mFragments.put(position, fragment)
        return fragment
    }

    fun updateScrollY(pos: Int, y: Int) {
        (-1..1).map { mFragments[pos + it] }
                .forEach { it?.updateScrollY(y) }
    }
}
