package com.simplemobiletools.calendar.fragments

import android.content.res.Resources
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.YEAR_LABEL
import com.simplemobiletools.calendar.helpers.YearlyCalendarImpl
import com.simplemobiletools.calendar.interfaces.NavigationListener
import com.simplemobiletools.calendar.interfaces.YearlyCalendar
import com.simplemobiletools.calendar.models.DayYearly
import com.simplemobiletools.calendar.views.SmallMonthView
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.fragment_year.view.*
import org.joda.time.DateTime
import java.util.*

class YearFragment : Fragment(), YearlyCalendar {
    var mListener: NavigationListener? = null
    private var mYear = 0
    private var mSundayFirst = false
    private var lastHash = 0

    lateinit var mView: View
    lateinit var mCalendar: YearlyCalendarImpl

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater!!.inflate(R.layout.fragment_year, container, false)
        mYear = arguments.getInt(YEAR_LABEL)
        context.updateTextColors(mView.calendar_holder)
        setupMonths()

        mCalendar = YearlyCalendarImpl(this, context, mYear)

        return mView
    }

    override fun onResume() {
        super.onResume()
        val sundayFirst = context.config.isSundayFirst
        if (sundayFirst != mSundayFirst) {
            mSundayFirst = sundayFirst
            setupMonths()
        }
        updateEvents()
    }

    fun updateEvents() {
        mCalendar.getEvents(mYear)
    }

    fun setupMonths() {
        val dateTime = DateTime().withDate(mYear, 2, 1).withHourOfDay(12)
        val days = dateTime.dayOfMonth().maximumValue
        mView.month_2.setDays(days)

        val res = resources
        markCurrentMonth(res)

        for (i in 1..12) {
            val monthView = mView.findViewById(res.getIdentifier("month_" + i, "id", activity.packageName)) as SmallMonthView
            var dayOfWeek = dateTime.withMonthOfYear(i).dayOfWeek().get()
            if (!mSundayFirst)
                dayOfWeek--

            monthView.firstDay = dayOfWeek
            monthView.setOnClickListener {
                mListener?.goToDateTime(DateTime().withDate(mYear, i, 1))
            }
        }
    }

    private fun markCurrentMonth(res: Resources) {
        val now = DateTime()
        if (now.year == mYear) {
            val monthLabel = mView.findViewById(res.getIdentifier("month_${now.monthOfYear}_label", "id", activity.packageName)) as TextView
            monthLabel.setTextColor(context.config.primaryColor)

            val monthView = mView.findViewById(res.getIdentifier("month_${now.monthOfYear}", "id", activity.packageName)) as SmallMonthView
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
            val monthView = mView.findViewById(res.getIdentifier("month_$i", "id", context.packageName)) as SmallMonthView
            monthView.setEvents(events.get(i))
        }
    }
}
