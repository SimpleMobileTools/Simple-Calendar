package com.simplemobiletools.calendar.fragments

import android.support.v4.app.Fragment

abstract class MyFragmentHolder : Fragment() {
    abstract fun goToToday()

    abstract fun refreshEvents()

    abstract fun shouldGoToTodayBeVisible(): Boolean

    abstract fun updateActionBarTitle()

    abstract fun getNewEventDayCode(): String
}
