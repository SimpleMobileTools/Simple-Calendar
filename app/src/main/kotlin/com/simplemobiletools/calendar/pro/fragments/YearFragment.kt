package com.simplemobiletools.calendar.pro.fragments

import android.content.res.Resources
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.helpers.YEAR_LABEL
import com.simplemobiletools.calendar.pro.helpers.YearlyCalendarImpl
import com.simplemobiletools.calendar.pro.interfaces.YearlyCalendar
import com.simplemobiletools.calendar.pro.models.DayYearly
import com.simplemobiletools.calendar.pro.views.SmallMonthView
import com.simplemobiletools.commons.extensions.getAdjustedPrimaryColor
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.fragment_year.view.*
import org.joda.time.DateTime
import java.util.*

class YearFragment : Fragment(), YearlyCalendar {
    private var mYear = 0
    private var mSundayFirst = false
    private var lastHash = 0
    private var mCalendar: YearlyCalendarImpl? = null

    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_year, container, false)
        mYear = arguments!!.getInt(YEAR_LABEL)
        requireContext().updateTextColors(mView.calendar_holder)
        setupMonths()

        mCalendar = YearlyCalendarImpl(this, requireContext(), mYear)

        return mView
    }

    override fun onPause() {
        super.onPause()
        mSundayFirst = requireContext().config.isSundayFirst
    }

    override fun onResume() {
        super.onResume()
        val sundayFirst = requireContext().config.isSundayFirst
        if (sundayFirst != mSundayFirst) {
            mSundayFirst = sundayFirst
            setupMonths()
        }
        updateCalendar()
    }

    fun updateCalendar() {
        mCalendar?.getEvents(mYear)
    }

    private fun setupMonths() {
        val dateTime = DateTime().withDate(mYear, 2, 1).withHourOfDay(12)
        val days = dateTime.dayOfMonth().maximumValue
        mView.month_2.setDays(days)

        val res = resources
        markCurrentMonth(res)

        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(res.getIdentifier("month_" + i, "id", requireContext().packageName))
            var dayOfWeek = dateTime.withMonthOfYear(i).dayOfWeek().get()
            if (!mSundayFirst) {
                dayOfWeek--
            }

            monthView.firstDay = dayOfWeek
            monthView.setOnClickListener {
                (activity as MainActivity).openMonthFromYearly(DateTime().withDate(mYear, i, 1))
            }
        }
    }

    private fun markCurrentMonth(res: Resources) {
        val now = DateTime()
        if (now.year == mYear) {
            val monthLabel = mView.findViewById<TextView>(res.getIdentifier("month_${now.monthOfYear}_label", "id", requireContext().packageName))
            monthLabel.setTextColor(requireContext().getAdjustedPrimaryColor())

            val monthView = mView.findViewById<SmallMonthView>(res.getIdentifier("month_${now.monthOfYear}", "id", requireContext().packageName))
            monthView.todaysId = now.dayOfMonth
        }
    }

    override fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int) {
        if (!isAdded)
            return

        if (hashCode == lastHash) {
            return
        }

        lastHash = hashCode
        val res = resources
        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(res.getIdentifier("month_$i", "id", requireContext().packageName))
            monthView.setEvents(events.get(i))
        }
    }
}
