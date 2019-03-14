package com.simplemobiletools.calendar.pro.fragments

import androidx.fragment.app.Fragment

abstract class MyFragmentHolder : Fragment() {
    abstract fun goToToday()

    abstract fun showGoToDateDialog()

    abstract fun refreshEvents()

    abstract fun shouldGoToTodayBeVisible(): Boolean

    abstract fun updateActionBarTitle()

    abstract fun getNewEventDayCode(): String
}
