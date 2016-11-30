package com.simplemobiletools.calendar.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import com.simplemobiletools.calendar.R
import com.simplemobiletools.calendar.activities.EventActivity
import com.simplemobiletools.calendar.extensions.adjustAlpha
import com.simplemobiletools.calendar.services.WidgetService

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
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun initVariables(context: Context) {
        mContext = context
        mRes = mContext.resources

        val prefs = initPrefs(context)
        val storedTextColor = prefs.getInt(WIDGET_TEXT_COLOR, Color.WHITE)
        mTextColor = storedTextColor.adjustAlpha(HIGH_ALPHA)

        mWidgetManager = AppWidgetManager.getInstance(mContext)

        mRemoteViews = RemoteViews(mContext.packageName, R.layout.widget_event_list)
        mIntent = Intent(mContext, MyWidgetListProvider::class.java)

        val bgColor = prefs.getInt(WIDGET_BG_COLOR, Color.BLACK)
        mRemoteViews.setInt(R.id.widget_event_list, "setBackgroundColor", bgColor)

        Intent(context, WidgetService::class.java).apply {
            data = Uri.parse(this.toUri(Intent.URI_INTENT_SCHEME))
            mRemoteViews.setRemoteAdapter(R.id.widget_event_list, this)
        }

        val startActivityIntent = Intent(context, EventActivity::class.java)
        val startActivityPendingIntent = PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        mRemoteViews.setPendingIntentTemplate(R.id.widget_event_list, startActivityPendingIntent)

        val appWidgetIds = mWidgetManager.getAppWidgetIds(ComponentName(context, MyWidgetListProvider::class.java))
        mWidgetManager.updateAppWidget(appWidgetIds, mRemoteViews)
        mWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_event_list)
    }

    private fun initPrefs(context: Context) = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
}
