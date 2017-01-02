package com.simplemobiletools.calendar.adapters

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.util.SparseArray
import com.simplemobiletools.calendar.fragments.DayFragment
import com.simplemobiletools.calendar.helpers.DAY_CODE

class MyDayPagerAdapter(fm: FragmentManager, private val mCodes: List<String>, private val mListener: DayFragment.DeleteListener) :
        FragmentStatePagerAdapter(fm) {
    var fragments: SparseArray<DayFragment> = SparseArray(10)

    override fun getCount() = mCodes.size

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        val code = mCodes[position]
        bundle.putString(DAY_CODE, code)

        if (fragments.get(position) != null)
            return fragments[position]

        val fragment = DayFragment()
        fragment.arguments = bundle
        fragment.setListener(mListener)
        fragments.put(position, fragment)
        return fragment
    }

    fun deleteItems(pos: Int) {
        fragments.get(pos)?.deleteEvents()
    }

    fun undoDeletion(pos: Int) {
        fragments.get(pos)?.undoDeletion()
    }
}
