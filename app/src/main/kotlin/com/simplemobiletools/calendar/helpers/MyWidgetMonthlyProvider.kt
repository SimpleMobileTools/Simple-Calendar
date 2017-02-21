package com.simplemobiletools.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.RemoteViews
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.SplashActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.launchNewEventIntent
import com.simplemobiletools.calendar.interfaces.MonthlyCalendar
import com.simplemobiletools.calendar.models.Day
import com.simplemobiletools.commons.extensions.adjustAlpha
import org.joda.time.DateTime

class MyWidgetMonthlyProvider : AppWidgetProvider(), MonthlyCalendar {
    companion object {
        private val PREV = "prev"
        private val NEXT = "next"
        private val NEW_EVENT = "new_event"

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

        val config = context.config
        mTextColor = config.widgetTextColor
        mWeakTextColor = config.widgetTextColor.adjustAlpha(LOW_ALPHA)

        mWidgetManager = AppWidgetManager.getInstance(mContext)

        mRemoteViews = RemoteViews(mContext.packageName, R.layout.fragment_month_widget)
        mIntent = Intent(mContext, MyWidgetMonthlyProvider::class.java)
        setupButtons()
        updateLabelColor()
        updateTopViews()

        mRemoteViews.setInt(R.id.calendar_holder, "setBackgroundColor", config.widgetBgColor)

        mCalendar?.updateMonthlyCalendar(DateTime(), false)
    }

    private fun updateWidget() {
        val thisWidget = ComponentName(mContext, MyWidgetMonthlyProvider::class.java)
        AppWidgetManager.getInstance(mContext).updateAppWidget(thisWidget, mRemoteViews)
    }

    private fun setupIntent(action: String, id: Int) {
        mIntent.action = action
        val pendingIntent = PendingIntent.getBroadcast(mContext, 0, mIntent, 0)
        mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
    }

    private fun setupAppOpenIntent(id: Int) {
        Intent(mContext, SplashActivity::class.java).apply {
            val pendingIntent = PendingIntent.getActivity(mContext, 0, this, 0)
            mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun setupDayOpenIntent(id: Int, dayCode: String) {
        Intent(mContext, SplashActivity::class.java).apply {
            putExtra(DAY_CODE, dayCode)
            val pendingIntent = PendingIntent.getActivity(mContext, Integer.parseInt(dayCode), this, 0)
            mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun setupButtons() {
        setupIntent(PREV, R.id.top_left_arrow)
        setupIntent(NEXT, R.id.top_right_arrow)
        setupIntent(NEW_EVENT, R.id.top_new_event)
        setupAppOpenIntent(R.id.top_value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (mCalendar == null) {
            initVariables(context)
        }

        when (action) {
            PREV -> mCalendar?.getPrevMonth()
            NEXT -> mCalendar?.getNextMonth()
            NEW_EVENT -> mContext.launchNewEventIntent(true)
            else -> super.onReceive(context, intent)
        }
    }

    fun updateDays(days: List<Day>) {
        val displayWeekNumbers = mContext.config.displayWeekNumbers
        val len = days.size
        val packageName = mContext.packageName
        mRemoteViews.setInt(R.id.week_num, "setTextColor", mTextColor)
        mRemoteViews.setViewVisibility(R.id.week_num, if (displayWeekNumbers) View.VISIBLE else View.GONE)

        for (i in 0..5) {
            val id = mRes.getIdentifier("week_num_$i", "id", packageName)
            mRemoteViews.apply {
                setTextViewText(id, days[i * 7 + 3].weekOfYear.toString() + ":")
                setInt(id, "setTextColor", mTextColor)
                setViewVisibility(id, if (displayWeekNumbers) View.VISIBLE else View.GONE)
            }
        }

        for (i in 0..len - 1) {
            val day = days[i]
            val id = mRes.getIdentifier("day_$i", "id", packageName)
            var curTextColor = mWeakTextColor

            if (day.isThisMonth) {
                curTextColor = mTextColor
            }

            val text = day.value.toString()
            if (day.hasEvent) {
                val underlinedText = SpannableString(text)
                underlinedText.setSpan(UnderlineSpan(), 0, text.length, 0)
                mRemoteViews.setTextViewText(id, underlinedText)
            } else {
                mRemoteViews.setTextViewText(id, text)
            }

            val circleId = mRes.getIdentifier("day_${i}_circle", "id", packageName)
            if (day.isToday) {
                val wantedSize = mContext.resources.displayMetrics.widthPixels / 7
                val drawable = mContext.resources.getDrawable(R.drawable.circle_empty)
                drawable.setColorFilter(mTextColor, PorterDuff.Mode.SRC_IN)
                mRemoteViews.setImageViewBitmap(circleId, drawableToBitmap(drawable, wantedSize))
                mRemoteViews.setViewVisibility(circleId, View.VISIBLE)
            } else {
                mRemoteViews.setViewVisibility(circleId, View.GONE)
            }

            mRemoteViews.setInt(id, "setTextColor", curTextColor)
            setupDayOpenIntent(id, day.code)
        }
    }

    fun drawableToBitmap(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawable.setBounds(0, 0, width, height)
            drawable.draw(this)
        }
        return bitmap
    }

    private fun updateTopViews() {
        mRemoteViews.setInt(R.id.top_value, "setTextColor", mTextColor)

        var bmp = getColoredIcon(mContext, mTextColor, R.drawable.ic_pointer_left)
        mRemoteViews.setImageViewBitmap(R.id.top_left_arrow, bmp)

        bmp = getColoredIcon(mContext, mTextColor, R.drawable.ic_pointer_right)
        mRemoteViews.setImageViewBitmap(R.id.top_right_arrow, bmp)

        bmp = getColoredIcon(mContext, mTextColor, R.drawable.ic_plus)
        mRemoteViews.setImageViewBitmap(R.id.top_new_event, bmp)
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
        val mSundayFirst = mContext.config.isSundayFirst
        val packageName = mContext.packageName
        val letters = letterIDs
        for (i in 0..6) {
            val id = mRes.getIdentifier("label_$i", "id", packageName)
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
