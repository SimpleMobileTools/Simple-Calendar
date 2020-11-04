package com.simplemobiletools.calendar.pro.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SplashActivity
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.getLaunchIntent
import com.simplemobiletools.commons.extensions.setText

class MyWidgetDateProvider : AppWidgetProvider() {
    private val OPEN_APP_INTENT_ID = 1

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
            RemoteViews(context.packageName, R.layout.widget_date).apply {
                applyColorFilter(R.id.widget_date_background, context.config.widgetBgColor)
                setText(R.id.widget_date_label, Formatter.getTodayDayNumber())
                setText(R.id.widget_month_label, Formatter.getCurrentMonthShort())

                setTextColor(R.id.widget_date_label, context.config.widgetTextColor)
                setTextColor(R.id.widget_month_label, context.config.widgetTextColor)

                setupAppOpenIntent(context, this)
                appWidgetManager.updateAppWidget(it, this)
            }

            appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widget_date_holder)
        }
    }

    private fun getComponentName(context: Context) = ComponentName(context, MyWidgetDateProvider::class.java)

    private fun setupAppOpenIntent(context: Context, views: RemoteViews) {
        (context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)).apply {
            val pendingIntent = PendingIntent.getActivity(context, OPEN_APP_INTENT_ID, this, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widget_date_holder, pendingIntent)
        }
    }
}
