package com.simplemobiletools.calendar.pro.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.adapters.MyWeekPagerAdapter
import com.simplemobiletools.calendar.pro.databinding.FragmentWeekHolderBinding
import com.simplemobiletools.calendar.pro.databinding.WeeklyViewHourTextviewBinding
import com.simplemobiletools.calendar.pro.extensions.*
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.WEEKLY_VIEW
import com.simplemobiletools.calendar.pro.helpers.WEEK_START_DATE_TIME
import com.simplemobiletools.calendar.pro.interfaces.WeekFragmentListener
import com.simplemobiletools.calendar.pro.views.MyScrollView
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.WEEK_SECONDS
import com.simplemobiletools.commons.views.MyViewPager
import org.joda.time.DateTime

class WeekFragmentsHolder : MyFragmentHolder(), WeekFragmentListener {
    private val PREFILLED_WEEKS = 151
    private val MAX_SEEKBAR_VALUE = 14

    private lateinit var binding: FragmentWeekHolderBinding
    private lateinit var viewPager: MyViewPager
    private var defaultWeeklyPage = 0
    private var thisWeekTS = 0L
    private var currentWeekTS = 0L
    private var isGoToTodayVisible = false
    private var weekScrollY = 0

    override val viewType = WEEKLY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateTimeString = arguments?.getString(WEEK_START_DATE_TIME) ?: return
        currentWeekTS = (DateTime.parse(dateTimeString) ?: DateTime()).seconds()
        thisWeekTS = DateTime.parse(requireContext().getFirstDayOfWeek(DateTime())).seconds()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val textColor = requireContext().getProperTextColor()
        binding = FragmentWeekHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        binding.weekViewMonthLabel.setTextColor(textColor)
        binding.weekViewWeekNumber.setTextColor(textColor)

        val itemHeight = requireContext().getWeeklyViewItemHeight().toInt()
        binding.weekViewHoursHolder.setPadding(0, 0, 0, itemHeight)

