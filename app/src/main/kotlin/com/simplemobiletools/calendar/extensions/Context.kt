package com.simplemobiletools.calendar.extensions

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.simplemobiletools.calendar.MyWidgetProvider
import com.simplemobiletools.calendar.R

fun Context.updateWidget() {
    val widgetsCnt = AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, MyWidgetProvider::class.java))
    if (widgetsCnt.size == 0)
        return

    val intent = Intent(this, MyWidgetProvider::class.java)
    intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val ids = intArrayOf(R.xml.widget_info)
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    sendBroadcast(intent)
}
