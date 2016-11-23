package com.simplemobiletools.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.RemoteViews
import com.simplemobiletools.calendar.MonthlyCalendarImpl
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.DayActivity
import com.simplemobiletools.calendar.activities.MainActivity
import com.simplemobiletools.calendar.extensions.adjustAlpha
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.models.Day
import org.joda.time.DateTime

class MyWidgetProvider : AppWidgetProvider(), MonthlyCalendar {
    companion object {
        private val PREV = "prev"
        private val NEXT = "next"

        private var mDayTextSize = 0f
        private var mTodayTextSize = 0f
        private var mTextColor = 0
        private var mWeakTextColor = 0
        private var mCalendar: MonthlyCalendarImpl? = null

        lateinit var mRemoteViews: RemoteViews
        lateinit var mRes: Resources
        lateinit var mContext: Context
        lateinit var mWidgetManager: AppWidgetManager
        lateinit var mIntent: Intent
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        initVariables(context)
        updateWidget()
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun initVariables(context: Context) {
        mContext = context
        mRes = mContext.resources
        mCalendar = MonthlyCalendarImpl(this, mContext)

        val prefs = initPrefs(context)
        val storedTextColor = prefs.getInt(WIDGET_TEXT_COLOR, Color.WHITE)
        mTextColor = storedTextColor.adjustAlpha(HIGH_ALPHA)
        mWeakTextColor = storedTextColor.adjustAlpha(LOW_ALPHA)

        mDayTextSize = mRes.getDimension(R.dimen.day_text_size) / mRes.displayMetrics.density
        mTodayTextSize = mRes.getDimension(R.dimen.today_text_size) / mRes.displayMetrics.density
        mWidgetManager = AppWidgetManager.getInstance(mContext)

        mRemoteViews = RemoteViews(mContext.packageName, R.layout.fragment_month)
        mIntent = Intent(mContext, MyWidgetProvider::class.java)
        setupButtons()
        updateLabelColor()
        updateTopViews()

        val bgColor = prefs.getInt(WIDGET_BG_COLOR, Color.BLACK)
        mRemoteViews.setInt(R.id.calendar_holder, "setBackgroundColor", bgColor)

        mCalendar?.updateMonthlyCalendar(DateTime())
    }

    private fun updateWidget() {
        val thisWidget = ComponentName(mContext, MyWidgetProvider::class.java)
        AppWidgetManager.getInstance(mContext).updateAppWidget(thisWidget, mRemoteViews)
    }

    private fun setupIntent(action: String, id: Int) {
        mIntent.action = action
        val pendingIntent = PendingIntent.getBroadcast(mContext, 0, mIntent, 0)
        mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
    }

    private fun setupAppOpenIntent(id: Int) {
        Intent(mContext, MainActivity::class.java).apply {
            val pendingIntent = PendingIntent.getActivity(mContext, 0, this, 0)
            mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun setupDayOpenIntent(id: Int, dayCode: String) {
        Intent(mContext, DayActivity::class.java).apply {
            putExtra(DAY_CODE, dayCode)
            val pendingIntent = PendingIntent.getActivity(mContext, Integer.parseInt(dayCode), this, 0)
            mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun setupButtons() {
        setupIntent(PREV, R.id.top_left_arrow)
        setupIntent(NEXT, R.id.top_right_arrow)
        setupAppOpenIntent(R.id.top_value)
    }

    private fun initPrefs(context: Context) = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            PREV -> mCalendar?.getPrevMonth()
            NEXT -> mCalendar?.getNextMonth()
            else -> super.onReceive(context, intent)
        }
    }

    fun updateDays(days: List<Day>) {
        val displayWeekNumbers = Config.newInstance(mContext).displayWeekNumbers
        val len = days.size
        val packageName = mContext.packageName
        mRemoteViews.setInt(R.id.week_num, "setTextColor", mWeakTextColor)
        mRemoteViews.setViewVisibility(R.id.week_num, if (displayWeekNumbers) View.VISIBLE else View.GONE)

        for (i in 0..5) {
            val id = mRes.getIdentifier("week_num_" + i, "id", packageName)
            mRemoteViews.apply {
                setTextViewText(id, days[i * 7].weekOfYear.toString() + ":")
                setInt(id, "setTextColor", mWeakTextColor)
                setViewVisibility(id, if (displayWeekNumbers) View.VISIBLE else View.GONE)
            }
        }

        for (i in 0..len - 1) {
            val day = days[i]
            val id = mRes.getIdentifier("day_" + i, "id", packageName)
            var curTextColor = mWeakTextColor
            var curTextSize = mDayTextSize

            if (day.isThisMonth) {
                curTextColor = mTextColor
            }

            if (day.isToday) {
                curTextSize = mTodayTextSize
            }

            val text = day.value.toString()
            if (day.hasEvent) {
                val underlinedText = SpannableString(text)
                underlinedText.setSpan(UnderlineSpan(), 0, text.length, 0)
                mRemoteViews.setTextViewText(id, underlinedText)
            } else {
                mRemoteViews.setTextViewText(id, text)
            }
            mRemoteViews.setInt(id, "setTextColor", curTextColor)
            mRemoteViews.setFloat(id, "setTextSize", curTextSize)
            setupDayOpenIntent(id, day.code)
        }
    }

    private fun updateTopViews() {
        mRemoteViews.setInt(R.id.top_value, "setTextColor", mTextColor)

        var bmp = getColoredIcon(mContext, mTextColor, R.mipmap.arrow_left)
        mRemoteViews.setImageViewBitmap(R.id.top_left_arrow, bmp)

        bmp = getColoredIcon(mContext, mTextColor, R.mipmap.arrow_right)
        mRemoteViews.setImageViewBitmap(R.id.top_right_arrow, bmp)
    }

    fun updateMonth(month: String) {
        mRemoteViews.setTextViewText(R.id.top_value, month)
    }

    override fun updateMonthlyCalendar(month: String, days: List<Day>) {
        updateMonth(month)
        updateDays(days)
        updateWidget()
    }

    private fun updateLabelColor() {
        val mSundayFirst = Config.newInstance(mContext).isSundayFirst
        val packageName = mContext.packageName
        val letters = letterIDs
        for (i in 0..6) {
            val id = mRes.getIdentifier("label_" + i, "id", packageName)
            mRemoteViews.setInt(id, "setTextColor", mTextColor)

            var index = i
            if (!mSundayFirst)
                index = (index + 1) % letters.size

            mRemoteViews.setTextViewText(id, mContext.resources.getString(letters[index]))
        }
    }

    private fun getColoredIcon(context: Context, newTextColor: Int, id: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inMutable = true
        val bmp = BitmapFactory.decodeResource(context.resources, id, options)
        val paint = Paint()
        val filter = LightingColorFilter(newTextColor, 1)
        paint.colorFilter = filter
        val canvas = Canvas(bmp)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        return bmp
    }
}
