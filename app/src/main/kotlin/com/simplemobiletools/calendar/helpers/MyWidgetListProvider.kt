package com.simplemobiletools.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.widget.RemoteViews
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.DayActivity
import com.simplemobiletools.calendar.extensions.adjustAlpha
import com.simplemobiletools.calendar.models.Day

class MyWidgetListProvider : AppWidgetProvider() {
    companion object {
        private var mTextColor = 0

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

        val prefs = initPrefs(context)
        val storedTextColor = prefs.getInt(WIDGET_TEXT_COLOR, Color.WHITE)
        mTextColor = storedTextColor.adjustAlpha(HIGH_ALPHA)

        mWidgetManager = AppWidgetManager.getInstance(mContext)

        mRemoteViews = RemoteViews(mContext.packageName, R.layout.fragment_month)
        mIntent = Intent(mContext, MyWidgetMonthlyProvider::class.java)

        val bgColor = prefs.getInt(WIDGET_BG_COLOR, Color.BLACK)
        mRemoteViews.setInt(R.id.calendar_holder, "setBackgroundColor", bgColor)
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

    private fun setupDayOpenIntent(id: Int, dayCode: String) {
        Intent(mContext, DayActivity::class.java).apply {
            putExtra(DAY_CODE, dayCode)
            val pendingIntent = PendingIntent.getActivity(mContext, Integer.parseInt(dayCode), this, 0)
            mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    private fun initPrefs(context: Context) = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

    fun updateDays(days: List<Day>) {
        val displayWeekNumbers = Config.newInstance(mContext).displayWeekNumbers
        val len = days.size
        val packageName = mContext.packageName
    }
}
