package com.simplemobiletools.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.widget.RemoteViews
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.DayActivity
import com.simplemobiletools.calendar.activities.SplashActivity
import com.simplemobiletools.calendar.extensions.config
import com.simplemobiletools.calendar.extensions.launchNewEventIntent
import com.simplemobiletools.calendar.services.WidgetService
import com.simplemobiletools.commons.extensions.getColoredIcon
import org.joda.time.DateTime

class MyWidgetListProvider : AppWidgetProvider() {
    private val NEW_EVENT = "new_event"
    private val LAUNCH_TODAY = "launch_today"

    companion object {
        private var mTextColor = 0

        lateinit var mRemoteViews: RemoteViews
        lateinit var mRes: Resources
        lateinit var mWidgetManager: AppWidgetManager
        lateinit var mIntent: Intent
        lateinit var mContext: Context
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        initVariables(context)
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun initVariables(context: Context) {
        mContext = context
        mRes = context.resources

        mTextColor = context.config.widgetTextColor

        mWidgetManager = AppWidgetManager.getInstance(context)

        mRemoteViews = RemoteViews(context.packageName, R.layout.widget_event_list)
        mIntent = Intent(context, MyWidgetListProvider::class.java)

        mRemoteViews.setInt(R.id.widget_event_list_holder, "setBackgroundColor", context.config.widgetBgColor)
        mRemoteViews.setInt(R.id.widget_event_list_empty, "setTextColor", mTextColor)
        mRemoteViews.setInt(R.id.widget_event_list_today, "setTextColor", mTextColor)

        val now = (System.currentTimeMillis() / 1000).toInt()
        val todayCode = Formatter.getDayCodeFromTS(now)
        val todayText = Formatter.getDayTitle(context, todayCode)
        mRemoteViews.setTextViewText(R.id.widget_event_list_today, todayText)

        mRemoteViews.setImageViewBitmap(R.id.widget_event_new_event, context.resources.getColoredIcon(mTextColor, R.drawable.ic_plus))
        setupIntent(NEW_EVENT, R.id.widget_event_new_event)
        setupIntent(LAUNCH_TODAY, R.id.widget_event_list_today)

        Intent(context, WidgetService::class.java).apply {
            data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
            mRemoteViews.setRemoteAdapter(R.id.widget_event_list, this)
        }

        val startActivityIntent = Intent(context, SplashActivity::class.java)
        val startActivityPendingIntent = PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        mRemoteViews.setPendingIntentTemplate(R.id.widget_event_list, startActivityPendingIntent)
        mRemoteViews.setEmptyView(R.id.widget_event_list, R.id.widget_event_list_empty)

        val appWidgetIds = mWidgetManager.getAppWidgetIds(ComponentName(context, MyWidgetListProvider::class.java))
        mWidgetManager.updateAppWidget(appWidgetIds, mRemoteViews)
        mWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
    }

    private fun setupIntent(action: String, id: Int) {
        mIntent.action = action
        val pendingIntent = PendingIntent.getBroadcast(mContext, 0, mIntent, 0)
        mRemoteViews.setOnClickPendingIntent(id, pendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        mContext = context

        when (intent.action) {
            NEW_EVENT -> context.launchNewEventIntent(true, true)
            LAUNCH_TODAY -> launchDayActivity()
            else -> super.onReceive(context, intent)
        }
    }

    private fun launchDayActivity() {
        Intent(mContext, DayActivity::class.java).apply {
            putExtra(DAY_CODE, Formatter.getDayCodeFromDateTime(DateTime()))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(this)
        }
    }
}
