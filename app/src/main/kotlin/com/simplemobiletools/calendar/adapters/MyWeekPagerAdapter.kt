package com.simplemobiletools.calendar.adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.simplemobiletools.calendar.fragments.WeekFragment

class MyWeekPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getCount() = 1

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val fragment = WeekFragment()
        fragment.arguments = bundle
        return fragment
    }
}
