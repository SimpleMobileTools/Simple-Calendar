package com.simplemobiletools.calendar.pro.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.EventListAdapter
import com.simplemobiletools.calendar.pro.databinding.WidgetConfigListBinding
import com.simplemobiletools.calendar.pro.dialogs.CustomPeriodPickerDialog
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.seconds
import com.simplemobiletools.calendar.pro.extensions.widgetsDB
import com.simplemobiletools.calendar.pro.helpers.EVENT_PERIOD_CUSTOM
import com.simplemobiletools.calendar.pro.helpers.EVENT_PERIOD_TODAY
import com.simplemobiletools.calendar.pro.helpers.Formatter
import com.simplemobiletools.calendar.pro.helpers.MyWidgetListProvider
import com.simplemobiletools.calendar.pro.models.ListEvent
import com.simplemobiletools.calendar.pro.models.ListItem
import com.simplemobiletools.calendar.pro.models.ListSectionDay
import com.simplemobiletools.calendar.pro.models.Widget
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import org.joda.time.DateTime
import java.util.TreeSet

class WidgetListConfigureActivity : SimpleActivity() {
    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColor = 0
    private var mSelectedPeriodOption = 0

    private val binding by viewBinding(WidgetConfigListBinding::inflate)

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(binding.root)
        initVariables()

