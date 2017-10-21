package com.simplemobiletools.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

class MyWidgetMonthlyProvider : AppWidgetProvider() {
    private val PREV = "prev"
    private val NEXT = "next"
    private val NEW_EVENT = "new_event"

    companion object {
        var targetDate = DateTime.now()
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        performUpdate(context)
    }

    private fun performUpdate(context: Context) {
        val largerFontSize = context.config.getFontSize() + 3f
        val textColor = context.config.widgetTextColor

        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
            val views = RemoteViews(context.packageName, R.layout.fragment_month_widget)
            views.setBackgroundColor(R.id.calendar_holder, context.config.widgetBgColor)

            views.setTextColor(R.id.top_value, textColor)
            views.setTextSize(R.id.top_value, largerFontSize)

            var bmp = getColoredIcon(context, textColor, R.drawable.ic_pointer_left)
            views.setImageViewBitmap(R.id.top_left_arrow, bmp)

            bmp = getColoredIcon(context, textColor, R.drawable.ic_pointer_right)
            views.setImageViewBitmap(R.id.top_right_arrow, bmp)

            bmp = getColoredIcon(context, textColor, R.drawable.ic_plus)
            views.setImageViewBitmap(R.id.top_new_event, bmp)

            setupIntent(context, views, PREV, R.id.top_left_arrow)
            setupIntent(context, views, NEXT, R.id.top_right_arrow)
            setupIntent(context, views, NEW_EVENT, R.id.top_new_event)
            setupAppOpenIntent(context, views, R.id.top_value)
            updateDayLabels(context, views, textColor)

            appWidgetManager.updateAppWidget(it, views)
            MonthlyCalendarImpl(monthlyCalendar, context).getMonth(targetDate)
        }
    }

    private fun getComponentName(context: Context) = ComponentName(context, MyWidgetMonthlyProvider::class.java)

    private fun setupIntent(context: Context, views: RemoteViews, action: String, id: Int) {
        Intent(context, MyWidgetMonthlyProvider::class.java).apply {
            this.action = action
            val pendingIntent = PendingIntent.getBroadcast(context, 0, this, 0)
            views.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun setupAppOpenIntent(context: Context, views: RemoteViews, id: Int) {
        val intent = Intent(context, SplashActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        views.setOnClickPendingIntent(id, pendingIntent)
    }

    private fun setupDayOpenIntent(context: Context, views: RemoteViews, id: Int, dayCode: String) {
        Intent(context, SplashActivity::class.java).apply {
            putExtra(DAY_CODE, dayCode)
            val pendingIntent = PendingIntent.getActivity(context, Integer.parseInt(dayCode), this, 0)
            views.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PREV -> getPrevMonth(context)
            NEXT -> getNextMonth(context)
            NEW_EVENT -> context.launchNewEventIntent()
            else -> super.onReceive(context, intent)
        }
    }

    private fun getPrevMonth(context: Context) {
        targetDate = targetDate!!.minusMonths(1)
        MonthlyCalendarImpl(monthlyCalendar, context).getMonth(targetDate!!)
    }

    private fun getNextMonth(context: Context) {
        targetDate = targetDate!!.plusMonths(1)
        MonthlyCalendarImpl(monthlyCalendar, context).getMonth(targetDate!!)
    }

    private fun updateDays(context: Context, views: RemoteViews, days: List<DayMonthly>) {
        val displayWeekNumbers = context.config.displayWeekNumbers
        val textColor = context.config.widgetTextColor
        val smallerFontSize = context.config.getFontSize() - 3f
        val res = context.resources
        val len = days.size
        val packageName = context.packageName
        views.apply {
            setTextColor(R.id.week_num, textColor)
            setTextSize(R.id.week_num, smallerFontSize)
            setViewVisibility(R.id.week_num, if (displayWeekNumbers) View.VISIBLE else View.GONE)
        }

        for (i in 0..5) {
            val id = res.getIdentifier("week_num_$i", "id", packageName)
            views.apply {
                setTextViewText(id, "${days[i * 7 + 3].weekOfYear}:")    // fourth day of the week matters at determining week of the year
                setTextColor(id, textColor)
                setTextSize(id, smallerFontSize)
                setViewVisibility(id, if (displayWeekNumbers) View.VISIBLE else View.GONE)
            }
        }

        val weakTextColor = textColor.adjustAlpha(LOW_ALPHA)
        for (i in 0 until len) {
            val day = days[i]
            var currTextColor = if (day.isThisMonth) textColor else weakTextColor
            val primaryColor = context.config.primaryColor
            if (day.isToday)
                currTextColor = primaryColor.getContrastColor()

            val id = res.getIdentifier("day_$i", "id", packageName)
            views.removeAllViews(id)
            addDayNumber(context, views, day, currTextColor, id, primaryColor)
            setupDayOpenIntent(context, views, id, day.code)

            day.dayEvents.forEach {
                var backgroundColor = it.color
                var eventTextColor = backgroundColor.getContrastColor().adjustAlpha(MEDIUM_ALPHA)

                if (!day.isThisMonth) {
                    eventTextColor = eventTextColor.adjustAlpha(0.25f)
                    backgroundColor = backgroundColor.adjustAlpha(0.25f)
                }

                val newRemoteView = RemoteViews(packageName, R.layout.day_monthly_event_view).apply {
                    setTextViewText(R.id.day_monthly_event_id, it.title.replace(" ", "\u00A0"))
                    setTextColor(R.id.day_monthly_event_id, eventTextColor)
                    setTextSize(R.id.day_monthly_event_id, smallerFontSize - 3f)
                    setBackgroundColor(R.id.day_monthly_event_id, backgroundColor)
                }
                views.addView(id, newRemoteView)
            }
        }
    }

    private fun addDayNumber(context: Context, views: RemoteViews, day: DayMonthly, textColor: Int, id: Int, primaryColor: Int) {
        val newRemoteView = RemoteViews(context.packageName, R.layout.day_monthly_number_view).apply {
            setTextViewText(R.id.day_monthly_number_id, day.value.toString())
            setTextColor(R.id.day_monthly_number_id, textColor)
            setTextSize(R.id.day_monthly_number_id, context.config.getFontSize() - 3f)

            if (day.isToday) {
                setViewPadding(R.id.day_monthly_number_id, 10, 0, 10, 0)
                setBackgroundColor(R.id.day_monthly_number_id, primaryColor)
            }
        }
        views.addView(id, newRemoteView)
    }

    private val monthlyCalendar = object : MonthlyCalendar {
        override fun updateMonthlyCalendar(context: Context, month: String, days: List<DayMonthly>, checkedEvents: Boolean) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
                val views = RemoteViews(context.packageName, R.layout.fragment_month_widget)
                views.setTextViewText(R.id.top_value, month)
                updateDays(context, views, days)
                appWidgetManager.updateAppWidget(it, views)
            }
        }
    }

    private fun updateDayLabels(context: Context, views: RemoteViews, textColor: Int) {
        val sundayFirst = context.config.isSundayFirst
        val smallerFontSize = context.config.getFontSize() - 3f
        val res = context.resources
        val packageName = context.packageName
        val letters = letterIDs
        for (i in 0..6) {
            val id = res.getIdentifier("label_$i", "id", packageName)
            views.setTextColor(id, textColor)
            views.setTextSize(id, smallerFontSize)

            var index = i
            if (!sundayFirst)
                index = (index + 1) % letters.size

            views.setTextViewText(id, res.getString(letters[index]))
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