        viewPager = binding.weekViewViewPager
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        context?.config?.allowCustomizeDayCount?.let { allow ->
            binding.weekViewDaysCount.beVisibleIf(allow)
            binding.weekViewSeekbar.beVisibleIf(allow)
        }
        setupSeekbar()
    }

    private fun setupFragment() {
        addHours()
        setupWeeklyViewPager()

        binding.weekViewHoursScrollview.setOnTouchListener { _, _ -> true }

        binding.weekViewSeekbar.apply {
            progress = context?.config?.weeklyViewDays ?: 7
            max = MAX_SEEKBAR_VALUE

            onSeekBarChangeListener {
                if (it == 0) {
                    progress = 1
                }

                updateWeeklyViewDays(progress)
            }
        }

        setupWeeklyActionbarTitle(currentWeekTS)
    }

    private fun setupWeeklyViewPager() {
        val weekTSs = getWeekTimestamps(currentWeekTS)
        val weeklyAdapter = MyWeekPagerAdapter(requireActivity().supportFragmentManager, weekTSs, this)

        defaultWeeklyPage = weekTSs.size / 2

        viewPager.apply {
            adapter = weeklyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    currentWeekTS = weekTSs[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }

                    setupWeeklyActionbarTitle(weekTSs[position])
                }
            })
            currentItem = defaultWeeklyPage
        }

        binding.weekViewHoursScrollview.setOnScrollviewListener(object : MyScrollView.ScrollViewListener {
            override fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int) {
                weekScrollY = y
                weeklyAdapter.updateScrollY(viewPager.currentItem, y)
            }
        })
    }

    private fun addHours(textColor: Int = requireContext().getProperTextColor()) {
        val itemHeight = requireContext().getWeeklyViewItemHeight().toInt()
        binding.weekViewHoursHolder.removeAllViews()
        val hourDateTime = DateTime().withDate(2000, 1, 1).withTime(0, 0, 0, 0)
        for (i in 1..23) {
            val formattedHours = Formatter.getTime(requireContext(), hourDateTime.withHourOfDay(i))
            WeeklyViewHourTextviewBinding.inflate(layoutInflater).root.apply {
                text = formattedHours
                setTextColor(textColor)
                height = itemHeight
                binding.weekViewHoursHolder.addView(this)
            }
        }
    }

    private fun getWeekTimestamps(targetSeconds: Long): List<Long> {
        val weekTSs = ArrayList<Long>(PREFILLED_WEEKS)
        val dateTime = Formatter.getDateTimeFromTS(targetSeconds)
        val shownWeekDays = requireContext().config.weeklyViewDays
        var currentWeek = dateTime.minusDays(PREFILLED_WEEKS / 2 * shownWeekDays)
        for (i in 0 until PREFILLED_WEEKS) {
            weekTSs.add(currentWeek.seconds())
            currentWeek = currentWeek.plusDays(shownWeekDays)
        }
        return weekTSs
    }

    private fun setupWeeklyActionbarTitle(timestamp: Long) {
        val startDateTime = Formatter.getDateTimeFromTS(timestamp)
        val month = Formatter.getShortMonthName(requireContext(), startDateTime.monthOfYear)
        binding.weekViewMonthLabel.text = month
        val weekNumber = startDateTime.plusDays(3).weekOfWeekyear
        binding.weekViewWeekNumber.text = "${getString(com.simplemobiletools.commons.R.string.week_number_short)} $weekNumber"
    }

    override fun goToToday() {
        currentWeekTS = thisWeekTS
        setupFragment()
    }

    override fun showGoToDateDialog() {
        if (activity == null) {
            return
        }

        val datePicker = getDatePickerView()
        val dateTime = getCurrentDate()!!
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, dateTime.dayOfMonth, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
            .setPositiveButton(com.simplemobiletools.commons.R.string.ok) { _, _ -> dateSelected(dateTime, datePicker) }
            .apply {
                activity?.setupDialogStuff(datePicker, this)
            }
    }

    private fun dateSelected(dateTime: DateTime, datePicker: DatePicker) {
        val month = datePicker.month + 1
        val year = datePicker.year
        val day = datePicker.dayOfMonth
        val newDateTime = dateTime.withDate(year, month, day)
        currentWeekTS = requireContext().getFirstDayOfWeekDt(newDateTime).seconds()
        setupFragment()
    }

    private fun setupSeekbar() {
        if (context?.config?.allowCustomizeDayCount != true) {
            return
        }

        // avoid seekbar width changing if the days count changes to 1, 10 etc
        binding.weekViewDaysCount.onGlobalLayout {
            if (binding.weekViewSeekbar.width != 0) {
                binding.weekViewSeekbar.layoutParams.width = binding.weekViewSeekbar.width
            }
            (binding.weekViewSeekbar.layoutParams as RelativeLayout.LayoutParams).removeRule(RelativeLayout.START_OF)
        }

        updateDaysCount(context?.config?.weeklyViewDays ?: 7)
        binding.weekViewDaysCount.setTextColor(requireContext().getProperTextColor())
    }

    private fun updateWeeklyViewDays(days: Int) {
        requireContext().config.weeklyViewDays = days
        updateDaysCount(days)
        setupWeeklyViewPager()
    }

    private fun updateDaysCount(cnt: Int) {
        binding.weekViewDaysCount.text = requireContext().resources.getQuantityString(com.simplemobiletools.commons.R.plurals.days, cnt, cnt)
    }

    override fun refreshEvents() {
        (viewPager.adapter as? MyWeekPagerAdapter)?.updateCalendars(viewPager.currentItem)
    }

    override fun shouldGoToTodayBeVisible() = currentWeekTS != thisWeekTS

    override fun getNewEventDayCode(): String {
        val currentTS = System.currentTimeMillis() / 1000
        return if (currentTS > currentWeekTS && currentTS < currentWeekTS + WEEK_SECONDS) {
            Formatter.getTodayCode()
        } else {
            Formatter.getDayCodeFromTS(currentWeekTS)
        }
    }

    override fun scrollTo(y: Int) {
        binding.weekViewHoursScrollview.scrollY = y
        weekScrollY = y
    }

    override fun updateHoursTopMargin(margin: Int) {
        binding.apply {
            weekViewHoursDivider.layoutParams?.height = margin
            weekViewHoursScrollview.requestLayout()
            weekViewHoursScrollview.onGlobalLayout {
                weekViewHoursScrollview.scrollY = weekScrollY
            }
        }
    }

    override fun getCurrScrollY() = weekScrollY

    override fun updateRowHeight(rowHeight: Int) {
        val childCnt = binding.weekViewHoursHolder.childCount
        for (i in 0..childCnt) {
            val textView = binding.weekViewHoursHolder.getChildAt(i) as? TextView ?: continue
            textView.layoutParams.height = rowHeight
        }

        binding.weekViewHoursHolder.setPadding(0, 0, 0, rowHeight)
        (viewPager.adapter as? MyWeekPagerAdapter)?.updateNotVisibleScaleLevel(viewPager.currentItem)
    }

    override fun getFullFragmentHeight() =
        binding.weekViewHolder.height - binding.weekViewSeekbar.height - binding.weekViewDaysCountDivider.height

    override fun printView() {
        val lightTextColor = resources.getColor(com.simplemobiletools.commons.R.color.theme_light_text_color)
        binding.apply {
            weekViewDaysCountDivider.beGone()
            weekViewSeekbar.beGone()
            weekViewDaysCount.beGone()
            addHours(lightTextColor)
            weekViewWeekNumber.setTextColor(lightTextColor)
            weekViewMonthLabel.setTextColor(lightTextColor)
            root.background = ColorDrawable(Color.WHITE)
            (viewPager.adapter as? MyWeekPagerAdapter)?.togglePrintMode(viewPager.currentItem)

            Handler().postDelayed({
                requireContext().printBitmap(binding.weekViewHolder.getViewBitmap())

                Handler().postDelayed({
                    weekViewDaysCountDivider.beVisible()
                    weekViewSeekbar.beVisible()
                    weekViewDaysCount.beVisible()
                    weekViewWeekNumber.setTextColor(requireContext().getProperTextColor())
                    weekViewMonthLabel.setTextColor(requireContext().getProperTextColor())
                    addHours()
                    root.background = ColorDrawable(requireContext().getProperBackgroundColor())
                    (viewPager.adapter as? MyWeekPagerAdapter)?.togglePrintMode(viewPager.currentItem)
                }, 1000)
            }, 1000)
        }
    }

    override fun getCurrentDate(): DateTime? {
        return if (currentWeekTS != 0L) {
            Formatter.getDateTimeFromTS(currentWeekTS)
        } else {
            null
        }
    }
}