        val isCustomizingColors = intent.extras?.getBoolean(IS_CUSTOMIZING_COLORS) ?: false
        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isCustomizingColors) {
            finish()
        }

        binding.apply {
            EventListAdapter(this@WidgetListConfigureActivity, getListItems(), false, null, configEventsList) {}.apply {
                updateTextColor(mTextColor)
                configEventsList.adapter = this
            }

            periodPickerHolder.background = ColorDrawable(getProperBackgroundColor())
            periodPickerValue.setOnClickListener { showPeriodSelector() }

            configSave.setOnClickListener { saveConfig() }
            configBgColor.setOnClickListener { pickBackgroundColor() }
            configTextColor.setOnClickListener { pickTextColor() }

            periodPickerHolder.beGoneIf(isCustomizingColors)

            val primaryColor = getProperPrimaryColor()
            configBgSeekbar.setColors(mTextColor, primaryColor, primaryColor)
        }

        updateSelectedPeriod(config.lastUsedEventSpan)
    }

    private fun initVariables() {
        mBgColor = config.widgetBgColor
        mBgAlpha = Color.alpha(mBgColor) / 255f

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        binding.configBgSeekbar.apply {
            progress = (mBgAlpha * 100).toInt()

            onSeekBarChangeListener { progress ->
                mBgAlpha = progress / 100f
                updateBackgroundColor()
            }
        }
        updateBackgroundColor()

        mTextColor = config.widgetTextColor
        if (mTextColor == resources.getColor(com.simplemobiletools.commons.R.color.default_widget_text_color) && config.isUsingSystemTheme) {
            mTextColor = resources.getColor(com.simplemobiletools.commons.R.color.you_primary_color, theme)
        }

        updateTextColor()
    }

    private fun saveConfig() {
        val widget = Widget(null, mWidgetId, mSelectedPeriodOption)
        ensureBackgroundThread {
            widgetsDB.insertOrUpdate(widget)
        }

        storeWidgetColors()
        requestWidgetUpdate()

        config.lastUsedEventSpan = mSelectedPeriodOption

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun showPeriodSelector() {
        hideKeyboard()
        val seconds = TreeSet<Int>()
        seconds.apply {
            add(EVENT_PERIOD_TODAY)
            add(WEEK_SECONDS)
            add(MONTH_SECONDS)
            add(YEAR_SECONDS)
            add(mSelectedPeriodOption)
        }

        val items = ArrayList<RadioItem>(seconds.size)
        seconds.mapIndexedTo(items) { index, value ->
            RadioItem(index, getFormattedSeconds(value), value)
        }

        var selectedIndex = 0
        seconds.forEachIndexed { index, value ->
            if (value == mSelectedPeriodOption) {
                selectedIndex = index
            }
        }

        items.add(RadioItem(EVENT_PERIOD_CUSTOM, getString(R.string.within_the_next)))

        RadioGroupDialog(this, items, selectedIndex, showOKButton = true, cancelCallback = null) {
            val option = it as Int
            if (option == EVENT_PERIOD_CUSTOM) {
                CustomPeriodPickerDialog(this) {
                    updateSelectedPeriod(it)
                }
            } else {
                updateSelectedPeriod(option)
            }
        }
    }

    private fun updateSelectedPeriod(selectedPeriod: Int) {
        mSelectedPeriodOption = selectedPeriod
        when (selectedPeriod) {
            0 -> {
                mSelectedPeriodOption = YEAR_SECONDS
                binding.periodPickerValue.setText(R.string.within_the_next_one_year)
            }

            EVENT_PERIOD_TODAY -> binding.periodPickerValue.setText(R.string.today_only)
            else -> binding.periodPickerValue.text = getFormattedSeconds(mSelectedPeriodOption)
        }
    }

    private fun getFormattedSeconds(seconds: Int): String = if (seconds == EVENT_PERIOD_TODAY) {
        getString(R.string.today_only)
    } else {
        when {
            seconds == YEAR_SECONDS -> getString(R.string.within_the_next_one_year)
            seconds % MONTH_SECONDS == 0 -> resources.getQuantityString(R.plurals.within_the_next_months, seconds / MONTH_SECONDS, seconds / MONTH_SECONDS)
            seconds % WEEK_SECONDS == 0 -> resources.getQuantityString(R.plurals.within_the_next_weeks, seconds / WEEK_SECONDS, seconds / WEEK_SECONDS)
            else -> resources.getQuantityString(R.plurals.within_the_next_days, seconds / DAY_SECONDS, seconds / DAY_SECONDS)
        }
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
            widgetTextColor = mTextColor
        }
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mBgColorWithoutTransparency = color
                updateBackgroundColor()
            }
        }
    }

    private fun pickTextColor() {
        ColorPickerDialog(this, mTextColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mTextColor = color
                updateTextColor()
            }
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetListProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateTextColor() {
        (binding.configEventsList.adapter as? EventListAdapter)?.updateTextColor(mTextColor)
        binding.configTextColor.setFillWithStroke(mTextColor, mTextColor)
        binding.configSave.setTextColor(getProperPrimaryColor().getContrastColor())
    }

    private fun updateBackgroundColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        binding.configEventsList.background.applyColorFilter(mBgColor)
        binding.configBgColor.setFillWithStroke(mBgColor, mBgColor)
        binding.configSave.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
    }

    private fun getListItems(): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>(10)
        var dateTime = DateTime.now().withTime(0, 0, 0, 0).plusDays(1)
        var code = Formatter.getDayCodeFromTS(dateTime.seconds())
        var day = Formatter.getDateDayTitle(code)
        listItems.add(ListSectionDay(day, code, false, false))

        var time = dateTime.withHourOfDay(7)
        listItems.add(
            ListEvent.empty.copy(
                id = 1,
                startTS = time.seconds(),
                endTS = time.plusMinutes(30).seconds(),
                title = getString(R.string.sample_title_1),
                description = getString(R.string.sample_description_1),
                color = getProperPrimaryColor(),
            )
        )
        time = dateTime.withHourOfDay(8)
        listItems.add(
            ListEvent.empty.copy(
                id = 2,
                startTS = time.seconds(),
                endTS = time.plusHours(1).seconds(),
                title = getString(R.string.sample_title_2),
                description = getString(R.string.sample_description_2),
                color = getProperPrimaryColor(),
            )
        )

        dateTime = dateTime.plusDays(1)
        code = Formatter.getDayCodeFromTS(dateTime.seconds())
        day = Formatter.getDateDayTitle(code)
        listItems.add(ListSectionDay(day, code, false, false))

        time = dateTime.withHourOfDay(8)
        listItems.add(
            ListEvent.empty.copy(
                id = 3,
                startTS = time.seconds(),
                endTS = time.plusHours(1).seconds(),
                title = getString(R.string.sample_title_3),
                description = "",
                color = getProperPrimaryColor(),
            )
        )
        time = dateTime.withHourOfDay(13)
        listItems.add(
            ListEvent.empty.copy(
                id = 4,
                startTS = time.seconds(),
                endTS = time.plusHours(1).seconds(),
                title = getString(R.string.sample_title_4),
                description = getString(R.string.sample_description_4),
                color = getProperPrimaryColor(),
            )
        )
        time = dateTime.withHourOfDay(18)
        listItems.add(
            ListEvent.empty.copy(
                id = 5,
                startTS = time.seconds(),
                endTS = time.plusMinutes(10).seconds(),
                title = getString(R.string.sample_title_5),
                description = "",
                color = getProperPrimaryColor(),
            )
        )

        return listItems
    }
}
