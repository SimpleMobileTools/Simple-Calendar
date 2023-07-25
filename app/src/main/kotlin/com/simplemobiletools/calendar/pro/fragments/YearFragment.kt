package com.simplemobiletools.calendar.pro.fragments

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
import com.simplemobiletools.calendar.pro.extensions.getProperDayIndexInWeek
import com.simplemobiletools.calendar.pro.extensions.getViewBitmap
import com.simplemobiletools.calendar.pro.extensions.printBitmap
import com.simplemobiletools.calendar.pro.helpers.YEAR_LABEL
import com.simplemobiletools.calendar.pro.helpers.YearlyCalendarImpl
import com.simplemobiletools.calendar.pro.interfaces.NavigationListener
import com.simplemobiletools.calendar.pro.interfaces.YearlyCalendar
import com.simplemobiletools.calendar.pro.models.DayYearly
import com.simplemobiletools.calendar.pro.views.SmallMonthView
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.getProperPrimaryColor
import com.simplemobiletools.commons.extensions.getProperTextColor
import com.simplemobiletools.commons.extensions.updateTextColors
import kotlinx.android.synthetic.main.fragment_year.view.calendar_wrapper
import kotlinx.android.synthetic.main.fragment_year.view.month_2
import kotlinx.android.synthetic.main.top_navigation.view.top_left_arrow
import kotlinx.android.synthetic.main.top_navigation.view.top_right_arrow
import kotlinx.android.synthetic.main.top_navigation.view.top_value
import org.joda.time.DateTime

class YearFragment : Fragment(), YearlyCalendar {
    private var mYear = 0
    private var mFirstDayOfWeek = 0
    private var isPrintVersion = false
    private var lastHash = 0
    private var mCalendar: YearlyCalendarImpl? = null

    var listener: NavigationListener? = null

    lateinit var mView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_year, container, false)
        mYear = requireArguments().getInt(YEAR_LABEL)
        requireContext().updateTextColors(mView.calendar_wrapper)
        setupMonths()
        setupButtons()

        mCalendar = YearlyCalendarImpl(this, requireContext(), mYear)
        return mView
    }

    override fun onPause() {
        super.onPause()
        mFirstDayOfWeek = requireContext().config.firstDayOfWeek
    }

    override fun onResume() {
        super.onResume()
        val firstDayOfWeek = requireContext().config.firstDayOfWeek
        if (firstDayOfWeek != mFirstDayOfWeek) {
            mFirstDayOfWeek = firstDayOfWeek
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

        val now = DateTime()

        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            val dayOfWeek = requireContext().getProperDayIndexInWeek(dateTime.withMonthOfYear(i))
            val monthLabel = mView.findViewById<TextView>(resources.getIdentifier("month_${i}_label", "id", requireContext().packageName))
            val curTextColor = when {
                isPrintVersion -> resources.getColor(R.color.theme_light_text_color)
                else -> requireContext().getProperTextColor()
            }

            monthLabel.setTextColor(curTextColor)

            monthView.firstDay = dayOfWeek
            monthView.setOnClickListener {
                (activity as MainActivity).openMonthFromYearly(DateTime().withDate(mYear, i, 1))
            }
        }

        if (!isPrintVersion) {
            markCurrentMonth(now)
        }
    }

    private fun setupButtons() {
        val textColor = requireContext().getProperTextColor()
        mView.top_left_arrow.apply {
            applyColorFilter(textColor)
            background = null
            setOnClickListener {
                listener?.goLeft()
            }

            val pointerLeft = requireContext().getDrawable(R.drawable.ic_chevron_left_vector)
            pointerLeft?.isAutoMirrored = true
            setImageDrawable(pointerLeft)
        }

        mView.top_right_arrow.apply {
            applyColorFilter(textColor)
            background = null
            setOnClickListener {
                listener?.goRight()
            }

            val pointerRight = requireContext().getDrawable(R.drawable.ic_chevron_right_vector)
            pointerRight?.isAutoMirrored = true
            setImageDrawable(pointerRight)
        }

        mView.top_value.apply {
            setTextColor(requireContext().getProperTextColor())
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
        }
    }

    private fun markCurrentMonth(now: DateTime) {
        if (now.year == mYear) {
            val monthLabel = mView.findViewById<TextView>(resources.getIdentifier("month_${now.monthOfYear}_label", "id", requireContext().packageName))
            monthLabel.setTextColor(requireContext().getProperPrimaryColor())

            val monthView = mView.findViewById<SmallMonthView>(resources.getIdentifier("month_${now.monthOfYear}", "id", requireContext().packageName))
            monthView.todaysId = now.dayOfMonth
        }
    }

    override fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int) {
        if (!isAdded) {
            return
        }

        if (hashCode == lastHash) {
            return
        }

        lastHash = hashCode
        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            monthView.setEvents(events.get(i))
        }

        mView.top_value.post {
            mView.top_value.text = mYear.toString()
        }
    }

    fun printCurrentView() {
        isPrintVersion = true
        setupMonths()
        toggleSmallMonthPrintModes()

        requireContext().printBitmap(mView.calendar_wrapper.getViewBitmap())

        isPrintVersion = false
        setupMonths()
        toggleSmallMonthPrintModes()
    }

    private fun toggleSmallMonthPrintModes() {
        for (i in 1..12) {
            val monthView = mView.findViewById<SmallMonthView>(resources.getIdentifier("month_$i", "id", requireContext().packageName))
            monthView.togglePrintMode()
        }
    }
}
