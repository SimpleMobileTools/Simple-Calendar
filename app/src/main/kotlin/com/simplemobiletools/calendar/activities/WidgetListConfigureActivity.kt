package com.simplemobiletools.calendar.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.SeekBar
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.extensions.adjustAlpha
import com.simplemobiletools.calendar.helpers.*
import kotlinx.android.synthetic.main.widget_config_list.*
import yuku.ambilwarna.AmbilWarnaDialog

class WidgetListConfigureActivity : AppCompatActivity() {
    lateinit var mRes: Resources
    private var mPackageName = ""

    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColorWithoutTransparency = 0
    private var mBgColor = 0
    private var mTextColorWithoutTransparency = 0
    private var mTextColor = 0

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

        config_save.setOnClickListener { saveConfig() }
        config_bg_color.setOnClickListener { pickBackgroundColor() }
        config_text_color.setOnClickListener { pickTextColor() }
    }

    private fun initVariables() {
        mRes = resources

        val prefs = initPrefs(this)
        mTextColorWithoutTransparency = prefs.getInt(WIDGET_TEXT_COLOR, resources.getColor(R.color.colorPrimary))
        updateTextColors()

        mBgColor = prefs.getInt(WIDGET_BG_COLOR, 1)
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

    private fun initPrefs(context: Context) = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

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
        getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).apply {
            edit().putInt(WIDGET_BG_COLOR, mBgColor).putInt(WIDGET_TEXT_COLOR, mTextColorWithoutTransparency).apply()
        }
    }

    fun pickBackgroundColor() {
        val dialog = AmbilWarnaDialog(this, mBgColorWithoutTransparency, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {
            }

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                mBgColorWithoutTransparency = color
                updateBgColor()
            }
        })

        dialog.show()
    }

    fun pickTextColor() {
        val dialog = AmbilWarnaDialog(this, mTextColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onCancel(dialog: AmbilWarnaDialog) {
            }

            override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                mTextColorWithoutTransparency = color
                updateTextColors()
            }
        })

        dialog.show()
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetMonthlyProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateTextColors() {
        mTextColor = mTextColorWithoutTransparency.adjustAlpha(HIGH_ALPHA)

        config_text_color.setBackgroundColor(mTextColor)
        config_save.setTextColor(mTextColor)
    }

    private fun updateBgColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        config_bg_color.setBackgroundColor(mBgColor)
        config_save.setBackgroundColor(mBgColor)
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
