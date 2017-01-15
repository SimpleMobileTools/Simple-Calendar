package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.adapters.EventListWidgetAdapterOld
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.seconds
import com.simplemobiletools.calendar.helpers.Formatter
import com.simplemobiletools.calendar.helpers.MyWidgetListProvider
import com.simplemobiletools.calendar.models.ListEvent
import com.simplemobiletools.calendar.models.ListItem
import com.simplemobiletools.calendar.models.ListSection
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.adjustAlpha
import kotlinx.android.synthetic.main.widget_config_list.*
import org.joda.time.DateTime
import java.util.*

class WidgetListConfigureActivity : AppCompatActivity() {
    lateinit var mRes: Resources
    private var mPackageName = ""

    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColorWithoutTransparency = 0
    private var mTextColor = 0

    private var mEventsAdapter: EventListWidgetAdapterOld? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.widget_config_list)
        mPackageName = packageName
        initVariables()

        val extras = intent.extras
        if (extras != null)
            mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            finish()

        mEventsAdapter = EventListWidgetAdapterOld(this, getListItems())
        mEventsAdapter!!.setTextColor(mTextColor)
        config_events_list.adapter = mEventsAdapter

        config_save.setOnClickListener { saveConfig() }
        config_bg_color.setOnClickListener { pickBackgroundColor() }
        config_text_color.setOnClickListener { pickTextColor() }
    }

    private fun initVariables() {
        mRes = resources

        mTextColorWithoutTransparency = config.widgetTextColor
        updateTextColors()

        mBgColor = config.widgetBgColor
        if (mBgColor == 1) {
            mBgColor = Color.BLACK
            mBgAlpha = .2f
        } else {
            mBgAlpha = Color.alpha(mBgColor) / 255.toFloat()
        }

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        config_bg_seekbar.setOnSeekBarChangeListener(bgSeekbarChangeListener)
        config_bg_seekbar.progress = (mBgAlpha * 100).toInt()
        updateBgColor()
    }

    fun saveConfig() {
        storeWidgetColors()
        requestWidgetUpdate()

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
            widgetTextColor = mTextColorWithoutTransparency
        }
    }

    fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency) {
            mBgColorWithoutTransparency = it
            updateBgColor()
        }
    }

    fun pickTextColor() {
        ColorPickerDialog(this, mTextColor) {
            mTextColorWithoutTransparency = it
            updateTextColors()
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetListProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateTextColors() {
        mTextColor = mTextColorWithoutTransparency
        mEventsAdapter?.setTextColor(mTextColor)
        config_text_color.setBackgroundColor(mTextColor)
        config_save.setTextColor(mTextColor)
    }

    private fun updateBgColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        config_events_list.setBackgroundColor(mBgColor)
        config_bg_color.setBackgroundColor(mBgColor)
        config_save.setBackgroundColor(mBgColor)
    }

    private fun getListItems(): ArrayList<ListItem> {
        val listItems = ArrayList<ListItem>(10)
        var dateTime = DateTime.now().withTime(0, 0, 0, 0).plusDays(1)
        var code = Formatter.getDayCodeFromTS(dateTime.seconds())
        var day = Formatter.getDayTitle(this, code)
        listItems.add(ListSection(day))

        var time = dateTime.withHourOfDay(7)
        listItems.add(ListEvent(1, time.seconds(), time.plusMinutes(30).seconds(), getString(R.string.sample_title_1), getString(R.string.sample_description_1)))
        time = dateTime.withHourOfDay(8)
        listItems.add(ListEvent(2, time.seconds(), time.plusHours(1).seconds(), getString(R.string.sample_title_2), getString(R.string.sample_description_2)))

        dateTime = dateTime.plusDays(1)
        code = Formatter.getDayCodeFromTS(dateTime.seconds())
        day = Formatter.getDayTitle(this, code)
        listItems.add(ListSection(day))

        time = dateTime.withHourOfDay(8)
        listItems.add(ListEvent(3, time.seconds(), time.plusHours(1).seconds(), getString(R.string.sample_title_3), ""))
        time = dateTime.withHourOfDay(13)
        listItems.add(ListEvent(4, time.seconds(), time.plusHours(1).seconds(), getString(R.string.sample_title_4), getString(R.string.sample_description_4)))
        time = dateTime.withHourOfDay(18)
        listItems.add(ListEvent(5, time.seconds(), time.plusMinutes(10).seconds(), getString(R.string.sample_title_5), ""))

        return listItems
    }

    private val bgSeekbarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            mBgAlpha = progress.toFloat() / 100.toFloat()
            updateBgColor()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {

        }
    }
}
