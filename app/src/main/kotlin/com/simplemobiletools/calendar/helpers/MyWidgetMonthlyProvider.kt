package com.simplemobiletools.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.view.View
import android.widget.RemoteViews
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SplashActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.launchNewEventIntent
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.models.DayMonthly
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.getContrastColor
import com.simplemobiletools.commons.extensions.setBackgroundColor
import com.simplemobiletools.commons.extensions.setTextSize
import org.joda.time.DateTime

class MyWidgetMonthlyProvider : AppWidgetProvider(), MonthlyCalendar {
    private val PREV = "prev"
    private val NEXT = "next"
    private val NEW_EVENT = "new_event"

    private var mTextColor = 0
    private var mCalendar: MonthlyCalendarImpl? = null
    private var mRemoteViews: RemoteViews? = null

    private var mSmallerFontSize = 0f
    private var mMediumFontSize = 0f
    private var mLargerFontSize = 0f

    lateinit var mRes: Resources
    lateinit var mContext: Context
    lateinit var mWidgetManager: AppWidgetManager
    lateinit var mIntent: Intent

    companion object {
        private var mTargetDate = DateTime()
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
        mWidgetManager = AppWidgetManager.getInstance(mContext)

        context.config.apply {
            mTextColor = widgetTextColor
            mMediumFontSize = getFontSize()
            mSmallerFontSize = getFontSize() - 3f
            mLargerFontSize = getFontSize() + 3f

            mRemoteViews = RemoteViews(mContext.packageName, R.layout.fragment_month_widget)
            mRemoteViews?.setBackgroundColor(R.id.calendar_holder, widgetBgColor)
        }

        mIntent = Intent(mContext, MyWidgetMonthlyProvider::class.java)
        setupButtons()
        updateLabelColor()
        updateTopViews()
        mCalendar?.updateMonthlyCalendar(mTargetDate, false)
    }

    private fun updateWidget() {
        val thisWidget = ComponentName(mContext, MyWidgetMonthlyProvider::class.java)
        try {
            AppWidgetManager.getInstance(mContext).updateAppWidget(thisWidget, mRemoteViews)
        } catch (ignored: Exception) {
        }
    }

    private fun setupIntent(action: String, id: Int) {
        mIntent.action = action
        val pendingIntent = PendingIntent.getBroadcast(mContext, 0, mIntent, 0)
        mRemoteViews?.setOnClickPendingIntent(id, pendingIntent)
    }

    private fun setupAppOpenIntent(id: Int) {
        Intent(mContext, SplashActivity::class.java).apply {
            val pendingIntent = PendingIntent.getActivity(mContext, 0, this, 0)
            mRemoteViews?.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun setupDayOpenIntent(id: Int, dayCode: String) {
        Intent(mContext, SplashActivity::class.java).apply {
            putExtra(DAY_CODE, dayCode)
            val pendingIntent = PendingIntent.getActivity(mContext, Integer.parseInt(dayCode), this, 0)
            mRemoteViews?.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun setupButtons() {
        setupIntent(PREV, R.id.top_left_arrow)
        setupIntent(NEXT, R.id.top_right_arrow)
        setupIntent(NEW_EVENT, R.id.top_new_event)
        setupAppOpenIntent(R.id.top_value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (mCalendar == null || mRemoteViews == null) {
            initVariables(context)
            return
        }

        val action = intent.action
        when (action) {
            PREV -> getPrevMonth()
            NEXT -> getNextMonth()
            NEW_EVENT -> mContext.launchNewEventIntent()
            else -> super.onReceive(context, intent)
        }
    }

    private fun getPrevMonth() {
        mTargetDate = mTargetDate.minusMonths(1)
        mCalendar?.getMonth(mTargetDate)
    }

    private fun getNextMonth() {
        mTargetDate = mTargetDate.plusMonths(1)
        mCalendar?.getMonth(mTargetDate)
    }

    private fun updateDays(days: List<DayMonthly>) {
        if (mRemoteViews == null)
            return

        val displayWeekNumbers = mContext.config.displayWeekNumbers
        val len = days.size
        val packageName = mContext.packageName
        mRemoteViews!!.apply {
            setTextColor(R.id.week_num, mTextColor)
            setTextSize(R.id.week_num, mSmallerFontSize)
            setViewVisibility(R.id.week_num, if (displayWeekNumbers) View.VISIBLE else View.GONE)
        }

        for (i in 0..5) {
            val id = mRes.getIdentifier("week_num_$i", "id", packageName)
            mRemoteViews!!.apply {
                setTextViewText(id, "${days[i * 7 + 3].weekOfYear}:")    // fourth day of the week matters
                setTextColor(id, mTextColor)
                setTextSize(id, mSmallerFontSize)
                setViewVisibility(id, if (displayWeekNumbers) View.VISIBLE else View.GONE)
            }
        }

        val weakTextColor = mTextColor.adjustAlpha(LOW_ALPHA)
        for (i in 0 until len) {
            val day = days[i]
            var textColor = if (day.isThisMonth) mTextColor else weakTextColor
            val primaryColor = mContext.config.primaryColor
            if (day.isToday)
                textColor = primaryColor.getContrastColor()

            val id = mRes.getIdentifier("day_$i", "id", packageName)

            mRemoteViews!!.apply {
                removeAllViews(id)
                val newRemoteView = RemoteViews(packageName, R.layout.day_monthly_item_view).apply {
                    setTextViewText(R.id.day_monthly_number_id, day.value.toString())
                    setTextColor(R.id.day_monthly_number_id, textColor)

                    if (day.isToday) {
                        setViewPadding(R.id.day_monthly_number_id, 10, 0, 10, 0)
                        setBackgroundColor(R.id.day_monthly_number_id, primaryColor)
                    }
                }
                addView(id, newRemoteView)
                setupDayOpenIntent(id, day.code)
            }
        }
    }

    private fun updateTopViews() {
        mRemoteViews?.setTextColor(R.id.top_value, mTextColor)
        mRemoteViews?.setTextSize(R.id.top_value, mLargerFontSize)

        var bmp = getColoredIcon(mContext, mTextColor, R.drawable.ic_pointer_left)
        mRemoteViews?.setImageViewBitmap(R.id.top_left_arrow, bmp)

        bmp = getColoredIcon(mContext, mTextColor, R.drawable.ic_pointer_right)
        mRemoteViews?.setImageViewBitmap(R.id.top_right_arrow, bmp)

        bmp = getColoredIcon(mContext, mTextColor, R.drawable.ic_plus)
        mRemoteViews?.setImageViewBitmap(R.id.top_new_event, bmp)
    }

    private fun updateMonth(month: String) {
        mRemoteViews?.setTextViewText(R.id.top_value, month)
    }

    override fun updateMonthlyCalendar(month: String, days: List<DayMonthly>) {
        try {
            updateMonth(month)
            updateDays(days)
            updateWidget()
        } catch (ignored: ArrayIndexOutOfBoundsException) {
        }
    }

    private fun updateLabelColor() {
        val mSundayFirst = mContext.config.isSundayFirst
        val packageName = mContext.packageName
        val letters = letterIDs
        for (i in 0..6) {
            val id = mRes.getIdentifier("label_$i", "id", packageName)
            mRemoteViews?.setTextColor(id, mTextColor)
            mRemoteViews?.setTextSize(id, mSmallerFontSize)

            var index = i
            if (!mSundayFirst)
                index = (index + 1) % letters.size

            mRemoteViews?.setTextViewText(id, mContext.resources.getString(letters[index]))
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
