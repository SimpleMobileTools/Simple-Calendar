package com.simplemobiletools.calendar.pro.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.simplemobiletools.calendar.pro.R
import com.simplemobiletools.calendar.pro.activities.SplashActivity
import com.simplemobiletools.calendar.pro.extensions.config
import com.simplemobiletools.calendar.pro.extensions.getWidgetFontSize
import com.simplemobiletools.calendar.pro.extensions.launchNewEventIntent
import com.simplemobiletools.calendar.pro.services.WidgetService
import com.simplemobiletools.calendar.pro.services.WidgetServiceEmpty
import com.simplemobiletools.commons.extensions.*
import org.joda.time.DateTime

class MyWidgetListProvider : AppWidgetProvider() {
    private val NEW_EVENT = "new_event"
    private val LAUNCH_CAL = "launch_cal"
    private val GO_TO_TODAY = "go_to_today"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        performUpdate(context)
    }

    private fun performUpdate(context: Context) {
        val fontSize = context.getWidgetFontSize()
        val textColor = context.config.widgetTextColor

        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
            val views = RemoteViews(context.packageName, R.layout.widget_event_list).apply {
                applyColorFilter(R.id.widget_event_list_background, context.config.widgetBgColor)
                setTextColor(R.id.widget_event_list_empty, textColor)
                setTextSize(R.id.widget_event_list_empty, fontSize)

                setTextColor(R.id.widget_event_list_today, textColor)
                setTextSize(R.id.widget_event_list_today, fontSize)
            }

            val todayText = Formatter.getLongestDate(getNowSeconds())
            views.setText(R.id.widget_event_list_today, todayText)

            views.setImageViewBitmap(R.id.widget_event_new_event, context.resources.getColoredBitmap(R.drawable.ic_plus_vector, textColor))
            setupIntent(context, views, NEW_EVENT, R.id.widget_event_new_event)
            setupIntent(context, views, LAUNCH_CAL, R.id.widget_event_list_today)

            views.setImageViewBitmap(R.id.widget_event_go_to_today, context.resources.getColoredBitmap(R.drawable.ic_today_vector, textColor))
            setupIntent(context, views, GO_TO_TODAY, R.id.widget_event_go_to_today)

            Intent(context, WidgetService::class.java).apply {
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                views.setRemoteAdapter(R.id.widget_event_list, this)
            }

            val startActivityIntent = context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)
            val startActivityPendingIntent = PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setPendingIntentTemplate(R.id.widget_event_list, startActivityPendingIntent)
            views.setEmptyView(R.id.widget_event_list, R.id.widget_event_list_empty)

            appWidgetManager.updateAppWidget(it, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.widget_event_list)
        }
    }

    private fun getComponentName(context: Context) = ComponentName(context, MyWidgetListProvider::class.java)

    private fun setupIntent(context: Context, views: RemoteViews, action: String, id: Int) {
        Intent(context, MyWidgetListProvider::class.java).apply {
            this.action = action
            val pendingIntent = PendingIntent.getBroadcast(context, 0, this, 0)
            views.setOnClickPendingIntent(id, pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NEW_EVENT -> context.launchNewEventIntent()
            LAUNCH_CAL -> launchCalenderInDefaultView(context)
            GO_TO_TODAY -> goToToday(context)
            else -> super.onReceive(context, intent)
        }
    }

    private fun launchCalenderInDefaultView(context: Context) {
        (context.getLaunchIntent() ?: Intent(context, SplashActivity::class.java)).apply {
            putExtra(DAY_CODE, Formatter.getDayCodeFromDateTime(DateTime()))
            putExtra(VIEW_TO_OPEN, context.config.listWidgetViewToOpen)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(this)
        }
    }

    // hacky solution for reseting the events list
    private fun goToToday(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.getAppWidgetIds(getComponentName(context)).forEach {
            val views = RemoteViews(context.packageName, R.layout.widget_event_list)
            Intent(context, WidgetServiceEmpty::class.java).apply {
                data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
                views.setRemoteAdapter(R.id.widget_event_list, this)
            }

            appWidgetManager.updateAppWidget(it, views)
        }

        performUpdate(context)
    }
}
