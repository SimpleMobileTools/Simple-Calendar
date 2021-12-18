package com.simplemobiletools.calendar.pro.fragments

import androidx.fragment.app.Fragment
import org.joda.time.DateTime

abstract class MyFragmentHolder : Fragment() {
    abstract val viewType: Int

    abstract fun goToToday()

    abstract fun showGoToDateDialog()

    abstract fun refreshEvents()

    abstract fun shouldGoToTodayBeVisible(): Boolean

    abstract fun updateActionBarTitle()

    abstract fun getNewEventDayCode(): String

    abstract fun printView()

    abstract fun getCurrentDate(): DateTime?
}
