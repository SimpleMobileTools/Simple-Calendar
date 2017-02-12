package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import android.widget.TextView
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.helpers.LOW_ALPHA
import com.simplemobiletools.calendar.helpers.MonthlyCalendarImpl
import com.simplemobiletools.calendar.helpers.MyWidgetMonthlyProvider
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.models.Day
import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.beVisible
import com.simplemobiletools.commons.extensions.removeFlag
import kotlinx.android.synthetic.main.first_row.*
import kotlinx.android.synthetic.main.top_navigation.*
import kotlinx.android.synthetic.main.widget_config_monthly.*
import org.joda.time.DateTime

class WidgetMonthlyConfigureActivity : AppCompatActivity(), MonthlyCalendar {
    lateinit var mRes: Resources
    private var mDays: List<Day>? = null
    private var mPackageName = ""

    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColorWithoutTransparency = 0
    private var mTextColor = 0
    private var mWeakTextColor = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.widget_config_monthly)
        mPackageName = packageName
        initVariables()

        val extras = intent.extras
        if (extras != null)
            mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            finish()

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

        MonthlyCalendarImpl(this, applicationContext).updateMonthlyCalendar(DateTime(), false)
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
            updateDays()
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetMonthlyProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateTextColors() {
        mTextColor = mTextColorWithoutTransparency
        mWeakTextColor = mTextColorWithoutTransparency.adjustAlpha(LOW_ALPHA)

        top_left_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
        top_right_arrow.drawable.mutate().setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP)
        top_value.setTextColor(mTextColor)
        config_text_color.setBackgroundColor(mTextColor)
        config_save.setTextColor(mTextColor)
        updateLabels()
    }

    private fun updateBgColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        config_calendar.setBackgroundColor(mBgColor)
        config_bg_color.setBackgroundColor(mBgColor)
        config_save.setBackgroundColor(mBgColor)
    }

    private fun updateDays() {
        val len = mDays!!.size

        if (applicationContext.config.displayWeekNumbers) {
            week_num.setTextColor(mTextColor)
            week_num.beVisible()

            for (i in 0..5) {
                (findViewById(mRes.getIdentifier("week_num_$i", "id", mPackageName)) as TextView).apply {
                    text = "${mDays!![i * 7 + 3].weekOfYear}:"
                    setTextColor(mTextColor)
                    beVisible()
                }
            }
        }

        val todayCircle = resources.getDrawable(R.drawable.circle_empty)
        todayCircle.setColorFilter(mTextColor, PorterDuff.Mode.SRC_IN)

        for (i in 0..len - 1) {
            val day = mDays!![i]
            var curTextColor = mWeakTextColor

            if (day.isThisMonth) {
                curTextColor = mTextColor
            }

            (findViewById(mRes.getIdentifier("day_$i", "id", mPackageName)) as TextView).apply {
                text = day.value.toString()
                setTextColor(curTextColor)

                paintFlags = if (day.hasEvent) (paintFlags or Paint.UNDERLINE_TEXT_FLAG) else (paintFlags.removeFlag(Paint.UNDERLINE_TEXT_FLAG))
                background = if (day.isToday) todayCircle else null
            }
        }
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

    override fun updateMonthlyCalendar(month: String, days: List<Day>) {
        runOnUiThread {
            mDays = days
            updateMonth(month)
            updateDays()
        }
    }

    private fun updateMonth(month: String) {
        top_value.text = month
    }

    private fun updateLabels() {
        for (i in 0..6) {
            (findViewById(mRes.getIdentifier("label_$i", "id", mPackageName)) as TextView).apply {
                setTextColor(mTextColor)
            }
        }
    }
}
