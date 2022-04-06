package com.simplemobiletools.calendar.pro.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.SeekBar
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.adapters.EventListAdapter
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
import kotlinx.android.synthetic.main.widget_config_list.*
import org.joda.time.DateTime
import java.util.*

class WidgetListConfigureActivity : SimpleActivity() {
    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColorWithoutTransparency = 0
    private var mTextColor = 0
    private var selectedPeriodOption = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.widget_config_list)
        initVariables()

        val isCustomizingColors = intent.extras?.getBoolean(IS_CUSTOMIZING_COLORS) ?: false
        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isCustomizingColors) {
            finish()
        }

        EventListAdapter(this, getListItems(), false, null, config_events_list) {}.apply {
            updateTextColor(mTextColor)
            config_events_list.adapter = this
        }

        period_picker_holder.background = ColorDrawable(getProperBackgroundColor())
        period_picker_value.setOnClickListener { showPeriodSelector() }

        config_save.setOnClickListener { saveConfig() }
        config_bg_color.setOnClickListener { pickBackgroundColor() }
        config_text_color.setOnClickListener { pickTextColor() }

        period_picker_holder.beGoneIf(isCustomizingColors)

        val primaryColor = getProperPrimaryColor()
        config_bg_seekbar.setColors(mTextColor, primaryColor, primaryColor)

        updateSelectedPeriod(config.lastUsedEventSpan)
    }

    override fun onResume() {
        super.onResume()
        window.decorView.setBackgroundColor(0)
        updateTextColors(config_list_holder)
    }

    private fun initVariables() {
        mTextColorWithoutTransparency = config.widgetTextColor
        updateColors()

        mBgColor = config.widgetBgColor
        mBgAlpha = Color.alpha(mBgColor) / 255.toFloat()

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        config_bg_seekbar.setOnSeekBarChangeListener(bgSeekbarChangeListener)
        config_bg_seekbar.progress = (mBgAlpha * 100).toInt()
        updateBgColor()
    }

    private fun saveConfig() {
        val widget = Widget(null, mWidgetId, selectedPeriodOption)
        ensureBackgroundThread {
            widgetsDB.insertOrUpdate(widget)
        }

        storeWidgetColors()
        requestWidgetUpdate()

        config.lastUsedEventSpan = selectedPeriodOption

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
            add(selectedPeriodOption)
        }

        val items = ArrayList<RadioItem>(seconds.size)
        seconds.mapIndexedTo(items) { index, value ->
            RadioItem(index, getFormattedSeconds(value), value)
        }

        var selectedIndex = 0
        seconds.forEachIndexed { index, value ->
            if (value == selectedPeriodOption) {
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
        selectedPeriodOption = selectedPeriod
        when (selectedPeriod) {
            0 -> {
                selectedPeriodOption = YEAR_SECONDS
                period_picker_value.setText(R.string.within_the_next_one_year)
            }
            EVENT_PERIOD_TODAY -> period_picker_value.setText(R.string.today_only)
            else -> period_picker_value.text = getFormattedSeconds(selectedPeriodOption)
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
            widgetTextColor = mTextColorWithoutTransparency
        }
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mBgColorWithoutTransparency = color
                updateBgColor()
            }
        }
    }

    private fun pickTextColor() {
        ColorPickerDialog(this, mTextColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mTextColorWithoutTransparency = color
                updateColors()
            }
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetListProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateColors() {
        mTextColor = mTextColorWithoutTransparency
        (config_events_list.adapter as? EventListAdapter)?.updateTextColor(mTextColor)
        config_text_color.setFillWithStroke(mTextColor, Color.BLACK)
        config_save.setTextColor(mTextColor)
    }

    private fun updateBgColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        config_events_list.background.applyColorFilter(mBgColor)
        config_bg_color.setFillWithStroke(mBgColor, Color.BLACK)
        config_save.setBackgroundColor(mBgColor)
    }

    private fun getListItems(): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>(10)
        var dateTime = DateTime.now().withTime(0, 0, 0, 0).plusDays(1)
        var code = Formatter.getDayCodeFromTS(dateTime.seconds())
        var day = Formatter.getDateDayTitle(code)
        listItems.add(ListSectionDay(day, code, false, false))

        var time = dateTime.withHourOfDay(7)
        listItems.add(
            ListEvent(
                1,
                time.seconds(),
                time.plusMinutes(30).seconds(),
                getString(R.string.sample_title_1),
                getString(R.string.sample_description_1),
                false,
                getProperPrimaryColor(),
                "",
                false,
                false,
                false,
                false
            )
        )
        time = dateTime.withHourOfDay(8)
        listItems.add(
            ListEvent(
                2,
                time.seconds(),
                time.plusHours(1).seconds(),
                getString(R.string.sample_title_2),
                getString(R.string.sample_description_2),
                false,
                getProperPrimaryColor(),
                "",
                false,
                false,
                false,
                false
            )
        )

        dateTime = dateTime.plusDays(1)
        code = Formatter.getDayCodeFromTS(dateTime.seconds())
        day = Formatter.getDateDayTitle(code)
        listItems.add(ListSectionDay(day, code, false, false))

        time = dateTime.withHourOfDay(8)
        listItems.add(
            ListEvent(
                3,
                time.seconds(),
                time.plusHours(1).seconds(),
                getString(R.string.sample_title_3),
                "",
                false,
                getProperPrimaryColor(),
                "",
                false,
                false,
                false,
                false
            )
        )
        time = dateTime.withHourOfDay(13)
        listItems.add(
            ListEvent(
                4,
                time.seconds(),
                time.plusHours(1).seconds(),
                getString(R.string.sample_title_4),
                getString(R.string.sample_description_4),
                false,
                getProperPrimaryColor(),
                "",
                false,
                false,
                false,
                false
            )
        )
        time = dateTime.withHourOfDay(18)
        listItems.add(
            ListEvent(
                5,
                time.seconds(),
                time.plusMinutes(10).seconds(),
                getString(R.string.sample_title_5),
                "",
                false,
                getProperPrimaryColor(),
                "",
                false,
                false,
                false,
                false
            )
        )

        return listItems
    }

    private val bgSeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mBgAlpha = progress.toFloat() / 100.toFloat()
            updateBgColor()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }
}
